package lu.easy.syndic.service;

import lu.easy.syndic.dto.Dtos.AgencyPublic;
import lu.easy.syndic.entity.Agency;
import lu.easy.syndic.repository.AgencyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
public class AgencyService {

    /** Identifiants réservés (sous-domaines techniques). */
    public static final Set<String> RESERVED = Set.of("www", "api", "app", "admin", "super", "static", "assets");

    private final AgencyRepository agencies;
    private final String defaultSlug;

    public AgencyService(AgencyRepository agencies, @Value("${app.agency.default-slug:default}") String defaultSlug) {
        this.agencies = agencies;
        this.defaultSlug = defaultSlug;
    }

    /** Agence active correspondant au slug ; sans slug, l'agence par défaut. */
    public Agency resolveActive(String slug) {
        String s = (slug == null || slug.isBlank()) ? defaultSlug : slug.trim().toLowerCase();
        return agencies.findBySlug(s).filter(a -> a.active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agence introuvable"));
    }

    /** Agence exigée (inscription) : pas d'agence par défaut, le lien doit désigner une agence active. */
    public Agency resolveRequired(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lien d'inscription invalide : utilisez le lien fourni par votre syndic.");
        }
        return agencies.findBySlug(slug.trim().toLowerCase()).filter(a -> a.active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Agence introuvable ou désactivée : vérifiez le lien fourni par votre syndic."));
    }

    public static AgencyPublic toPublic(Agency a) {
        return new AgencyPublic(a.id, a.slug, a.name, a.tagline, a.logoUrl, a.primaryColor, a.accentColor);
    }
}
