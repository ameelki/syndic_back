package lu.easy.syndic.repository;

import lu.easy.syndic.entity.Deadline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DeadlineRepository extends JpaRepository<Deadline, Long> {
    List<Deadline> findAllByDoneFalseAndDueDateLessThanEqualOrderByDueDateAsc(LocalDate limit);
}
