package lu.easy.syndic.controller;

import lu.easy.syndic.entity.Budget;
import lu.easy.syndic.repository.BudgetRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/budgets")
public class BudgetController extends CrudController<Budget> {

    public BudgetController(BudgetRepository repo) {
        super(repo, "Budget");
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.DESC, "year");
    }
}
