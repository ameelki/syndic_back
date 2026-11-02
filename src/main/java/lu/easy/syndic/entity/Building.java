package lu.easy.syndic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "buildings")
public class Building extends BaseEntity {
    @NotBlank @Size(max = 120) @Column(nullable = false, unique = true, length = 120) public String name;
    @Size(max = 200) public String address;
    @Size(max = 20) public String postalCode;
    @Size(max = 100) public String city;
    @Size(max = 1000) @Column(length = 1000) public String notes;
}
