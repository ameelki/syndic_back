package lu.easy.syndic.repository;

import lu.easy.syndic.entity.ProxyGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProxyGrantRepository extends JpaRepository<ProxyGrant, Long> {
    List<ProxyGrant> findAllByAssemblyId(Long assemblyId);
    List<ProxyGrant> findAllByAssemblyIdAndToOwnerId(Long assemblyId, Long toOwnerId);
    Optional<ProxyGrant> findByAssemblyIdAndFromOwnerId(Long assemblyId, Long fromOwnerId);
    void deleteAllByAssemblyId(Long assemblyId);
}
