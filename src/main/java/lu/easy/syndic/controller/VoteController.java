package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import lu.easy.syndic.dto.Dtos.*;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.*;
import lu.easy.syndic.service.CurrentUser;
import lu.easy.syndic.service.VotingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/votes")
@Transactional
public class VoteController {

    private final VoteRepository votes;
    private final ResolutionRepository resolutions;
    private final AssemblyRepository assemblies;
    private final ProxyGrantRepository proxies;
    private final UserRepository users;
    private final VotingService voting;
    private final CurrentUser current;

    public VoteController(VoteRepository votes, ResolutionRepository resolutions, AssemblyRepository assemblies,
                          ProxyGrantRepository proxies, UserRepository users, VotingService voting, CurrentUser current) {
        this.votes = votes;
        this.resolutions = resolutions;
        this.assemblies = assemblies;
        this.proxies = proxies;
        this.users = users;
        this.voting = voting;
        this.current = current;
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    private User owner(Authentication a) {
        User u = current.get(a);
        if (u.role != Role.OWNER) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seuls les copropriétaires votent");
        return u;
    }

    private Assembly assembly(Long id) {
        return assemblies.findById(id).orElseThrow(() -> CurrentUser.notFound("Assemblée"));
    }

    private boolean hasVoted(Long assemblyId, Long ownerId) {
        return resolutions.findAllByAssemblyId(assemblyId).stream()
                .anyMatch(r -> votes.findByResolutionIdAndOwnerId(r.id, ownerId).isPresent());
    }

    /** Un copropriétaire vote (ou modifie son vote) tant que le vote est ouvert ; poids = tantièmes + procurations reçues. */
    @PostMapping("/{resolutionId}")
    public MessageResponse vote(@PathVariable Long resolutionId, @Valid @RequestBody VoteRequest r, Authentication a) {
        User u = owner(a);
        Resolution res = resolutions.findById(resolutionId).orElseThrow(() -> CurrentUser.notFound("Résolution"));
        Assembly asm = assembly(res.assemblyId);
        if (!voting.votingOpen(asm)) throw bad("Le vote n'est pas ouvert pour cette assemblée (ou l'échéance est dépassée)");
        int weight = voting.weightOf(u.id, asm);
        if (weight <= 0) {
            throw bad("Aucun de vos lots n'est concerné par cette assemblée, ou vous avez donné procuration");
        }
        Vote v = votes.findByResolutionIdAndOwnerId(resolutionId, u.id).orElseGet(() -> {
            Vote n = new Vote();
            n.resolutionId = resolutionId;
            n.ownerId = u.id;
            return n;
        });
        v.choice = r.choice();
        v.weight = weight;
        votes.save(v);
        return new MessageResponse("Vote enregistré");
    }

    // ----- Procurations -----

    /** Copropriétaires à qui je peux donner procuration pour cette assemblée. */
    @GetMapping("/assemblies/{aid}/candidates")
    @Transactional(readOnly = true)
    public List<PersonWeight> candidates(@PathVariable Long aid, Authentication a) {
        User me = owner(a);
        Assembly asm = assembly(aid);
        Map<Long, Integer> own = voting.ownWeights(asm);
        List<Long> gave = proxies.findAllByAssemblyId(aid).stream().map(g -> g.fromOwnerId).toList();
        return users.findAllByRoleOrderByLastNameAsc(Role.OWNER).stream()
                .filter(x -> x.status == UserStatus.ACTIVE && !x.id.equals(me.id))
                .filter(x -> own.getOrDefault(x.id, 0) > 0 && !gave.contains(x.id))
                .map(x -> new PersonWeight(x.id, VotingService.fullName(x), own.get(x.id))).toList();
    }

    @PutMapping("/assemblies/{aid}/proxy")
    public MessageResponse giveProxy(@PathVariable Long aid, @Valid @RequestBody ProxyRequest r, Authentication a) {
        User me = owner(a);
        Assembly asm = assembly(aid);
        if (asm.status == AssemblyStatus.CLOSED) throw bad("L'assemblée est clôturée");
        Map<Long, Integer> own = voting.ownWeights(asm);
        if (own.getOrDefault(me.id, 0) <= 0) throw bad("Vous n'avez aucun lot concerné par cette assemblée");
        if (r.toOwnerId().equals(me.id)) throw bad("Vous ne pouvez pas vous donner procuration à vous-même");
        User to = users.findById(r.toOwnerId()).orElseThrow(() -> CurrentUser.notFound("Copropriétaire"));
        if (to.role != Role.OWNER || to.status != UserStatus.ACTIVE || own.getOrDefault(to.id, 0) <= 0) {
            throw bad("Ce copropriétaire ne peut pas recevoir de procuration pour cette assemblée");
        }
        if (!proxies.findAllByAssemblyIdAndToOwnerId(aid, me.id).isEmpty()) {
            throw bad("Vous détenez déjà des procurations : vous ne pouvez pas en donner");
        }
        if (proxies.findByAssemblyIdAndFromOwnerId(aid, to.id).isPresent()) {
            throw bad("Ce copropriétaire a lui-même donné procuration");
        }
        if (hasVoted(aid, me.id)) throw bad("Vous avez déjà voté : la procuration n'est plus possible");
        ProxyGrant existing = proxies.findByAssemblyIdAndFromOwnerId(aid, me.id).orElse(null);
        long already = proxies.findAllByAssemblyIdAndToOwnerId(aid, to.id).stream()
                .filter(g -> existing == null || !g.id.equals(existing.id)).count();
        if (already >= VotingService.MAX_PROXIES) {
            throw bad("Ce copropriétaire détient déjà le maximum de procurations (" + VotingService.MAX_PROXIES + ")");
        }
        ProxyGrant g = existing != null ? existing : new ProxyGrant();
        g.assemblyId = aid;
        g.fromOwnerId = me.id;
        g.toOwnerId = to.id;
        proxies.save(g);
        return new MessageResponse("Procuration enregistrée");
    }

    @DeleteMapping("/assemblies/{aid}/proxy")
    public MessageResponse revokeProxy(@PathVariable Long aid, Authentication a) {
        User me = owner(a);
        Assembly asm = assembly(aid);
        if (asm.status == AssemblyStatus.CLOSED) throw bad("L'assemblée est clôturée");
        proxies.findByAssemblyIdAndFromOwnerId(aid, me.id).ifPresent(proxies::delete);
        return new MessageResponse("Procuration retirée");
    }
}
