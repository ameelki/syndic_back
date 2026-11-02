package lu.easy.syndic.repository;

import lu.easy.syndic.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findAllByReporterIdOrderByCreatedAtDesc(Long reporterId);
    long countByStatusNot(IncidentStatus status);
    long countByPriorityAndStatusNot(Priority priority, IncidentStatus status);
    long countByReporterIdAndStatusNot(Long reporterId, IncidentStatus status);
}
