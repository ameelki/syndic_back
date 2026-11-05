package lu.easy.syndic.controller;

import lu.easy.syndic.entity.ActivityLog;
import lu.easy.syndic.repository.ActivityLogRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/activity")
@Transactional(readOnly = true)
public class ActivityController {

    private final ActivityLogRepository repo;

    public ActivityController(ActivityLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ActivityLog> latest() {
        return repo.findTop200ByOrderByIdDesc();
    }
}
