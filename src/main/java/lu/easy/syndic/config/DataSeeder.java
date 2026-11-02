package lu.easy.syndic.config;

import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Crée le compte syndic au premier démarrage. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public DataSeeder(UserRepository users, PasswordEncoder encoder,
                      @Value("${app.admin.email}") String email, @Value("${app.admin.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email.trim().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmail(email)) return;
        User u = new User();
        u.firstName = "Syndic";
        u.lastName = "Administrateur";
        u.email = email;
        u.password = encoder.encode(password);
        u.role = Role.ADMIN;
        u.status = UserStatus.ACTIVE;
        u.approvedAt = LocalDateTime.now();
        users.save(u);
    }
}
