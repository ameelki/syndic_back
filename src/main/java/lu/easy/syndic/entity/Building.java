package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "buildings", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "agency_id"}))
public class Building extends AgencyScoped {
    @NotBlank @Size(max = 120) @Column(nullable = false, length = 120) public String name;
    @Size(max = 200) public String address;
    @Size(max = 20) public String postalCode;
    @Size(max = 100) public String city;
    @Size(max = 1000) @Column(length = 1000) public String notes;
}
