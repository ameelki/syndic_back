package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.ContactMessageRepository;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Messagerie copropriétaire → syndic, avec réponse du syndic. */
@RestController
@RequestMapping("/api")
public class MessageController {

    public record MessageRequest(@NotBlank @Size(max = 150) String subject, @NotBlank @Size(max = 3000) String content) {}
    public record ReplyRequest(@NotBlank @Size(max = 3000) String reply) {}

    private final ContactMessageRepository repo;
    private final CurrentUser current;

    public MessageController(ContactMessageRepository repo, CurrentUser current) {
        this.repo = repo;
        this.current = current;
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
        return repo.save(m);
    }

    @GetMapping("/admin/messages")
    public List<ContactMessage> all() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @PutMapping("/admin/messages/{id}/reply")
    public ContactMessage reply(@PathVariable Long id, @Valid @RequestBody ReplyRequest r) {
        ContactMessage m = repo.findById(id).orElseThrow(() -> CurrentUser.notFound("Message"));
        m.reply = r.reply().trim();
        m.repliedAt = LocalDateTime.now();
        return repo.save(m);
    }
}
