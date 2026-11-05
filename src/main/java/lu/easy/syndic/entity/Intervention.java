package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Intervention d'un prestataire (historique par prestataire). */
@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "interventions")
public class Intervention extends BuildingScoped {
    @Column(nullable = false) public Long supplierId;
    @NotNull @Column(nullable = false) public LocalDate interventionDate;
    @NotBlank @Size(max = 500) @Column(nullable = false, length = 500) public String description;
    @DecimalMin("0.0") @Column(precision = 12, scale = 2) public BigDecimal cost;
    @Size(max = 100) public String invoiceRef;
}
