package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.LotRepository;
import lu.easy.syndic.repository.ReservationRepository;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ReservationController {

    public record ReservationRequest(@NotBlank @Size(max = 100) String space, @NotNull LocalDate date,
                                     @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                                     @Size(max = 500) String note, Long buildingId) {}

    private final ReservationRepository repo;
    private final CurrentUser current;
    private final LotRepository lots;

    public ReservationController(ReservationRepository repo, CurrentUser current, LotRepository lots) {
        this.repo = repo;
        this.current = current;
        this.lots = lots;
    }

    private boolean overlaps(Reservation a, LocalTime s, LocalTime e) {
        return s.isBefore(a.endTime) && e.isAfter(a.startTime);
    }

    /** Deux réservations ne se chevauchent que dans le même bâtiment (un espace commun général compte pour tous). */
    private boolean conflict(String space, LocalDate date, LocalTime s, LocalTime e, Long ignoreId, Long buildingId) {
        return repo.findAllByDateAndSpaceAndStatus(date, space, ReservationStatus.APPROVED).stream()
                .filter(r -> r.buildingId == null || buildingId == null || r.buildingId.equals(buildingId))
                .anyMatch(r -> !r.id.equals(ignoreId) && overlaps(r, s, e));
    }

    @GetMapping("/reservations")
    public List<Reservation> mine(Authentication a) {
        return repo.findAllByOwnerIdOrderByDateDesc(current.get(a).id);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Reservation create(@Valid @RequestBody ReservationRequest r, Authentication a) {
        User u = current.get(a);
        Set<Long> mine = lots.findAllByOwnerId(u.id).stream().map(l -> l.buildingId).filter(Objects::nonNull).collect(Collectors.toSet());
        Long bid = r.buildingId() != null && mine.contains(r.buildingId()) ? r.buildingId()
                : (mine.size() == 1 ? mine.iterator().next() : null);
        if (!r.endTime().isAfter(r.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure de fin doit être après l'heure de début");
        }
        if (r.date().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date doit être dans le futur");
        }
        if (conflict(r.space().trim(), r.date(), r.startTime(), r.endTime(), null, bid)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce créneau est déjà réservé");
        }
        Reservation res = new Reservation();
        res.buildingId = bid;
        res.space = r.space().trim();
        res.date = r.date();
        res.startTime = r.startTime();
        res.endTime = r.endTime();
        res.note = r.note();
        res.ownerId = u.id;
        res.ownerName = u.firstName + " " + u.lastName;
        return repo.save(res);
    }

    /** Un copropriétaire peut annuler sa propre réservation. */
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id, Authentication a) {
        Reservation r = repo.findById(id).orElseThrow(() -> CurrentUser.notFound("Réservation"));
        if (!r.ownerId.equals(current.get(a).id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        repo.delete(r);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/reservations")
    public List<Reservation> all(@RequestParam(required = false) Long buildingId) {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "date")).stream()
                .filter(x -> buildingId == null || x.buildingId == null || buildingId.equals(x.buildingId)).toList();
    }

    @PatchMapping("/admin/reservations/{id}/status")
    public Reservation status(@PathVariable Long id, @RequestParam ReservationStatus status) {
        Reservation r = repo.findById(id).orElseThrow(() -> CurrentUser.notFound("Réservation"));
        if (status == ReservationStatus.APPROVED && conflict(r.space, r.date, r.startTime, r.endTime, r.id, r.buildingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un autre créneau validé chevauche celui-ci");
        }
        r.status = status;
        return repo.save(r);
    }
}
