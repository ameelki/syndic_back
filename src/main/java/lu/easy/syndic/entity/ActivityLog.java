package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "activity_logs")
public class ActivityLog extends AgencyScoped {
    public Long actorId;
    @Column(length = 200) public String actorName;
    @Column(nullable = false, length = 60) public String action;
    @Column(length = 250) public String target;
    public LocalDateTime createdAt = LocalDateTime.now();
}
