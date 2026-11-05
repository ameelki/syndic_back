package lu.easy.syndic.service;

import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.*;
import lu.easy.syndic.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tâche quotidienne (8 h) : relance les charges en retard (3 relances max, 14 jours d'écart) et prévient les syndics
 * des échéances proches. Les relances sont désactivées par défaut (app.reminders.enabled=true pour les activer).
 */
@Component
public class ReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);

    private final ChargeRepository charges;
    private final ChargeCallRepository calls;
    private final DeadlineRepository deadlines;
    private final ChargeService chargeService;
    private final NotificationService notifs;
    private final boolean enabled;

    public ReminderJob(ChargeRepository charges, ChargeCallRepository calls, DeadlineRepository deadlines,
                       ChargeService chargeService, NotificationService notifs,
                       @Value("${app.reminders.enabled:false}") boolean enabled) {
        this.charges = charges;
        this.calls = calls;
        this.deadlines = deadlines;
        this.chargeService = chargeService;
        this.notifs = notifs;
        this.enabled = enabled;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void run() {
        deadlineAlerts();
        if (enabled) overdueReminders();
    }

    void overdueReminders() {
        LocalDate today = LocalDate.now();
        int sent = 0;
        for (Charge c : charges.findAllByStatus(ChargeStatus.UNPAID)) {
            int count = c.reminderCount == null ? 0 : c.reminderCount;
            if (count >= 3) continue;
            if (c.lastReminderAt != null && c.lastReminderAt.isAfter(LocalDateTime.now().minusDays(14))) continue;
            ChargeCall call = calls.findById(c.callId).orElse(null);
            if (call == null || call.dueDate == null || !call.dueDate.isBefore(today)) continue;
            try {
                TenantContext.set(c.agencyId);
                if (chargeService.sendReminder(c)) sent++;
            } catch (Exception e) {
                log.warn("Relance impossible pour la charge {} : {}", c.id, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
        if (sent > 0) log.info("{} relance(s) automatique(s) envoyée(s)", sent);
    }

    void deadlineAlerts() {
        LocalDate limit = LocalDate.now().plusDays(30);
        List<Deadline> due = deadlines.findAllByDoneFalseAndDueDateLessThanEqualOrderByDueDateAsc(limit);
        for (Deadline d : due) {
            if (d.dueDate.equals(d.notifiedFor)) continue;
            try {
                TenantContext.set(d.agencyId);
                notifs.notifyAdmins("Échéance proche : " + d.title, "À traiter avant le " + d.dueDate + ".", "/app/admin/deadlines");
                d.notifiedFor = d.dueDate;
                deadlines.save(d);
            } catch (Exception e) {
                log.warn("Alerte d'échéance impossible ({}) : {}", d.id, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
