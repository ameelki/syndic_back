package lu.easy.syndic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "charges")
public class Charge extends BaseEntity {
    @Column(nullable = false) public Long callId;
    @Column(nullable = false) public Long lotId;
    public Long ownerId;
    @Column(nullable = false, precision = 12, scale = 2) public BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public ChargeStatus status = ChargeStatus.UNPAID;
    public LocalDate paidAt;
}
