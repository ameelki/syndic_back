package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import lu.easy.syndic.entity.BaseEntity;
import lu.easy.syndic.entity.BuildingScoped;
import lu.easy.syndic.entity.Lot;
import lu.easy.syndic.entity.Role;
import lu.easy.syndic.entity.User;
import lu.easy.syndic.repository.LotRepository;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** CRUD générique ; les droits (lecture / écriture) sont définis dans SecurityConfig. */
public abstract class CrudController<T extends BaseEntity> {

    protected final JpaRepository<T, Long> repo;
    private final String label;

    protected CrudController(JpaRepository<T, Long> repo, String label) {
        this.repo = repo;
        this.label = label;
    }

    protected Sort sort() {
        return Sort.by(Sort.Direction.DESC, "id");
    }

    @Autowired
    protected CurrentUser currentUser;
    @Autowired
    protected LotRepository lotRepository;

    /**
     * Liste, filtrée par bâtiment : un élément sans bâtiment est « général » et reste visible partout.
     * Syndic : filtre optionnel ?buildingId=. Copropriétaire : uniquement les bâtiments où il possède un lot.
     */
    @GetMapping
    public List<T> list(@RequestParam(required = false) Long buildingId, Authentication auth) {
        List<T> all = repo.findAll(sort());
        if (all.isEmpty() || !(all.get(0) instanceof BuildingScoped)) return all;
        User u = currentUser.get(auth);
        Set<Long> owned = null;
        if (u.role == Role.OWNER) {
            owned = lotRepository.findAllByOwnerId(u.id).stream().map(l -> l.buildingId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
        }
        final Set<Long> visible = owned;
        return all.stream().filter(t -> {
            Long b = ((BuildingScoped) t).buildingId;
            if (b == null) return true;
            if (visible != null && !visible.contains(b)) return false;
            return buildingId == null || buildingId.equals(b);
        }).toList();
    }

    @GetMapping("/{id}")
    public T get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> CurrentUser.notFound(label));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public T create(@Valid @RequestBody T body) {
        body.id = null;
        return repo.save(body);
    }

    @PutMapping("/{id}")
    public T update(@PathVariable Long id, @Valid @RequestBody T body) {
        if (!repo.existsById(id)) throw CurrentUser.notFound(label);
        body.id = id;
        return repo.save(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) throw CurrentUser.notFound(label);
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
