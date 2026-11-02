package lu.easy.syndic.service;

import lu.easy.syndic.dto.Dtos.*;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChargeService {

    private final ChargeCallRepository calls;
    private final ChargeRepository charges;
    private final LotRepository lots;
    private final UserRepository users;

    public ChargeService(ChargeCallRepository calls, ChargeRepository charges, LotRepository lots, UserRepository users) {
        this.calls = calls;
        this.charges = charges;
        this.lots = lots;
        this.users = users;
    }

    /** Répartit le montant total entre tous les lots selon leurs tantièmes. */
    public ChargeCall createCall(ChargeCallRequest r) {
        Long bid = r.buildingId();
        List<Lot> all = lots.findAll().stream().filter(l -> bid == null || bid.equals(l.buildingId)).toList();
        int totalShares = all.stream().mapToInt(l -> l.tantiemes).sum();
        if (totalShares <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun lot enregistré");
        if (all.stream().noneMatch(l -> l.ownerId != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun lot n'est rattaché à un copropriétaire");
        }
        ChargeCall c = new ChargeCall();
        c.title = r.title().trim();
        c.totalAmount = r.totalAmount();
        c.dueDate = r.dueDate();
        c.buildingId = bid;
        c = calls.save(c);
        for (Lot l : all) {
            if (l.ownerId == null) continue;
            Charge ch = new Charge();
            ch.callId = c.id;
            ch.lotId = l.id;
            ch.ownerId = l.ownerId;
            ch.amount = r.totalAmount().multiply(BigDecimal.valueOf(l.tantiemes))
                    .divide(BigDecimal.valueOf(totalShares), 2, RoundingMode.HALF_UP);
            charges.save(ch);
        }
        return c;
    }

    public void deleteCall(Long id) {
        if (!calls.existsById(id)) throw CurrentUser.notFound("Appel de fonds");
        charges.deleteAllByCallId(id);
        calls.deleteById(id);
    }

    public ChargeView setPaid(Long id, boolean paid) {
        Charge c = charges.findById(id).orElseThrow(() -> CurrentUser.notFound("Charge"));
        c.status = paid ? ChargeStatus.PAID : ChargeStatus.UNPAID;
        c.paidAt = paid ? LocalDate.now() : null;
        charges.save(c);
        return views(List.of(c)).get(0);
    }

    @Transactional(readOnly = true)
    public List<ChargeView> adminList(Long callId, ChargeStatus status, Long buildingId) {
        List<Charge> list = callId != null ? charges.findAllByCallId(callId)
                : status != null ? charges.findAllByStatus(status) : charges.findAll();
        Set<Long> lotIds = buildingId == null ? null : lots.findAll().stream()
                .filter(l -> buildingId.equals(l.buildingId)).map(l -> l.id).collect(Collectors.toSet());
        return views(list.stream().filter(c -> status == null || c.status == status)
                .filter(c -> lotIds == null || lotIds.contains(c.lotId))
                .sorted(Comparator.comparing((Charge c) -> c.id).reversed()).toList());
    }

    @Transactional(readOnly = true)
    public List<ChargeView> forOwner(Long ownerId) {
        return views(charges.findAllByOwnerIdOrderByIdDesc(ownerId));
    }

    private List<ChargeView> views(List<Charge> list) {
        Map<Long, ChargeCall> callMap = calls.findAllById(list.stream().map(c -> c.callId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(c -> c.id, c -> c));
        Map<Long, Lot> lotMap = lots.findAllById(list.stream().map(c -> c.lotId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(l -> l.id, l -> l));
        Map<Long, User> userMap = users.findAllById(list.stream().map(c -> c.ownerId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(u -> u.id, u -> u));
        return list.stream().map(c -> {
            ChargeCall call = callMap.get(c.callId);
            Lot lot = lotMap.get(c.lotId);
            User owner = c.ownerId == null ? null : userMap.get(c.ownerId);
            return new ChargeView(c.id, c.callId, call == null ? "" : call.title, call == null ? null : call.dueDate,
                    c.lotId, lot == null ? "" : lot.number, c.ownerId,
                    owner == null ? "" : owner.firstName + " " + owner.lastName, c.amount, c.status, c.paidAt);
        }).toList();
    }
}
