package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "resolutions")
public class Resolution extends AgencyScoped {
    @Column(nullable = false) public Long assemblyId;
    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200) public String title;
    @Size(max = 2000) @Column(length = 2000) public String description;
    @Enumerated(EnumType.STRING) @Column(length = 20) public MajorityType majority = MajorityType.SIMPLE;
}
