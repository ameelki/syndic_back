package lu.easy.syndic.controller;

import lu.easy.syndic.dto.AttendanceRow;
import lu.easy.syndic.entity.Assembly;
import lu.easy.syndic.repository.AssemblyRepository;
import lu.easy.syndic.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Convocation et feuille de présence d'une assemblée (syndic). */
@RestController
@RequestMapping("/api/admin/assemblies")
@Transactional
public class AssemblyAdminController {

    private final AssemblyRepository assemblies;
    private final AttendanceService attendance;
    private final NotificationService notifs;
    private final ActivityService activity;

    public AssemblyAdminController(AssemblyRepository assemblies, AttendanceService attendance,
                                   NotificationService notifs, ActivityService activity) {
        this.assemblies = assemblies;
        this.attendance = attendance;
        this.notifs = notifs;
        this.activity = activity;
    }

    private Assembly find(Long id) {
        return assemblies.findById(id).orElseThrow(() -> CurrentUser.notFound("Assemblée"));
    }

    @GetMapping("/{id}/attendance")
    @Transactional(readOnly = true)
    public List<AttendanceRow> attendance(@PathVariable Long id) {
        return attendance.rows(find(id));
    }

    @PutMapping("/{id}/attendance/{ownerId}")
    public List<AttendanceRow> setPresent(@PathVariable Long id, @PathVariable Long ownerId, @RequestParam boolean present) {
        Assembly a = find(id);
        attendance.setPresent(a, ownerId, present);
        return attendance.rows(a);
    }

    /** Envoie la convocation (notification + email) à tous les copropriétaires concernés. */
    @PostMapping("/{id}/convoke")
    public Map<String, Object> convoke(@PathVariable Long id) {
        Assembly a = find(id);
        if (a.meetingDate == null || a.meetingDate.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de l'assemblée est déjà passée");
        }
        String when = a.meetingDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm"));
        StringBuilder body = new StringBuilder("Vous êtes convoqué(e) à l'assemblée « " + a.title + " » le " + when);
        if (a.location != null && !a.location.isBlank()) body.append(", lieu : ").append(a.location);
        body.append('.');
        if (a.agenda != null && !a.agenda.isBlank()) body.append("\n\nOrdre du jour :\n").append(a.agenda);
        body.append("\n\nVous ne pouvez pas être présent(e) ? Donnez procuration à un autre copropriétaire depuis la page de l'assemblée.");
        int sent = notifs.notifyOwners(attendance.eligibleOwners(a), "Convocation : " + a.title, body.toString(), "/app/assemblies/" + a.id);
        a.convokedAt = LocalDateTime.now();
        assemblies.save(a);
        activity.log("Convocation envoyée", a.title + " (" + sent + " destinataires)");
        return Map.of("sent", sent, "convokedAt", a.convokedAt);
    }
}
