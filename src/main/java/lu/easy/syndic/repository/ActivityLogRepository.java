package lu.easy.syndic.repository;

import lu.easy.syndic.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findTop200ByOrderByIdDesc();
}
