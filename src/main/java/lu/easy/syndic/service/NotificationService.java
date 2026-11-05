package lu.easy.syndic.service;

import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.AgencyRepository;
import lu.easy.syndic.repository.NotificationRepository;
import lu.easy.syndic.repository.UserRepository;
import lu.easy.syndic.security.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;

/** Notifications dans l'application (cloche) et par email. */
@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository users;
    private final AgencyRepository agencies;
    private final MailService mail;

    public NotificationService(NotificationRepository repo, UserRepository users, AgencyRepository agencies, MailService mail) {
        this.repo = repo;
        this.users = users;
        this.agencies = agencies;
        this.mail = mail;
    }

    /** Notification dans l'application seulement. */
    public void notify(User u, String title, String body, String path) {
        if (u == null) return;
        Notification n = new Notification();
        n.userId = u.id;
        n.agencyId = u.agencyId;
        n.title = cut(title, 150);
        n.body = cut(body, 500);
        n.link = path;
        repo.save(n);
    }

    /** Notification dans l'application + email. */
    public void notifyAndMail(User u, String title, String body, String path) {
        if (u == null) return;
        notify(u, title, body, path);
        String slug = u.agencyId == null ? null : agencies.findById(u.agencyId).map(a -> a.slug).orElse(null);
        mail.send(u.email, title, body + "\n\n" + mail.link(path == null ? "" : path, slug));
    }

    /** Prévient les syndics de l'agence courante. */
    public void notifyAdmins(String title, String body, String path) {
        Long agency = TenantContext.get();
        if (agency == null) return;
        for (User a : users.findAllByAgencyIdAndRole(agency, Role.ADMIN)) {
            if (a.status == UserStatus.ACTIVE) notify(a, title, body, path);
        }
    }

    /** Prévient (appli + email) tous les copropriétaires actifs ; buildingOwners = null pour toute l'agence. */
    public int notifyOwners(List<User> owners, String title, String body, String path) {
        int n = 0;
        for (User o : owners) {
            if (o.status != UserStatus.ACTIVE) continue;
            notifyAndMail(o, title, body, path);
            n++;
        }
        return n;
    }

    private static String cut(String s, int max) {
        return s == null ? null : (s.length() <= max ? s : s.substring(0, max - 1) + "…");
    }
}
