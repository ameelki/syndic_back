package lu.easy.syndic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident extends BuildingScoped {
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) public String title;
    @Size(max = 2000) @Column(length = 2000) public String description;
    @Size(max = 150) public String location;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public IncidentStatus status = IncidentStatus.OPEN;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public Priority priority = Priority.MEDIUM;
    public Long reporterId;
    public String reporterName;
    @Size(max = 1000) @Column(length = 1000) public String adminComment;
    @Column(updatable = false) public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime resolvedAt;
}
