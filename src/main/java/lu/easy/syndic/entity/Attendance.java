package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

/** Présence d'un copropriétaire à une assemblée (feuille de présence). */
@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "attendances", uniqueConstraints = @UniqueConstraint(columnNames = {"assemblyId", "ownerId"}))
public class Attendance extends AgencyScoped {
    @Column(nullable = false) public Long assemblyId;
    @Column(nullable = false) public Long ownerId;
    public boolean present;
}
