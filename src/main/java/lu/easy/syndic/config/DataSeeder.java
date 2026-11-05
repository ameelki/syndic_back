package lu.easy.syndic.config;

import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.AgencyRepository;
import lu.easy.syndic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Au premier démarrage : agence par défaut, son syndic (administrateur) et le super administrateur de la plateforme. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String[] TABLES = {"app_users", "buildings", "lots", "charge_calls", "charges", "expenses", "incidents", "works",
            "assemblies", "resolutions", "votes", "proxy_grants", "announcements", "documents", "budgets", "suppliers",
            "reservations", "contact_messages"};

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final AgencyRepository agencies;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final String agencySlug;
    private final String agencyName;
    private final String superEmail;
    private final String superPassword;

    public DataSeeder(JdbcTemplate jdbc, UserRepository users, AgencyRepository agencies, PasswordEncoder encoder,
                      @Value("${app.admin.email}") String email, @Value("${app.admin.password}") String password,
                      @Value("${app.agency.default-slug:default}") String agencySlug,
                      @Value("${app.agency.default-name:Syndic}") String agencyName,
                      @Value("${app.super.email:super@syndic.lu}") String superEmail,
                      @Value("${app.super.password:changeme123}") String superPassword) {
        this.jdbc = jdbc;
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

    /**
     * Base existante créée avant les agences : les lignes sans agence (comptes syndic / copropriétaires compris,
     * hors super administrateur) sont rattachées à l'agence par défaut, sinon elles resteraient inaccessibles.
     */
    private void adoptOrphans(Long agencyId) {
        for (String t : TABLES) {
            String where = t.equals("app_users") ? " AND role <> 'SUPER_ADMIN'" : "";
            jdbc.update("UPDATE " + t + " SET agency_id = ? WHERE agency_id IS NULL" + where, agencyId);
        }
    }

    @Override
    public void run(String... args) {
        Agency agency = agencies.findBySlug(agencySlug).orElseGet(() -> {
            Agency a = new Agency();
            a.slug = agencySlug;
            a.name = agencyName;
            return agencies.save(a);
        });
        adoptOrphans(agency.id);
        if (!users.existsByEmail(email)) {
            users.save(account("Syndic", "Administrateur", email, password, Role.ADMIN, agency.id));
        }
        if (!users.existsByEmail(superEmail)) {
            users.save(account("Super", "Administrateur", superEmail, superPassword, Role.SUPER_ADMIN, null));
        }
    }
}
