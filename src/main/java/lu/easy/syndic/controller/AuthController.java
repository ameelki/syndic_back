package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import lu.easy.syndic.dto.Dtos.*;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.AgencyRepository;
import lu.easy.syndic.repository.UserRepository;
import lu.easy.syndic.security.JwtService;
import lu.easy.syndic.service.AgencyService;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final CurrentUser current;
    private final AgencyRepository agencies;
    private final AgencyService agencyService;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt, CurrentUser current,
                          AgencyRepository agencies, AgencyService agencyService) {
        this.agencies = agencies;
        this.agencyService = agencyService;
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.current = current;
    }

    public static UserDto toDto(User u) {
        return new UserDto(u.id, u.firstName, u.lastName, u.email, u.phone, u.requestedLot, u.role, u.status,
                u.createdAt, u.approvedAt, u.agencyId);
    }

    /** L'inscription crée un compte EN ATTENTE : le syndic doit le valider avant toute connexion. */
    @PostMapping("/auth/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest r) {
        Agency agency = agencyService.resolveRequired(r.agencySlug());
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        }
        User u = new User();
        u.firstName = r.firstName().trim();
        u.lastName = r.lastName().trim();
        u.email = email;
        u.phone = r.phone();
        u.requestedLot = r.requestedLot();
        u.password = encoder.encode(r.password());
        u.role = Role.OWNER;
        u.status = UserStatus.PENDING;
        u.agencyId = agency.id;
        users.save(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(
                "Inscription enregistrée. Votre compte sera accessible après validation par le syndic."));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.password))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));
        // La connexion marche depuis n'importe quelle adresse : l'accès aux données dépend de l'agence du compte
        // (jeton -> utilisateur -> agence), pas du lien utilisé. Le front prend ensuite la marque de cette agence.
        Agency own = null;
        if (u.role != Role.SUPER_ADMIN) {
            own = u.agencyId == null ? null : agencies.findById(u.agencyId).orElse(null);
            if (own == null || !own.active) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette agence est désactivée. Contactez votre syndic.");
            }
        }
        if (u.status == UserStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Votre compte est en attente de validation par le syndic.");
        }
        if (u.status == UserStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Votre demande d'accès a été refusée. Contactez le syndic.");
        }
        return new AuthResponse(jwt.generate(u.email), toDto(u), own == null ? null : AgencyService.toPublic(own));
    }

    @GetMapping("/me")
    public UserDto me(Authentication a) {
        return toDto(current.get(a));
    }
}
