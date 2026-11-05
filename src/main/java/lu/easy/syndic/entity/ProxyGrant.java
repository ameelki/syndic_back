package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import java.time.LocalDateTime;

/** Procuration : fromOwner donne son vote à toOwner pour une assemblée. */
@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "proxy_grants", uniqueConstraints = @UniqueConstraint(columnNames = {"assemblyId", "fromOwnerId"}))
public class ProxyGrant extends AgencyScoped {
    @Column(nullable = false) public Long assemblyId;
    @Column(nullable = false) public Long fromOwnerId;
    @Column(nullable = false) public Long toOwnerId;
    @Column(updatable = false) public LocalDateTime createdAt = LocalDateTime.now();
}
