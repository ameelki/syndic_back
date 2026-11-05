package lu.easy.syndic.controller;

import lu.easy.syndic.entity.Notification;
import lu.easy.syndic.repository.NotificationRepository;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Cloche de notifications de l'utilisateur connecté. */
@RestController
@RequestMapping("/api/notifications")
@Transactional
public class NotificationController {

    private final NotificationRepository repo;
    private final CurrentUser current;

    public NotificationController(NotificationRepository repo, CurrentUser current) {
        this.repo = repo;
        this.current = current;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Map<String, Object> list(Authentication a) {
        Long me = current.get(a).id;
        List<Notification> items = repo.findTop30ByUserIdOrderByIdDesc(me);
        return Map.of("unread", repo.countByUserIdAndReadAtIsNull(me), "items", items);
    }

    @PostMapping("/read-all")
    public Map<String, Object> readAll(Authentication a) {
        Long me = current.get(a).id;
        List<Notification> unread = repo.findAllByUserIdAndReadAtIsNull(me);
        unread.forEach(n -> n.readAt = LocalDateTime.now());
        repo.saveAll(unread);
        return Map.of("unread", 0);
    }

    @PostMapping("/{id}/read")
    public Map<String, Object> read(@PathVariable Long id, Authentication a) {
        Long me = current.get(a).id;
        repo.findById(id).filter(n -> n.userId.equals(me)).ifPresent(n -> {
            if (n.readAt == null) { n.readAt = LocalDateTime.now(); repo.save(n); }
        });
        return Map.of("unread", repo.countByUserIdAndReadAtIsNull(me));
    }
}
