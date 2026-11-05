package lu.easy.syndic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/** Donnée appartenant à une agence : isolée par un filtre Hibernate + contrôlée au chargement (voir AgencyListener). */
@MappedSuperclass
@EntityListeners(AgencyListener.class)
public abstract class AgencyScoped extends BaseEntity {
    @Column(name = "agency_id")
    public Long agencyId;
}
