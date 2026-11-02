package lu.easy.syndic.controller;

import lu.easy.syndic.dto.Dtos.UserDto;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.LotRepository;
import lu.easy.syndic.repository.UserRepository;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@Transactional
public class AdminUserController {

    private final UserRepository users;
    private final LotRepository lots;

    public AdminUserController(UserRepository users, LotRepository lots) {
        this.users = users;
        this.lots = lots;
    }

    private User find(Long id) {
        User u = users.findById(id).orElseThrow(() -> CurrentUser.notFound("Utilisateur"));
        if (u.role == Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte syndic non modifiable");
        return u;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserDto> list(@RequestParam(required = false) UserStatus status) {
        List<User> list = status == null ? users.findAllByRoleOrderByLastNameAsc(Role.OWNER)
                : users.findAllByStatusOrderByCreatedAtDesc(status);
        return list.stream().filter(u -> u.role == Role.OWNER).map(AuthController::toDto).toList();
    }

    /** Valide l'inscription ; peut rattacher le copropriétaire à un lot. */
    @PostMapping("/{id}/approve")
    public UserDto approve(@PathVariable Long id, @RequestParam(required = false) Long lotId) {
        User u = find(id);
        u.status = UserStatus.ACTIVE;
        u.approvedAt = LocalDateTime.now();
        users.save(u);
        if (lotId != null) assign(u, lotId);
        return AuthController.toDto(u);
    }

    @PostMapping("/{id}/reject")
    public UserDto reject(@PathVariable Long id) {
        User u = find(id);
        u.status = UserStatus.REJECTED;
        users.save(u);
        lots.findAllByOwnerId(u.id).forEach(l -> { l.ownerId = null; lots.save(l); });
        return AuthController.toDto(u);
    }

    @PostMapping("/{id}/assign-lot")
    public UserDto assignLot(@PathVariable Long id, @RequestParam Long lotId) {
        User u = find(id);
        if (u.status != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le compte doit d'abord être validé");
        }
        assign(u, lotId);
        return AuthController.toDto(u);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User u = find(id);
        lots.findAllByOwnerId(u.id).forEach(l -> { l.ownerId = null; lots.save(l); });
        users.delete(u);
        return ResponseEntity.noContent().build();
    }

    private void assign(User u, Long lotId) {
        Lot l = lots.findById(lotId).orElseThrow(() -> CurrentUser.notFound("Lot"));
        l.ownerId = u.id;
        lots.save(l);
    }
}
