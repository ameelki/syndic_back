package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.ContactMessageRepository;
import lu.easy.syndic.repository.LotRepository;
import lu.easy.syndic.repository.UserRepository;
import lu.easy.syndic.service.CurrentUser;
import lu.easy.syndic.service.NotificationService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Messagerie copropriétaire → syndic, avec réponse du syndic. */
@RestController
@RequestMapping("/api")
public class MessageController {

    public record MessageRequest(@NotBlank @Size(max = 150) String subject, @NotBlank @Size(max = 3000) String content, Long buildingId) {}
    public record ReplyRequest(@NotBlank @Size(max = 3000) String reply) {}

    private final ContactMessageRepository repo;
    private final CurrentUser current;
    private final LotRepository lots;
    private final NotificationService notifs;
    private final UserRepository users;

    public MessageController(ContactMessageRepository repo, CurrentUser current, LotRepository lots,
                             NotificationService notifs, UserRepository users) {
        this.repo = repo;
        this.current = current;
        this.lots = lots;
        this.notifs = notifs;
        this.users = users;
    }

    @GetMapping("/messages")
    public List<ContactMessage> mine(Authentication a) {
        return repo.findAllByOwnerIdOrderByCreatedAtDesc(current.get(a).id);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactMessage send(@Valid @RequestBody MessageRequest r, Authentication a) {
        User u = current.get(a);
        ContactMessage m = new ContactMessage();
        m.ownerId = u.id;
        m.ownerName = u.firstName + " " + u.lastName;
        m.subject = r.subject().trim();
        m.content = r.content().trim();
        Set<Long> mine = lots.findAllByOwnerId(u.id).stream().map(l -> l.buildingId).filter(Objects::nonNull).collect(Collectors.toSet());
        m.buildingId = r.buildingId() != null && mine.contains(r.buildingId()) ? r.buildingId()
                : (mine.size() == 1 ? mine.iterator().next() : null);
        m = repo.save(m);
        notifs.notifyAdmins("Nouveau message : " + m.subject, m.ownerName, "/app/messages");
        return m;
    }

    @GetMapping("/admin/messages")
    public List<ContactMessage> all(@RequestParam(required = false) Long buildingId) {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(m -> buildingId == null || m.buildingId == null || buildingId.equals(m.buildingId)).toList();
    }

    @PutMapping("/admin/messages/{id}/reply")
    public ContactMessage reply(@PathVariable Long id, @Valid @RequestBody ReplyRequest r) {
        ContactMessage m = repo.findById(id).orElseThrow(() -> CurrentUser.notFound("Message"));
        m.reply = r.reply().trim();
        m.repliedAt = LocalDateTime.now();
        final ContactMessage saved = repo.save(m);
        if (saved.ownerId != null) {
            users.findById(saved.ownerId).ifPresent(o -> notifs.notifyAndMail(o, "Réponse du syndic : " + saved.subject, saved.reply, "/app/messages"));
        }
        return saved;
    }
}
