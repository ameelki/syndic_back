package lu.easy.syndic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assemblies")
public class Assembly extends BuildingScoped {
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) public String title;
    @NotNull @Column(nullable = false) public LocalDateTime meetingDate;
    @Size(max = 200) public String location;
    @Size(max = 4000) @Column(length = 4000) public String agenda;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public AssemblyStatus status = AssemblyStatus.PLANNED;
    /** Quorum exigé (% des tantièmes ayant voté) ; 0 = pas de quorum. */
    @Min(0) @Max(100) public Integer quorumPercent = 0;
    /** Le vote se ferme automatiquement à cette date (facultatif). */
    public LocalDateTime votingDeadline;
}
