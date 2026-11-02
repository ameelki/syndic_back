package lu.easy.syndic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resolutions")
public class Resolution extends BaseEntity {
    @Column(nullable = false) public Long assemblyId;
    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200) public String title;
    @Size(max = 2000) @Column(length = 2000) public String description;
    @Enumerated(EnumType.STRING) @Column(length = 20) public MajorityType majority = MajorityType.SIMPLE;
}
