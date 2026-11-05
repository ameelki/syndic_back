package lu.easy.syndic.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lu.easy.syndic.security.TenantContext;
import org.hibernate.Session;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Active le filtre Hibernate « agencyFilter » sur la session de la requête (Open Session In View) :
 * toutes les requêtes JPA ne voient alors que les données de l'agence de l'utilisateur connecté.
 * L'intercepteur passe après celui d'Open Session In View grâce à son ordre.
 */
@Configuration
public class TenantWebConfig implements WebMvcConfigurer {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                Long tenant = TenantContext.get();
                if (tenant != null) {
                    em.unwrap(Session.class).enableFilter("agencyFilter").setParameter("agencyId", tenant);
                }
                return true;
            }
        }).order(Ordered.LOWEST_PRECEDENCE);
    }
}
