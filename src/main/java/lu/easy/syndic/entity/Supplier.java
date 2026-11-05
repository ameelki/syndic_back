package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "suppliers")
public class Supplier extends BuildingScoped {
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) public String name;
    @Size(max = 100) public String trade;
    @Size(max = 100) public String contactName;
    @Size(max = 40) public String phone;
    @Size(max = 190) public String email;
    @DecimalMin("0.0") @Column(precision = 12, scale = 2) public BigDecimal annualCost;
    public LocalDate contractStart;
    public LocalDate contractEnd;
    @Size(max = 1000) @Column(length = 1000) public String notes;
}
