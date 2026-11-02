package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import lu.easy.syndic.dto.Dtos.*;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.*;
import lu.easy.syndic.service.CurrentUser;
import lu.easy.syndic.service.VotingService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/** Lecture : tous les comptes validés. Écriture (créer, modifier, résolutions) : syndic uniquement. */
@RestController
@RequestMapping("/api/assemblies")
public class AssemblyController extends CrudController<Assembly> {

    private final ResolutionRepository resolutions;
    private final VoteRepository votes;
    private final LotRepository lots;
    private final CurrentUser current;
    private final VotingService voting;
    private final ProxyGrantRepository proxyGrants;
    private final UserRepository users;

    public AssemblyController(AssemblyRepository repo, ResolutionRepository resolutions, VoteRepository votes,
                              LotRepository lots, CurrentUser current, VotingService voting,
                              ProxyGrantRepository proxyGrants, UserRepository users) {
        super(repo, "Assemblée");
        this.voting = voting;
        this.proxyGrants = proxyGrants;
        this.users = users;
        this.resolutions = resolutions;
        this.votes = votes;
        this.lots = lots;
        this.current = current;
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.DESC, "meetingDate");
    }

    @Override
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) throw CurrentUser.notFound("Assemblée");
        resolutions.findAllByAssemblyId(id).forEach(r -> votes.deleteAllByResolutionId(r.id));
        resolutions.deleteAllByAssemblyId(id);
        proxyGrants.deleteAllByAssemblyId(id);
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/view")
    @Transactional(readOnly = true)
    public AssemblyView view(@PathVariable Long id, Authentication a) {
        Assembly asm = get(id);
        User u = current.get(a);
        boolean admin = u.role == Role.ADMIN;
        Map<Long, Integer> weights = voting.weights(asm);
        Map<Long, Integer> own = voting.ownWeights(asm);
        int total = voting.totalWeight(asm);
        List<Resolution> rs = resolutions.findAllByAssemblyId(id);
        Map<Long, List<Vote>> byRes = new HashMap<>();
        Set<Long> voters = new HashSet<>();
        for (Resolution r : rs) {
            List<Vote> vs = votes.findAllByResolutionId(r.id);
            byRes.put(r.id, vs);
            vs.forEach(v -> voters.add(v.ownerId));
        }
        int votedWeight = voters.stream().mapToInt(o -> weights.getOrDefault(o, 0)).sum();
        boolean quorum = voting.quorumReached(asm, votedWeight, total);
        Map<Long, User> names = users.findAllById(voters).stream().collect(Collectors.toMap(x -> x.id, x -> x));
        List<ResolutionView> list = rs.stream().map(r -> {
            int f = 0, ag = 0, ab = 0;
            VoteChoice mine = null;
            List<VoterView> detail = new ArrayList<>();
            for (Vote v : byRes.get(r.id)) {
                int w = weights.getOrDefault(v.ownerId, 0);
                switch (v.choice) {
                    case FOR -> f += w;
                    case AGAINST -> ag += w;
                    case ABSTAIN -> ab += w;
                }
                if (v.ownerId.equals(u.id)) mine = v.choice;
                if (admin && w > 0) detail.add(new VoterView(v.ownerId, VotingService.fullName(names.get(v.ownerId)), w, v.choice));
            }
            detail.sort(Comparator.comparing(VoterView::name));
            return new ResolutionView(r.id, r.title, r.description, r.majority == null ? MajorityType.SIMPLE : r.majority,
                    f, ag, ab, (int) byRes.get(r.id).stream().filter(v -> weights.getOrDefault(v.ownerId, 0) > 0).count(), mine,
                    voting.result(asm, r.majority, f, ag, total, quorum), detail);
        }).toList();

        int myWeight = admin ? 0 : weights.getOrDefault(u.id, 0);
        boolean open = voting.votingOpen(asm);
        ProxyInfo proxy = null;
        if (!admin) {
            PersonWeight givenTo = null;
            Optional<ProxyGrant> g = proxyGrants.findByAssemblyIdAndFromOwnerId(id, u.id);
            if (g.isPresent()) {
                User to = users.findById(g.get().toOwnerId).orElse(null);
                givenTo = new PersonWeight(g.get().toOwnerId, VotingService.fullName(to), own.getOrDefault(u.id, 0));
            }
            List<PersonWeight> received = new ArrayList<>();
            for (ProxyGrant rg : proxyGrants.findAllByAssemblyIdAndToOwnerId(id, u.id)) {
                received.add(new PersonWeight(rg.fromOwnerId, VotingService.fullName(users.findById(rg.fromOwnerId).orElse(null)),
                        own.getOrDefault(rg.fromOwnerId, 0)));
            }
            proxy = new ProxyInfo(givenTo, received, VotingService.MAX_PROXIES);
        }
        return new AssemblyView(asm, list, myWeight, admin ? 0 : own.getOrDefault(u.id, 0), !admin && open && myWeight > 0,
                open, total, votedWeight, quorum, proxy);
    }

    @PostMapping("/{id}/resolutions")
    @ResponseStatus(HttpStatus.CREATED)
    public Resolution addResolution(@PathVariable Long id, @Valid @RequestBody ResolutionCreate r) {
        get(id);
        Resolution res = new Resolution();
        res.assemblyId = id;
        res.title = r.title().trim();
        res.description = r.description();
        res.majority = r.majority() == null ? MajorityType.SIMPLE : r.majority();
        return resolutions.save(res);
    }

    @DeleteMapping("/resolutions/{rid}")
    @Transactional
    public ResponseEntity<Void> deleteResolution(@PathVariable Long rid) {
        if (!resolutions.existsById(rid)) throw CurrentUser.notFound("Résolution");
        votes.deleteAllByResolutionId(rid);
        resolutions.deleteById(rid);
        return ResponseEntity.noContent().build();
    }
}
