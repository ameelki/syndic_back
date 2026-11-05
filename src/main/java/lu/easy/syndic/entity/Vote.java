package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "votes", uniqueConstraints = @UniqueConstraint(columnNames = {"resolutionId", "ownerId"}))
public class Vote extends AgencyScoped {
    @Column(nullable = false) public Long resolutionId;
    @Column(nullable = false) public Long ownerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public VoteChoice choice;
    @Column(nullable = false) public int weight;
}
