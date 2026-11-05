package lu.easy.syndic.controller;

import lu.easy.syndic.entity.Deadline;
import lu.easy.syndic.repository.DeadlineRepository;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Échéances à surveiller (syndic uniquement : tout /api/admin/** est réservé au syndic). */
@RestController
@RequestMapping("/api/admin/deadlines")
public class DeadlineController extends CrudController<Deadline> {

    public DeadlineController(DeadlineRepository repo) {
        super(repo, "Échéance");
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.ASC, "dueDate");
    }

    /** « Faite » : reporte la date si l'échéance est récurrente, sinon la clôture. */
    @PostMapping("/{id}/done")
    @Transactional
    public Deadline done(@PathVariable Long id) {
        Deadline d = repo.findById(id).orElseThrow(() -> CurrentUser.notFound("Échéance"));
        if (d.recurrenceMonths != null && d.recurrenceMonths > 0) {
            d.dueDate = d.dueDate.plusMonths(d.recurrenceMonths);
            d.notifiedFor = null;
        } else {
            d.done = true;
        }
        activity.log("Échéance traitée", d.title);
        return repo.save(d);
    }
}
