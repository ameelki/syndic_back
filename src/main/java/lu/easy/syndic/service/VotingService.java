package lu.easy.syndic.service;

import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Règles de vote : poids (tantièmes + procurations), majorités, quorum, échéance. */
@Service
public class VotingService {

    /** Nombre maximal de procurations qu'un copropriétaire peut recevoir. */
    public static final int MAX_PROXIES = 3;

    private final LotRepository lots;
    private final ProxyGrantRepository proxies;

    public VotingService(LotRepository lots, ProxyGrantRepository proxies) {
        this.lots = lots;
        this.proxies = proxies;
    }

    /** Lots qui votent à cette assemblée : lots avec propriétaire, du bâtiment concerné (ou tous). */
    public List<Lot> scopeLots(Assembly a) {
        return lots.findAll().stream()
                .filter(l -> l.ownerId != null && (a.buildingId == null || a.buildingId.equals(l.buildingId))).toList();
    }

    public int totalWeight(Assembly a) {
        return scopeLots(a).stream().mapToInt(l -> l.tantiemes).sum();
    }

    /** Tantièmes propres de chaque copropriétaire. */
    public Map<Long, Integer> ownWeights(Assembly a) {
        Map<Long, Integer> own = new HashMap<>();
        for (Lot l : scopeLots(a)) own.merge(l.ownerId, l.tantiemes, Integer::sum);
        return own;
    }

    /** Poids de vote effectif : tantièmes propres + procurations reçues ; 0 si on a donné procuration. */
    public Map<Long, Integer> weights(Assembly a) {
        Map<Long, Integer> own = ownWeights(a);
        Map<Long, Integer> w = new HashMap<>(own);
        for (ProxyGrant g : proxies.findAllByAssemblyId(a.id)) {
            int moved = own.getOrDefault(g.fromOwnerId, 0);
            w.put(g.fromOwnerId, 0);
            w.merge(g.toOwnerId, moved, Integer::sum);
        }
        return w;
    }

    public int weightOf(Long ownerId, Assembly a) {
        return weights(a).getOrDefault(ownerId, 0);
    }

    public boolean votingOpen(Assembly a) {
        return a.status == AssemblyStatus.OPEN && (a.votingDeadline == null || LocalDateTime.now().isBefore(a.votingDeadline));
    }

    public boolean quorumReached(Assembly a, int votedWeight, int totalWeight) {
        int q = a.quorumPercent == null ? 0 : a.quorumPercent;
        return q <= 0 || votedWeight * 100L >= (long) q * totalWeight;
    }

    /** PENDING tant que l'assemblée n'est pas clôturée, puis ADOPTED / REJECTED / NO_QUORUM. */
    public String result(Assembly a, MajorityType m, int f, int ag, int total, boolean quorum) {
        if (a.status != AssemblyStatus.CLOSED) return "PENDING";
        if (!quorum) return "NO_QUORUM";
        MajorityType rule = m == null ? MajorityType.SIMPLE : m;
        boolean ok = switch (rule) {
            case SIMPLE -> f > ag;
            case ABSOLUTE -> total > 0 && f * 2L > total;
            case TWO_THIRDS -> (f + ag) > 0 && f * 3L >= 2L * (f + ag);
            case UNANIMITY -> total > 0 && f == total;
        };
        return ok ? "ADOPTED" : "REJECTED";
    }

    public static String fullName(User u) {
        return u == null ? "?" : u.firstName + " " + u.lastName;
    }

    public static Set<Long> ids(Collection<Long> c) {
        return c.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
