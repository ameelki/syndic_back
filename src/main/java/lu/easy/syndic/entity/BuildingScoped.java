package lu.easy.syndic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/** Entité rattachable à un bâtiment (buildingId null = toute la copropriété / tous les bâtiments). */
@MappedSuperclass
public abstract class BuildingScoped extends BaseEntity {
    @Column(name = "building_id")
    public Long buildingId;
}
