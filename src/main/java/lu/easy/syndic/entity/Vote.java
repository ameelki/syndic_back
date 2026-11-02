package lu.easy.syndic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "votes", uniqueConstraints = @UniqueConstraint(columnNames = {"resolutionId", "ownerId"}))
public class Vote extends BaseEntity {
    @Column(nullable = false) public Long resolutionId;
    @Column(nullable = false) public Long ownerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public VoteChoice choice;
    @Column(nullable = false) public int weight;
}
