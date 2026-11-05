package lu.easy.syndic.repository;

import lu.easy.syndic.dto.FileMeta;
import lu.easy.syndic.entity.IncidentPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentPhotoRepository extends JpaRepository<IncidentPhoto, Long> {
    @Query("select new lu.easy.syndic.dto.FileMeta(p.id, p.fileName, p.contentType) from IncidentPhoto p where p.incidentId = :incidentId order by p.id")
    List<FileMeta> metas(@Param("incidentId") Long incidentId);

    long countByIncidentId(Long incidentId);

    void deleteAllByIncidentId(Long incidentId);
}
