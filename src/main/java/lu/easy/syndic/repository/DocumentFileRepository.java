package lu.easy.syndic.repository;

import lu.easy.syndic.entity.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {
    Optional<DocumentFile> findByDocumentId(Long documentId);
    void deleteAllByDocumentId(Long documentId);
}
