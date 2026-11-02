package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import lu.easy.syndic.dto.Dtos.*;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.UserRepository;
import lu.easy.syndic.security.JwtService;
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

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt, CurrentUser current) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.current = current;
    }

    public static UserDto toDto(User u) {
        return new UserDto(u.id, u.firstName, u.lastName, u.email, u.phone, u.requestedLot, u.role, u.status,
                u.createdAt, u.approvedAt);
    }

    /** L'inscription crée un compte EN ATTENTE : le syndic doit le valider avant toute connexion. */
    @PostMapping("/auth/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest r) {
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
        users.save(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(
                "Inscription enregistrée. Votre compte sera accessible après validation par le syndic."));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.password))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));
        if (u.status == UserStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Votre compte est en attente de validation par le syndic.");
        }
        if (u.status == UserStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Votre demande d'accès a été refusée. Contactez le syndic.");
        }
        return new AuthResponse(jwt.generate(u.email), toDto(u));
    }

    @GetMapping("/me")
    public UserDto me(Authentication a) {
        return toDto(current.get(a));
    }
}
