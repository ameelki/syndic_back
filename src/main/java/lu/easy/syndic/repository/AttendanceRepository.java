package lu.easy.syndic.repository;

import lu.easy.syndic.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findAllByAssemblyId(Long assemblyId);
    Optional<Attendance> findByAssemblyIdAndOwnerId(Long assemblyId, Long ownerId);
    void deleteAllByAssemblyId(Long assemblyId);
}
