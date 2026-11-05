package lu.easy.syndic.repository;

import lu.easy.syndic.entity.Intervention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {
    List<Intervention> findAllBySupplierIdOrderByInterventionDateDesc(Long supplierId);
    void deleteAllBySupplierId(Long supplierId);
}
