package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import java.time.LocalDateTime;

/** Photo jointe à un signalement (stockée en base : pas de disque à gérer sur un hébergement gratuit). */
@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "incident_photos")
public class IncidentPhoto extends AgencyScoped {
    @Column(nullable = false) public Long incidentId;
    @Column(length = 200) public String fileName;
    @Column(nullable = false, length = 60) public String contentType;
    @Column(nullable = false) public byte[] data;
    public LocalDateTime createdAt = LocalDateTime.now();
}
