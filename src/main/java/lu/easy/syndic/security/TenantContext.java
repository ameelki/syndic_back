package lu.easy.syndic.security;

/** Agence de la requête en cours (null = contexte système ou super administrateur). */
public final class TenantContext {
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();
    private TenantContext() {}
    public static Long get() { return CURRENT.get(); }
    public static void set(Long agencyId) { CURRENT.set(agencyId); }
    public static void clear() { CURRENT.remove(); }
}
