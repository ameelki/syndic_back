package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.DocumentFileRepository;
import lu.easy.syndic.repository.DocumentItemRepository;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

/**
 * Coffre de documents : lien externe ou fichier téléversé (PDF, images, Office, 10 Mo max).
 * Le syndic peut réserver un document au syndic seulement ; les copropriétaires ne voient que les documents
 * « visibles » de leurs bâtiments (ou généraux).
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentItemController extends CrudController<DocumentItem> {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("application/pdf", "image/jpeg", "image/png", "image/webp", "text/plain",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final DocumentFileRepository files;
    private final DocumentItemRepository docs;

    public DocumentItemController(DocumentItemRepository repo, DocumentFileRepository files) {
        super(repo, "Document");
        this.files = files;
        this.docs = repo;
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    @Override
    @GetMapping
    public List<DocumentItem> list(@RequestParam(required = false) Long buildingId, Authentication auth) {
        List<DocumentItem> all = super.list(buildingId, auth);
        if (currentUser.get(auth).role == Role.ADMIN) return all;
        return all.stream().filter(d -> d.ownersVisible == null || d.ownersVisible).toList();
    }

    /** Le formulaire ne connaît pas le fichier : on conserve ses informations. */
    @Override
    @PutMapping("/{id}")
    public DocumentItem update(@PathVariable Long id, @Valid @RequestBody DocumentItem body) {
        DocumentItem existing = repo.findById(id).orElseThrow(() -> CurrentUser.notFound("Document"));
        body.fileName = existing.fileName;
        body.fileType = existing.fileType;
        body.fileSize = existing.fileSize;
        if (body.ownersVisible == null) body.ownersVisible = true;
        return super.update(id, body);
    }

    @Override
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        files.deleteAllByDocumentId(id);
        return super.delete(id);
    }

    /** Téléversement (syndic) : remplace le fichier existant. */
    @PostMapping(value = "/{id}/file", consumes = "multipart/form-data")
    @Transactional
    public DocumentItem upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        DocumentItem d = docs.findById(id).orElseThrow(() -> CurrentUser.notFound("Document"));
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!TYPES.contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format non accepté (PDF, images, Word, Excel, texte)");
        if (file.isEmpty() || file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide ou trop lourd (10 Mo maximum)");
        DocumentFile f = files.findByDocumentId(id).orElseGet(() -> {
            DocumentFile n = new DocumentFile();
            n.documentId = id;
            return n;
        });
        f.data = file.getBytes();
        files.save(f);
        d.fileName = IncidentPhotoController.cleanName(file.getOriginalFilename());
        d.fileType = type;
        d.fileSize = file.getSize();
        activity.log("Fichier téléversé", d.title);
        return docs.save(d);
    }

    @GetMapping("/{id}/file")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> download(@PathVariable Long id, Authentication auth) {
        // list() applique déjà la règle bâtiment + visibilité : le document doit y figurer
        DocumentItem d = list(null, auth).stream().filter(x -> x.id.equals(id)).findFirst()
                .orElseThrow(() -> CurrentUser.notFound("Document"));
        DocumentFile f = files.findByDocumentId(id).orElseThrow(() -> CurrentUser.notFound("Fichier"));
        String name = URLEncoder.encode(d.fileName == null ? "document" : d.fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header("Content-Type", d.fileType == null ? "application/octet-stream" : d.fileType)
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + name)
                .body(f.data);
    }

    @DeleteMapping("/{id}/file")
    @Transactional
    public DocumentItem removeFile(@PathVariable Long id) {
        DocumentItem d = docs.findById(id).orElseThrow(() -> CurrentUser.notFound("Document"));
        files.deleteAllByDocumentId(id);
        d.fileName = null;
        d.fileType = null;
        d.fileSize = null;
        return docs.save(d);
    }
}
