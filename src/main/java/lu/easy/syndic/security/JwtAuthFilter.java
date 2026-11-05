package lu.easy.syndic.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lu.easy.syndic.entity.Role;
import lu.easy.syndic.entity.User;
import lu.easy.syndic.entity.UserStatus;
import lu.easy.syndic.repository.AgencyRepository;
import lu.easy.syndic.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Vérifie le jeton ET recharge l'utilisateur : un compte rejeté perd l'accès immédiatement.
 * Fixe aussi l'agence de la requête ; un compte sans agence active (hors super administrateur) n'est pas authentifié.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserRepository users;
    private final AgencyRepository agencies;

    public JwtAuthFilter(JwtService jwt, UserRepository users, AgencyRepository agencies) {
        this.jwt = jwt;
        this.users = users;
        this.agencies = agencies;
    }

    private boolean allowed(User u) {
        if (u.status != UserStatus.ACTIVE) return false;
        if (u.role == Role.SUPER_ADMIN) return true;
        return u.agencyId != null && agencies.findById(u.agencyId).map(a -> a.active).orElse(false);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String email = jwt.extractEmail(header.substring(7));
                if (email != null) {
                    users.findByEmail(email).filter(this::allowed).ifPresent(u -> {
                        var auth = new UsernamePasswordAuthenticationToken(
                                u.email, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.role.name())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        if (u.role != Role.SUPER_ADMIN) TenantContext.set(u.agencyId);
                    });
                }
            }
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}
