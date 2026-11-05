package lu.easy.syndic.controller;

import lu.easy.syndic.dto.Dtos.BuildingSummary;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.*;
import lu.easy.syndic.service.AgencyService;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Gestion des bâtiments (syndic) + liste des bâtiments visibles pour l'utilisateur connecté. */
@RestController
@RequestMapping("/api")
public class BuildingController {

    private final BuildingRepository buildings;
    private final LotRepository lots;
    private final ChargeRepository charges;
    private final IncidentRepository incidents;
    private final CurrentUser currentUser;
    private final AgencyService agencyService;

    public BuildingController(BuildingRepository repo, LotRepository lots, ChargeRepository charges,
                              IncidentRepository incidents, CurrentUser currentUser, AgencyService agencyService) {
        this.agencyService = agencyService;
        this.currentUser = currentUser;
        this.buildings = repo;
        this.lots = lots;
        this.charges = charges;
        this.incidents = incidents;
    }

    private Sort sort() {
        return Sort.by(Sort.Direction.ASC, "name");
    }

    // ----- CRUD syndic -----
    @GetMapping("/admin/buildings")
    public List<Building> adminList() {
        return buildings.findAll(sort());
    }

    @GetMapping("/admin/buildings/{id}")
    public Building adminGet(@PathVariable Long id) {
        return buildings.findById(id).orElseThrow(() -> CurrentUser.notFound("Bâtiment"));
    }

    @PostMapping("/admin/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public Building adminCreate(@jakarta.validation.Valid @RequestBody Building b) {
        b.id = null;
        b.agencyId = null;
        return buildings.save(b);
    }

    @PutMapping("/admin/buildings/{id}")
    public Building adminUpdate(@PathVariable Long id, @jakarta.validation.Valid @RequestBody Building b) {
        Building existing = buildings.findById(id).orElseThrow(() -> CurrentUser.notFound("Bâtiment"));
        b.id = id;
        b.agencyId = existing.agencyId;
        return buildings.save(b);
    }

    @DeleteMapping("/admin/buildings/{id}")
    public ResponseEntity<Void> adminDelete(@PathVariable Long id) {
        if (!buildings.existsById(id)) throw CurrentUser.notFound("Bâtiment");
        if (lots.existsByBuildingId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce bâtiment contient encore des lots : détachez-les d'abord");
        }
        buildings.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Vue d'ensemble : une ligne par bâtiment. */
    @GetMapping("/admin/buildings/summary")
    public List<BuildingSummary> summary() {
        List<Lot> allLots = lots.findAll();
        Map<Long, Long> lotBuilding = allLots.stream().filter(l -> l.buildingId != null)
                .collect(Collectors.toMap(l -> l.id, l -> l.buildingId));
        List<Charge> unpaid = charges.findAllByStatus(ChargeStatus.UNPAID);
        List<Incident> open = incidents.findAll().stream().filter(i -> i.status != IncidentStatus.RESOLVED).toList();
        return buildings.findAll(sort()).stream().map(b -> {
            List<Lot> bl = allLots.stream().filter(l -> b.id.equals(l.buildingId)).toList();
            long owners = bl.stream().map(l -> l.ownerId).filter(Objects::nonNull).distinct().count();
            BigDecimal due = unpaid.stream().filter(c -> b.id.equals(lotBuilding.get(c.lotId)))
                    .map(c -> c.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            long inc = open.stream().filter(i -> b.id.equals(i.buildingId)).count();
            return new BuildingSummary(b.id, b.name, b.address, b.city, bl.size(),
                    bl.stream().mapToLong(l -> l.tantiemes).sum(), owners, due, inc);
        }).toList();
    }

    // ----- Tous les comptes : bâtiments visibles -----
    @GetMapping("/buildings")
    public List<Building> visible(Authentication a) {
        User u = currentUser.get(a);
        if (u.role == Role.ADMIN) return buildings.findAll(sort());
        Set<Long> ids = lots.findAllByOwnerId(u.id).stream().map(l -> l.buildingId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return buildings.findAllById(ids).stream().sorted(java.util.Comparator.comparing(b -> b.name)).toList();
    }

    /** Public : liste des noms pour le formulaire d'inscription, pour l'agence du lien (obligatoire). */
    @GetMapping("/auth/buildings")
    public List<Map<String, Object>> publicList(@RequestParam(required = false) String agency) {
        Agency ag = agencyService.resolveRequired(agency);
        return buildings.findAllByAgencyId(ag.id, sort()).stream()
                .map(b -> Map.<String, Object>of("id", b.id, "name", b.name, "city", b.city == null ? "" : b.city)).toList();
    }
}
