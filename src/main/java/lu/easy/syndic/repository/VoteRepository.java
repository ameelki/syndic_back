package lu.easy.syndic.repository;

import lu.easy.syndic.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findAllByResolutionId(Long resolutionId);
    Optional<Vote> findByResolutionIdAndOwnerId(Long resolutionId, Long ownerId);
    void deleteAllByResolutionId(Long resolutionId);
}
