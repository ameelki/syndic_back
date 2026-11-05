package lu.easy.syndic.service;

import lu.easy.syndic.dto.AttendanceRow;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    private final VotingService voting;
    private final UserRepository users;
    private final ProxyGrantRepository proxies;
    private final AttendanceRepository attendance;

    public AttendanceService(VotingService voting, UserRepository users, ProxyGrantRepository proxies, AttendanceRepository attendance) {
        this.voting = voting;
        this.users = users;
        this.proxies = proxies;
        this.attendance = attendance;
    }

    /** Copropriétaires concernés par l'assemblée (propriétaires d'un lot du bâtiment, ou de tous si assemblée générale). */
    @Transactional(readOnly = true)
    public List<User> eligibleOwners(Assembly asm) {
        Set<Long> ids = voting.scopeLots(asm).stream().map(l -> l.ownerId).collect(Collectors.toSet());
        return users.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public List<AttendanceRow> rows(Assembly asm) {
        List<Lot> lots = voting.scopeLots(asm);
        Map<Long, User> owners = eligibleOwners(asm).stream().collect(Collectors.toMap(u -> u.id, u -> u));
        Map<Long, Integer> own = voting.ownWeights(asm);
        Map<Long, String> lotNames = new HashMap<>();
        for (Lot l : lots) lotNames.merge(l.ownerId, l.number, (a, b) -> a + ", " + b);
        Map<Long, Boolean> present = new HashMap<>();
        attendance.findAllByAssemblyId(asm.id).forEach(a -> present.put(a.ownerId, a.present));
        Map<Long, Long> to = new HashMap<>();
        Map<Long, Integer> received = new HashMap<>();
        for (ProxyGrant g : proxies.findAllByAssemblyId(asm.id)) {
            to.put(g.fromOwnerId, g.toOwnerId);
            received.merge(g.toOwnerId, 1, Integer::sum);
        }
        List<AttendanceRow> out = new ArrayList<>();
        for (User u : owners.values()) {
            Long holder = to.get(u.id);
            User h = holder == null ? null : owners.get(holder);
            out.add(new AttendanceRow(u.id, VotingService.fullName(u), lotNames.getOrDefault(u.id, ""), own.getOrDefault(u.id, 0),
                    present.getOrDefault(u.id, false), holder == null ? null : VotingService.fullName(h), received.getOrDefault(u.id, 0)));
        }
        out.sort(Comparator.comparing(AttendanceRow::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public void setPresent(Assembly asm, Long ownerId, boolean present) {
        Attendance a = attendance.findByAssemblyIdAndOwnerId(asm.id, ownerId).orElseGet(() -> {
            Attendance n = new Attendance();
            n.assemblyId = asm.id;
            n.ownerId = ownerId;
            return n;
        });
        a.present = present;
        attendance.save(a);
    }
}
