package lu.easy.syndic.repository;

import lu.easy.syndic.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResolutionRepository extends JpaRepository<Resolution, Long> {
    List<Resolution> findAllByAssemblyId(Long assemblyId);
    void deleteAllByAssemblyId(Long assemblyId);
}
