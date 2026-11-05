package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "documents")
public class DocumentItem extends BuildingScoped {
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) public String title;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public DocumentCategory category = DocumentCategory.OTHER;
    @Size(max = 500) @Column(length = 500) public String url;
    @Size(max = 1000) @Column(length = 1000) public String notes;
    /** Fichier téléversé (le contenu est dans DocumentFile). */
    @Size(max = 200) public String fileName;
    @Size(max = 100) public String fileType;
    public Long fileSize;
    /** Faux = document réservé au syndic. */
    public Boolean ownersVisible = true;
    @Column(updatable = false) public LocalDateTime createdAt = LocalDateTime.now();
}
