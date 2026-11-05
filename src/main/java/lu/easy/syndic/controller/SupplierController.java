package lu.easy.syndic.controller;

import lu.easy.syndic.entity.Supplier;
import lu.easy.syndic.repository.InterventionRepository;
import lu.easy.syndic.repository.SupplierRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/suppliers")
public class SupplierController extends CrudController<Supplier> {

    private final InterventionRepository interventions;

    public SupplierController(SupplierRepository repo, InterventionRepository interventions) {
        super(repo, "Fournisseur");
        this.interventions = interventions;
    }

    @Override
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        interventions.deleteAllBySupplierId(id);
        return super.delete(id);
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.ASC, "name");
    }
}
