package lu.easy.syndic.repository;

import lu.easy.syndic.entity.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgencyRepository extends JpaRepository<Agency, Long> {
    Optional<Agency> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
