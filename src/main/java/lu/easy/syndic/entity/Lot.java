package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "lots", uniqueConstraints = @UniqueConstraint(columnNames = {"number", "building_id"}))
public class Lot extends BuildingScoped {
    @NotBlank @Size(max = 40) @Column(nullable = false, length = 40) public String number;
    @Size(max = 80) public String building;
    public Integer floor;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public LotType type = LotType.APARTMENT;
    @DecimalMin("0.0") public BigDecimal surface;
    @Min(1) @Column(nullable = false) public int tantiemes = 1;
    public Long ownerId;
}
