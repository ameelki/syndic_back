package lu.easy.syndic.controller;

import lu.easy.syndic.entity.Lot;
import lu.easy.syndic.repository.LotRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/lots")
public class LotController extends CrudController<Lot> {

    public LotController(LotRepository repo) {
        super(repo, "Lot");
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.ASC, "number");
    }
}
