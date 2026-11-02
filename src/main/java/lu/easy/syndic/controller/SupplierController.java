package lu.easy.syndic.controller;

import lu.easy.syndic.entity.Supplier;
import lu.easy.syndic.repository.SupplierRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/suppliers")
public class SupplierController extends CrudController<Supplier> {

    public SupplierController(SupplierRepository repo) {
        super(repo, "Fournisseur");
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.ASC, "name");
    }
}
