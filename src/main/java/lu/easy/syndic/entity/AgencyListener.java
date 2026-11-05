package lu.easy.syndic.entity;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import lu.easy.syndic.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rattache toute nouvelle donnée à l'agence de l'utilisateur connecté et refuse de charger
 * (même par identifiant) une donnée d'une autre agence. Sans agence courante (contexte système,
 * super administrateur), aucune restriction.
 */
public class AgencyListener {

    @PrePersist
    public void onCreate(Object entity) {
        Long tenant = TenantContext.get();
        if (tenant != null && entity instanceof AgencyScoped a) a.agencyId = tenant;
    }

    @PostLoad
    public void onLoad(Object entity) {
        Long tenant = TenantContext.get();
        if (tenant != null && entity instanceof AgencyScoped a && !tenant.equals(a.agencyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressource introuvable");
        }
    }
}
