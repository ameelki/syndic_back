package lu.easy.syndic.controller;

import lu.easy.syndic.entity.DocumentItem;
import lu.easy.syndic.repository.DocumentItemRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
public class DocumentItemController extends CrudController<DocumentItem> {

    public DocumentItemController(DocumentItemRepository repo) {
        super(repo, "Document");
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
