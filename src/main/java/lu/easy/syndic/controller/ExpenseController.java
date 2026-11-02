package lu.easy.syndic.controller;

import lu.easy.syndic.entity.Expense;
import lu.easy.syndic.repository.ExpenseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/expenses")
public class ExpenseController extends CrudController<Expense> {

    public ExpenseController(ExpenseRepository repo) {
        super(repo, "Dépense");
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.DESC, "expenseDate");
    }
}
