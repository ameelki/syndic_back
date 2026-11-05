package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Échéance à surveiller : contrôle d'ascenseur, assurance, contrat d'entretien, etc. */
@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "deadlines")
public class Deadline extends BuildingScoped {
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) public String title;
    @Size(max = 40) @Column(length = 40) public String category;
    @NotNull @Column(nullable = false) public LocalDate dueDate;
    /** Si renseigné, l'échéance se reporte automatiquement de ce nombre de mois quand on la marque « faite ». */
    @Min(0) @Max(240) public Integer recurrenceMonths;
    public Boolean done = false;
    @Size(max = 1000) @Column(length = 1000) public String notes;
    /** Date de la dernière alerte envoyée au syndic (pour ne prévenir qu'une fois par échéance). */
    public LocalDate notifiedFor;
}
