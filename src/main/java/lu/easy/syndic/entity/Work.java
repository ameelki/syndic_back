package lu.easy.syndic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "works")
public class Work extends BuildingScoped {
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) public String title;
    @Size(max = 2000) @Column(length = 2000) public String description;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public WorkStatus status = WorkStatus.PLANNED;
    @DecimalMin("0.0") @Column(precision = 12, scale = 2) public BigDecimal budget;
    @Size(max = 150) public String supplier;
    public LocalDate startDate;
    public LocalDate endDate;
}
