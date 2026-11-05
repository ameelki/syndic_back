package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import lu.easy.syndic.dto.Dtos.*;
import lu.easy.syndic.entity.Agency;
import lu.easy.syndic.entity.Role;
import lu.easy.syndic.entity.User;
import lu.easy.syndic.entity.UserStatus;
import lu.easy.syndic.repository.AgencyRepository;
import lu.easy.syndic.repository.BuildingRepository;
import lu.easy.syndic.repository.UserRepository;
import lu.easy.syndic.service.AgencyService;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agences : marque publique (nom, logo, couleurs) lue par le front, réglage de sa propre marque par le syndic,
 * et gestion de toutes les agences par le super administrateur.
 */
@RestController
@RequestMapping("/api")
public class AgencyController {

    private static final String DEFAULT_PRIMARY = "#1f2b33";
    private static final String DEFAULT_ACCENT = "#f0b429";

    private final AgencyRepository agencies;
    private final AgencyService agencyService;
    private final UserRepository users;
    private final BuildingRepository buildings;
    private final PasswordEncoder encoder;
    private final CurrentUser currentUser;

    public AgencyController(AgencyRepository agencies, AgencyService agencyService, UserRepository users,
                            BuildingRepository buildings, PasswordEncoder encoder, CurrentUser currentUser) {
        this.agencies = agencies;
        this.agencyService = agencyService;
        this.users = users;
        this.buildings = buildings;
        this.encoder = encoder;
        this.currentUser = currentUser;
    }

    // ----- Public : le front se construit à partir de ces informations -----
    @GetMapping({"/auth/agency", "/auth/agency/{slug}"})
    public AgencyPublic publicInfo(@PathVariable(required = false) String slug) {
        return AgencyService.toPublic(agencyService.resolveActive(slug));
    }

    // ----- Syndic : sa propre agence -----
    @GetMapping("/admin/agency")
    public AgencyPublic mine(Authentication a) {
        return AgencyService.toPublic(own(a));
    }

    @PutMapping("/admin/agency")
    @Transactional
    public AgencyPublic updateMine(@Valid @RequestBody AgencyBranding b, Authentication a) {
        Agency ag = own(a);
        ag.name = b.name().trim();
        ag.tagline = blankToNull(b.tagline());
        ag.logoUrl = blankToNull(b.logoUrl());
        ag.primaryColor = orDefault(b.primaryColor(), DEFAULT_PRIMARY);
        ag.accentColor = orDefault(b.accentColor(), DEFAULT_ACCENT);
        return AgencyService.toPublic(agencies.save(ag));
    }

    private Agency own(Authentication a) {
        Long id = currentUser.get(a).agencyId;
        return agencies.findById(id).orElseThrow(() -> CurrentUser.notFound("Agence"));
    }

    // ----- Super administrateur : toutes les agences -----
    @GetMapping("/super/agencies")
    @Transactional(readOnly = true)
    public List<AgencyRow> list() {
        return agencies.findAll().stream().map(this::row).toList();
    }

    /** Crée l'agence et son premier compte syndic (déjà validé). */
    @PostMapping("/super/agencies")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public AgencyRow create(@Valid @RequestBody AgencyCreate r) {
        String slug = r.slug().trim().toLowerCase();
        if (AgencyService.RESERVED.contains(slug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet identifiant d'agence est réservé");
        }
        if (agencies.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet identifiant d'agence est déjà utilisé");
        }
        if (isBlank(r.adminEmail()) || isBlank(r.adminPassword()) || isBlank(r.adminFirstName()) || isBlank(r.adminLastName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Prénom, nom, email et mot de passe du syndic de l'agence sont obligatoires");
        }
        String email = r.adminEmail().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        }
        Agency ag = new Agency();
        ag.slug = slug;
        apply(ag, r);
        agencies.save(ag);

        User admin = new User();
        admin.firstName = r.adminFirstName().trim();
        admin.lastName = r.adminLastName().trim();
        admin.email = email;
        admin.password = encoder.encode(r.adminPassword());
        admin.role = Role.ADMIN;
        admin.status = UserStatus.ACTIVE;
        admin.approvedAt = LocalDateTime.now();
        admin.agencyId = ag.id;
        users.save(admin);
        return row(ag);
    }

    /** Modifie la marque et l'état (actif / désactivé) ; l'identifiant d'URL ne change pas. */
    @PutMapping("/super/agencies/{id}")
    @Transactional
    public AgencyRow update(@PathVariable Long id, @Valid @RequestBody AgencyCreate r) {
        Agency ag = agencies.findById(id).orElseThrow(() -> CurrentUser.notFound("Agence"));
        apply(ag, r);
        return row(agencies.save(ag));
    }

    private void apply(Agency ag, AgencyCreate r) {
        ag.name = r.name().trim();
        ag.tagline = blankToNull(r.tagline());
        ag.logoUrl = blankToNull(r.logoUrl());
        ag.primaryColor = orDefault(r.primaryColor(), DEFAULT_PRIMARY);
        ag.accentColor = orDefault(r.accentColor(), DEFAULT_ACCENT);
        if (r.active() != null) ag.active = r.active();
    }

    private AgencyRow row(Agency a) {
        return new AgencyRow(a.id, a.slug, a.name, a.tagline, a.logoUrl, a.primaryColor, a.accentColor, a.active,
                buildings.countByAgencyId(a.id), users.countByAgencyIdAndRole(a.id, Role.OWNER), a.createdAt);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String blankToNull(String s) { return isBlank(s) ? null : s.trim(); }
    private static String orDefault(String s, String d) { return isBlank(s) ? d : s.trim(); }
}
