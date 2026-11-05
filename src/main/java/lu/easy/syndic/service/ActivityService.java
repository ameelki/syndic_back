package lu.easy.syndic.service;

import lu.easy.syndic.entity.ActivityLog;
import lu.easy.syndic.entity.User;
import lu.easy.syndic.repository.ActivityLogRepository;
import lu.easy.syndic.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Journal d'activité : qui a fait quoi (visible par le syndic). N'interrompt jamais l'action en cas d'erreur. */
@Service
public class ActivityService {

    private final ActivityLogRepository repo;
    private final UserRepository users;

    public ActivityService(ActivityLogRepository repo, UserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    public void log(String action, String target) {
        try {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            User u = a == null || a.getName() == null ? null : users.findByEmail(a.getName()).orElse(null);
            ActivityLog l = new ActivityLog();
            l.actorId = u == null ? null : u.id;
            l.actorName = u == null ? "Système" : u.firstName + " " + u.lastName;
            l.agencyId = u == null ? null : u.agencyId;
            l.action = action;
            l.target = target == null ? null : (target.length() > 250 ? target.substring(0, 249) + "…" : target);
            repo.save(l);
        } catch (Exception ignored) {
            // le journal ne doit jamais bloquer l'action
        }
    }
}
