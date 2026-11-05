package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import lu.easy.syndic.entity.Intervention;
import lu.easy.syndic.repository.InterventionRepository;
import lu.easy.syndic.repository.SupplierRepository;
import lu.easy.syndic.service.ActivityService;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Historique des interventions par prestataire (syndic). */
@RestController
@RequestMapping("/api/admin")
@Transactional
public class InterventionController {

    private final InterventionRepository repo;
    private final SupplierRepository suppliers;
    private final ActivityService activity;

    public InterventionController(InterventionRepository repo, SupplierRepository suppliers, ActivityService activity) {
        this.repo = repo;
        this.suppliers = suppliers;
        this.activity = activity;
    }

    @GetMapping("/suppliers/{id}/interventions")
    @Transactional(readOnly = true)
    public List<Intervention> list(@PathVariable Long id) {
        suppliers.findById(id).orElseThrow(() -> CurrentUser.notFound("Prestataire"));
        return repo.findAllBySupplierIdOrderByInterventionDateDesc(id);
    }

    @PostMapping("/suppliers/{id}/interventions")
    @ResponseStatus(HttpStatus.CREATED)
    public Intervention add(@PathVariable Long id, @Valid @RequestBody Intervention body) {
        var s = suppliers.findById(id).orElseThrow(() -> CurrentUser.notFound("Prestataire"));
        body.id = null;
        body.agencyId = null;
        body.supplierId = id;
        if (body.buildingId == null) body.buildingId = s.buildingId;
        Intervention saved = repo.save(body);
        activity.log("Intervention ajoutée", s.name + " — " + saved.description);
        return saved;
    }

    @DeleteMapping("/interventions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) throw CurrentUser.notFound("Intervention");
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
