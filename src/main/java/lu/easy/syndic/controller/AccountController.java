package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lu.easy.syndic.dto.Dtos.MessageResponse;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.*;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/** Mon compte : changement de mot de passe, export de mes données et suppression (RGPD). */
@RestController
@RequestMapping("/api/me")
@Transactional
public class AccountController {

    public record PasswordChange(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record DeleteRequest(@NotBlank String password) {}

    private final CurrentUser current;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final LotRepository lots;
    private final ChargeRepository charges;
    private final IncidentRepository incidents;
    private final ContactMessageRepository messages;
    private final ReservationRepository reservations;
    private final VoteRepository votes;
    private final ProxyGrantRepository proxies;
    private final NotificationRepository notifications;
    private final PasswordTokenRepository tokens;

    public AccountController(CurrentUser current, UserRepository users, PasswordEncoder encoder, LotRepository lots,
                             ChargeRepository charges, IncidentRepository incidents, ContactMessageRepository messages,
                             ReservationRepository reservations, VoteRepository votes, ProxyGrantRepository proxies,
                             NotificationRepository notifications, PasswordTokenRepository tokens) {
        this.current = current;
        this.users = users;
        this.encoder = encoder;
        this.lots = lots;
        this.charges = charges;
        this.incidents = incidents;
        this.messages = messages;
        this.reservations = reservations;
        this.votes = votes;
        this.proxies = proxies;
        this.notifications = notifications;
        this.tokens = tokens;
    }

    @PostMapping("/password")
    public MessageResponse changePassword(@Valid @RequestBody PasswordChange r, Authentication a) {
        User u = current.get(a);
        if (!encoder.matches(r.currentPassword(), u.password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect");
        }
        u.password = encoder.encode(r.newPassword());
        users.save(u);
        return new MessageResponse("Mot de passe modifié.");
    }

    /** Toutes les données personnelles de l'utilisateur, au format JSON (droit d'accès / portabilité). */
    @GetMapping("/export")
    @Transactional(readOnly = true)
    public Map<String, Object> export(Authentication a) {
        User u = current.get(a);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("prenom", u.firstName);
        profile.put("nom", u.lastName);
        profile.put("email", u.email);
        profile.put("telephone", u.phone);
        profile.put("lotDemande", u.requestedLot);
        profile.put("role", u.role);
        profile.put("statut", u.status);
        profile.put("creeLe", u.createdAt);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exporteLe", LocalDateTime.now());
        out.put("profil", profile);
        out.put("lots", lots.findAllByOwnerId(u.id).stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("numero", l.number);
            m.put("tantiemes", l.tantiemes);
            return m;
        }).toList());
        out.put("charges", charges.findAllByOwnerIdOrderByIdDesc(u.id).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("montant", c.amount);
            m.put("statut", c.status);
            m.put("payeLe", c.paidAt);
            return m;
        }).toList());
        out.put("signalements", incidents.findAllByReporterIdOrderByCreatedAtDesc(u.id).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("titre", i.title);
            m.put("description", i.description);
            m.put("statut", i.status);
            m.put("date", i.createdAt);
            return m;
        }).toList());
        out.put("messages", messages.findAllByOwnerIdOrderByCreatedAtDesc(u.id).stream().map(x -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("objet", x.subject);
            m.put("contenu", x.content);
            m.put("reponse", x.reply);
            m.put("date", x.createdAt);
            return m;
        }).toList());
        out.put("reservations", reservations.findAllByOwnerIdOrderByDateDesc(u.id).stream().map(x -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("espace", x.space);
            m.put("date", x.date);
            m.put("debut", x.startTime);
            m.put("fin", x.endTime);
            m.put("statut", x.status);
            return m;
        }).toList());
        out.put("votes", votes.findAllByOwnerId(u.id).stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("resolution", v.resolutionId);
            m.put("choix", v.choice);
            m.put("poids", v.weight);
            return m;
        }).toList());
        return out;
    }

    /**
     * Suppression du compte : les données personnelles sont effacées ou anonymisées ; les écritures comptables
     * (charges) sont conservées sans lien nominatif.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteRequest r, Authentication a) {
        User u = current.get(a);
        if (!encoder.matches(r.password(), u.password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe incorrect");
        }
        if (u.role == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le compte de la plateforme ne peut pas être supprimé ici");
        }
        if (u.role == Role.ADMIN && users.findAllByAgencyIdAndRole(u.agencyId, Role.ADMIN).stream()
                .filter(x -> x.status == UserStatus.ACTIVE).count() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous êtes le seul gestionnaire de l'agence : invitez un collègue avant de supprimer votre compte");
        }
        lots.findAllByOwnerId(u.id).forEach(l -> { l.ownerId = null; lots.save(l); });
        messages.deleteAllByOwnerId(u.id);
        reservations.deleteAllByOwnerId(u.id);
        notifications.deleteAllByUserId(u.id);
        tokens.deleteAllByUserId(u.id);
        proxies.deleteAllByFromOwnerIdOrToOwnerId(u.id, u.id);
        incidents.findAllByReporterIdOrderByCreatedAtDesc(u.id).forEach(i -> { i.reporterId = null; i.reporterName = "Compte supprimé"; incidents.save(i); });
        u.firstName = "Compte";
        u.lastName = "supprimé";
        u.email = "supprime-" + u.id + "@invalid.local";
        u.phone = null;
        u.requestedLot = null;
        u.password = encoder.encode(UUID.randomUUID().toString());
        u.status = UserStatus.REJECTED;
        users.save(u);
        return ResponseEntity.noContent().build();
    }
}
