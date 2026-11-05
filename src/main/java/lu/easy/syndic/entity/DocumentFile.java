package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

/** Contenu d'un document du coffre (séparé de DocumentItem pour ne pas alourdir les listes). */
@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "document_files", uniqueConstraints = @UniqueConstraint(columnNames = "documentId"))
public class DocumentFile extends AgencyScoped {
    @Column(nullable = false) public Long documentId;
    @Column(nullable = false) public byte[] data;
}
