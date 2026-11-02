package lu.easy.syndic.repository;

import lu.easy.syndic.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
