package lu.easy.syndic.repository;

import lu.easy.syndic.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChargeRepository extends JpaRepository<Charge, Long> {
    List<Charge> findAllByOwnerIdOrderByIdDesc(Long ownerId);
    List<Charge> findAllByCallId(Long callId);
    List<Charge> findAllByStatus(ChargeStatus status);
    void deleteAllByCallId(Long callId);
}
