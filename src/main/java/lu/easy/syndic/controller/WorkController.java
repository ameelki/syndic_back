package lu.easy.syndic.controller;

import lu.easy.syndic.entity.Work;
import lu.easy.syndic.repository.WorkRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/works")
public class WorkController extends CrudController<Work> {

    public WorkController(WorkRepository repo) {
        super(repo, "Travaux");
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.DESC, "id");
    }
}
