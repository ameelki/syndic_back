package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "notifications")
public class Notification extends AgencyScoped {
    @Column(nullable = false) public Long userId;
    @Column(nullable = false, length = 150) public String title;
    @Column(length = 500) public String body;
    @Column(length = 200) public String link;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime readAt;
}
