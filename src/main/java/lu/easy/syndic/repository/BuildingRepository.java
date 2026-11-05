package lu.easy.syndic.repository;

import lu.easy.syndic.entity.Building;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    List<Building> findAllByAgencyId(Long agencyId, Sort sort);
    long countByAgencyId(Long agencyId);
}
