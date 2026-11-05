package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "budgets", uniqueConstraints = @UniqueConstraint(columnNames = {"year", "building_id"}))
public class Budget extends BuildingScoped {
    @Min(2000) @Max(2100) @Column(nullable = false) public int year;
    @NotNull @DecimalMin("0.0") @Column(nullable = false, precision = 12, scale = 2) public BigDecimal amount;
    @Size(max = 1000) @Column(length = 1000) public String notes;
}
