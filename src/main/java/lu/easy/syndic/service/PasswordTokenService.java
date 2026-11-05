package lu.easy.syndic.service;

import lu.easy.syndic.entity.PasswordToken;
import lu.easy.syndic.entity.User;
import lu.easy.syndic.repository.AgencyRepository;
import lu.easy.syndic.repository.PasswordTokenRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class PasswordTokenService {

    public static final String RESET = "RESET";
    public static final String INVITE = "INVITE";

    private final PasswordTokenRepository tokens;
    private final AgencyRepository agencies;
    private final MailService mail;
    private final SecureRandom random = new SecureRandom();

    public PasswordTokenService(PasswordTokenRepository tokens, AgencyRepository agencies, MailService mail) {
        this.tokens = tokens;
        this.agencies = agencies;
        this.mail = mail;
    }

    public static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Crée un lien à usage unique ; renvoie le jeton en clair (à mettre dans l'email, jamais stocké). */
    public String issue(User u, String purpose, int hours) {
        byte[] b = new byte[32];
        random.nextBytes(b);
        String raw = HexFormat.of().formatHex(b);
        PasswordToken t = new PasswordToken();
        t.tokenHash = hash(raw);
        t.userId = u.id;
        t.purpose = purpose;
        t.expiresAt = LocalDateTime.now().plusHours(hours);
        tokens.save(t);
        return raw;
    }

    /** Vrai si un lien a déjà été envoyé à cet utilisateur dans la dernière minute (anti-abus). */
    public boolean recentlyIssued(User u) {
        return tokens.findFirstByUserIdOrderByIdDesc(u.id)
                .filter(t -> t.createdAt.isAfter(LocalDateTime.now().minusMinutes(1))).isPresent();
    }

    public String link(User u, String raw) {
        String slug = u.agencyId == null ? null : agencies.findById(u.agencyId).map(a -> a.slug).orElse(null);
        return mail.link("/reinitialiser?token=" + raw, slug);
    }
}
