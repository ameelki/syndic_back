package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "charge_calls")
public class ChargeCall extends BuildingScoped {
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) public String title;
    @NotNull @DecimalMin("0.01") @Column(nullable = false, precision = 12, scale = 2) public BigDecimal totalAmount;
    @NotNull @Column(nullable = false) public LocalDate dueDate;
    @Column(updatable = false) public LocalDateTime createdAt = LocalDateTime.now();
}
