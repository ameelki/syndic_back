package lu.easy.syndic.controller;

import lu.easy.syndic.dto.FileMeta;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.IncidentPhotoRepository;
import lu.easy.syndic.repository.IncidentRepository;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Photos d'un signalement : visibles par l'auteur et par le syndic. */
@RestController
@RequestMapping("/api/incidents")
@Transactional
public class IncidentPhotoController {

    private static final int MAX_PHOTOS = 5;
    private static final long MAX_BYTES = 6L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");

    private final IncidentRepository incidents;
    private final IncidentPhotoRepository photos;
    private final CurrentUser current;

    public IncidentPhotoController(IncidentRepository incidents, IncidentPhotoRepository photos, CurrentUser current) {
        this.incidents = incidents;
        this.photos = photos;
        this.current = current;
    }

    private Incident allowed(Long incidentId, Authentication a) {
        User u = current.get(a);
        Incident i = incidents.findById(incidentId).orElseThrow(() -> CurrentUser.notFound("Signalement"));
        if (u.role != Role.ADMIN && !u.id.equals(i.reporterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return i;
    }

    @GetMapping("/{id}/photos")
    @Transactional(readOnly = true)
    public List<FileMeta> list(@PathVariable Long id, Authentication a) {
        allowed(id, a);
        return photos.metas(id);
    }

    @PostMapping(value = "/{id}/photos", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public FileMeta upload(@PathVariable Long id, @RequestParam("file") MultipartFile file, Authentication a) throws IOException {
        allowed(id, a);
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!TYPES.contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les photos (JPEG, PNG, WebP, HEIC) sont acceptées");
        if (file.isEmpty() || file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo vide ou trop lourde (6 Mo maximum)");
        if (photos.countByIncidentId(id) >= MAX_PHOTOS) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "5 photos maximum par signalement");
        IncidentPhoto p = new IncidentPhoto();
        p.incidentId = id;
        p.fileName = cleanName(file.getOriginalFilename());
        p.contentType = type;
        p.data = file.getBytes();
        p = photos.save(p);
        return new FileMeta(p.id, p.fileName, p.contentType);
    }

    @GetMapping("/photos/{photoId}/raw")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> raw(@PathVariable Long photoId, Authentication a) {
        IncidentPhoto p = photos.findById(photoId).orElseThrow(() -> CurrentUser.notFound("Photo"));
        allowed(p.incidentId, a);
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(p.contentType))
                .cacheControl(CacheControl.noStore()).body(p.data);
    }

    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> delete(@PathVariable Long photoId, Authentication a) {
        IncidentPhoto p = photos.findById(photoId).orElseThrow(() -> CurrentUser.notFound("Photo"));
        allowed(p.incidentId, a);
        photos.delete(p);
        return ResponseEntity.noContent().build();
    }

    static String cleanName(String n) {
        if (n == null || n.isBlank()) return "photo";
        String s = n.replaceAll("[\\\\/\\r\\n]", "_");
        return s.length() > 150 ? s.substring(s.length() - 150) : s;
    }
}
