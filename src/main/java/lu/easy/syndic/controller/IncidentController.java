package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import lu.easy.syndic.dto.Dtos.*;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.IncidentRepository;
import lu.easy.syndic.repository.LotRepository;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class IncidentController {

    private final IncidentRepository repo;
    private final CurrentUser current;
    private final LotRepository lots;

    public IncidentController(IncidentRepository repo, CurrentUser current, LotRepository lots) {
        this.lots = lots;
        this.repo = repo;
        this.current = current;
    }

    /** Copropriétaire : ses signalements. */
    @GetMapping("/incidents")
    public List<Incident> mine(Authentication a) {
        return repo.findAllByReporterIdOrderByCreatedAtDesc(current.get(a).id);
    }

    @PostMapping("/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public Incident report(@Valid @RequestBody IncidentCreate r, Authentication a) {
        User u = current.get(a);
        Incident i = new Incident();
        i.title = r.title().trim();
        i.description = r.description();
        i.location = r.location();
        i.priority = r.priority() == null ? Priority.MEDIUM : r.priority();
        Set<Long> mine = lots.findAllByOwnerId(u.id).stream().map(l -> l.buildingId)
                .filter(b -> b != null).collect(Collectors.toSet());
        i.buildingId = r.buildingId() != null ? r.buildingId() : (mine.size() == 1 ? mine.iterator().next() : null);
        i.reporterId = u.id;
        i.reporterName = u.firstName + " " + u.lastName;
        return repo.save(i);
    }

    /** Syndic : tous les signalements. */
    @GetMapping("/admin/incidents")
    public List<Incident> all(@RequestParam(required = false) Long buildingId) {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(i -> buildingId == null || i.buildingId == null || buildingId.equals(i.buildingId)).toList();
    }

    @PutMapping("/admin/incidents/{id}")
    public Incident update(@PathVariable Long id, @Valid @RequestBody IncidentUpdate r) {
        Incident i = repo.findById(id).orElseThrow(() -> CurrentUser.notFound("Incident"));
        i.status = r.status();
        i.priority = r.priority();
        i.adminComment = r.adminComment();
        i.resolvedAt = r.status() == IncidentStatus.RESOLVED ? (i.resolvedAt == null ? LocalDateTime.now() : i.resolvedAt) : null;
        return repo.save(i);
    }

    @DeleteMapping("/admin/incidents/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) throw CurrentUser.notFound("Incident");
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
