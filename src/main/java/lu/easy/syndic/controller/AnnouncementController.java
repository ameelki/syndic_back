package lu.easy.syndic.controller;

import lu.easy.syndic.entity.Announcement;
import lu.easy.syndic.repository.AnnouncementRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController extends CrudController<Announcement> {

    public AnnouncementController(AnnouncementRepository repo) {
        super(repo, "Annonce");
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
