package lu.easy.syndic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;

/** Une agence de syndic : ses données (bâtiments, copropriétaires…) sont isolées de celles des autres agences. */
@Entity
@Table(name = "agencies")
@FilterDef(name = "agencyFilter", parameters = @ParamDef(name = "agencyId", type = Long.class))
public class Agency extends BaseEntity {
    @NotBlank @Size(max = 120) @Column(nullable = false, length = 120) public String name;
    /** Identifiant d'URL (sous-domaine ou ?agence=) : minuscules, chiffres et tirets. */
    @NotBlank @Pattern(regexp = "^[a-z0-9]([a-z0-9-]{0,38}[a-z0-9])?$")
    @Column(nullable = false, unique = true, length = 40) public String slug;
    @Size(max = 200) public String tagline;
    @Size(max = 300) public String logoUrl;
    @Size(max = 7) @Column(length = 7) public String primaryColor = "#1f2b33";
    @Size(max = 7) @Column(length = 7) public String accentColor = "#f0b429";
    public boolean active = true;
    public LocalDateTime createdAt = LocalDateTime.now();
}
