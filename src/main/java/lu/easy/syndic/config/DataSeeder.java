package lu.easy.syndic.config;

import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.AgencyRepository;
import lu.easy.syndic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Au premier démarrage : agence par défaut, son syndic (administrateur) et le super administrateur de la plateforme. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final AgencyRepository agencies;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final String agencySlug;
    private final String agencyName;
    private final String superEmail;
    private final String superPassword;

    public DataSeeder(UserRepository users, AgencyRepository agencies, PasswordEncoder encoder,
                      @Value("${app.admin.email}") String email, @Value("${app.admin.password}") String password,
                      @Value("${app.agency.default-slug:default}") String agencySlug,
                      @Value("${app.agency.default-name:Syndic}") String agencyName,
                      @Value("${app.super.email:super@syndic.lu}") String superEmail,
                      @Value("${app.super.password:changeme123}") String superPassword) {
        this.users = users;
        this.agencies = agencies;
        this.encoder = encoder;
        this.email = email.trim().toLowerCase();
        this.password = password;
        this.agencySlug = agencySlug;
        this.agencyName = agencyName;
        this.superEmail = superEmail.trim().toLowerCase();
        this.superPassword = superPassword;
    }

    private User account(String first, String last, String mail, String pwd, Role role, Long agencyId) {
        User u = new User();
        u.firstName = first;
        u.lastName = last;
        u.email = mail;
        u.password = encoder.encode(pwd);
        u.role = role;
        u.status = UserStatus.ACTIVE;
        u.approvedAt = LocalDateTime.now();
        u.agencyId = agencyId;
        return u;
    }

    @Override
    public void run(String... args) {
        Agency agency = agencies.findBySlug(agencySlug).orElseGet(() -> {
            Agency a = new Agency();
            a.slug = agencySlug;
            a.name = agencyName;
            return agencies.save(a);
        });
        if (!users.existsByEmail(email)) {
            users.save(account("Syndic", "Administrateur", email, password, Role.ADMIN, agency.id));
        }
        if (!users.existsByEmail(superEmail)) {
            users.save(account("Super", "Administrateur", superEmail, superPassword, Role.SUPER_ADMIN, null));
        }
    }
}
