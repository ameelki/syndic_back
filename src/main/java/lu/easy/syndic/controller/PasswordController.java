package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lu.easy.syndic.dto.Dtos.MessageResponse;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.PasswordTokenRepository;
import lu.easy.syndic.repository.UserRepository;
import lu.easy.syndic.service.MailService;
import lu.easy.syndic.service.PasswordTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/** Mot de passe oublié / définition du mot de passe d'un compte invité (routes publiques sous /api/auth). */
@RestController
@RequestMapping("/api/auth")
public class PasswordController {

    public record ForgotRequest(@NotBlank @Email String email) {}
    public record ResetRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 72) String password) {}

    private final UserRepository users;
    private final PasswordTokenRepository tokens;
    private final PasswordTokenService tokenService;
    private final MailService mail;
    private final PasswordEncoder encoder;

    public PasswordController(UserRepository users, PasswordTokenRepository tokens, PasswordTokenService tokenService,
                              MailService mail, PasswordEncoder encoder) {
        this.users = users;
        this.tokens = tokens;
        this.tokenService = tokenService;
        this.mail = mail;
        this.encoder = encoder;
    }

    /** Répond toujours la même chose, que le compte existe ou non (pas de fuite d'information). */
    @PostMapping("/forgot")
    public MessageResponse forgot(@Valid @RequestBody ForgotRequest r) {
        users.findByEmail(r.email().trim().toLowerCase())
                .filter(u -> u.status == UserStatus.ACTIVE)
                .filter(u -> !tokenService.recentlyIssued(u))
                .ifPresent(u -> {
                    String raw = tokenService.issue(u, PasswordTokenService.RESET, 2);
                    mail.send(u.email, "Réinitialisation de votre mot de passe",
                            "Bonjour " + u.firstName + ",\n\nPour choisir un nouveau mot de passe, ouvrez ce lien (valable 2 heures) :\n"
                                    + tokenService.link(u, raw) + "\n\nSi vous n'êtes pas à l'origine de cette demande, ignorez cet email.");
                });
        return new MessageResponse("Si un compte existe pour cette adresse, un email vient d'être envoyé.");
    }

    @PostMapping("/reset")
    @Transactional
    public MessageResponse reset(@Valid @RequestBody ResetRequest r) {
        PasswordToken t = tokens.findByTokenHash(PasswordTokenService.hash(r.token().trim()))
                .filter(x -> x.usedAt == null && x.expiresAt.isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide ou expiré. Refaites une demande."));
        User u = users.findById(t.userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide ou expiré. Refaites une demande."));
        u.password = encoder.encode(r.password());
        users.save(u);
        t.usedAt = LocalDateTime.now();
        tokens.save(t);
        return new MessageResponse("Mot de passe modifié. Vous pouvez vous connecter.");
    }
}
