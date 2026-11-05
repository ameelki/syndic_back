package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "charges")
public class Charge extends AgencyScoped {
    @Column(nullable = false) public Long callId;
    @Column(nullable = false) public Long lotId;
    public Long ownerId;
    @Column(nullable = false, precision = 12, scale = 2) public BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public ChargeStatus status = ChargeStatus.UNPAID;
    public LocalDate paidAt;
    /** Nombre de relances envoyées et date de la dernière. */
    public Integer reminderCount = 0;
    public LocalDateTime lastReminderAt;
}
