package lu.easy.syndic.repository;

import lu.easy.syndic.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop30ByUserIdOrderByIdDesc(Long userId);
    long countByUserIdAndReadAtIsNull(Long userId);
    List<Notification> findAllByUserIdAndReadAtIsNull(Long userId);
    void deleteAllByUserId(Long userId);
}
