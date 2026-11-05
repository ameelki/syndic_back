package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lu.easy.syndic.dto.Dtos.UserDto;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.UserRepository;
import lu.easy.syndic.security.TenantContext;
import lu.easy.syndic.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Équipe du syndic : plusieurs comptes gestionnaires par agence. */
@RestController
@RequestMapping("/api/admin/staff")
@Transactional
public class StaffController {

    public record StaffInvite(@NotBlank @Size(max = 100) String firstName, @NotBlank @Size(max = 100) String lastName,
                              @NotBlank @Email @Size(max = 190) String email) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordTokenService tokens;
    private final MailService mail;
    private final CurrentUser current;
    private final ActivityService activity;

    public StaffController(UserRepository users, PasswordEncoder encoder, PasswordTokenService tokens, MailService mail,
                           CurrentUser current, ActivityService activity) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.mail = mail;
        this.current = current;
        this.activity = activity;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserDto> list() {
        Long agency = TenantContext.get();
        return users.findAllByAgencyIdAndRole(agency, Role.ADMIN).stream().map(AuthController::toDto).toList();
    }

    /** Crée un compte gestionnaire et envoie un lien (72 h) pour qu'il choisisse son mot de passe. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto invite(@Valid @RequestBody StaffInvite r, Authentication a) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        User me = current.get(a);
        User u = new User();
        u.firstName = r.firstName().trim();
        u.lastName = r.lastName().trim();
        u.email = email;
        u.password = encoder.encode(UUID.randomUUID() + UUID.randomUUID().toString());
        u.role = Role.ADMIN;
        u.status = UserStatus.ACTIVE;
        u.agencyId = me.agencyId;
        users.save(u);
        String raw = tokens.issue(u, PasswordTokenService.INVITE, 72);
        mail.send(u.email, "Invitation : accès gestionnaire",
                "Bonjour " + u.firstName + ",\n\n" + me.firstName + " " + me.lastName
                        + " vous invite à gérer la copropriété. Choisissez votre mot de passe avec ce lien (valable 72 heures) :\n"
                        + tokens.link(u, raw));
        activity.log("Invitation gestionnaire", u.firstName + " " + u.lastName);
        return AuthController.toDto(u);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id, Authentication a) {
        User me = current.get(a);
        User u = users.findById(id).filter(x -> x.role == Role.ADMIN)
                .orElseThrow(() -> CurrentUser.notFound("Gestionnaire"));
        if (u.id.equals(me.id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas supprimer votre propre compte ici");
        users.delete(u);
        activity.log("Suppression gestionnaire", u.firstName + " " + u.lastName);
        return ResponseEntity.noContent().build();
    }
}
