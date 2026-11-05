package lu.easy.syndic.controller;

import jakarta.validation.Valid;
import lu.easy.syndic.dto.Dtos.*;
import lu.easy.syndic.entity.ChargeCall;
import lu.easy.syndic.entity.ChargeStatus;
import lu.easy.syndic.repository.ChargeCallRepository;
import lu.easy.syndic.service.ChargeService;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChargeController {

    private final ChargeService service;
    private final ChargeCallRepository calls;
    private final CurrentUser current;

    public ChargeController(ChargeService service, ChargeCallRepository calls, CurrentUser current) {
        this.service = service;
        this.calls = calls;
        this.current = current;
    }

    // ----- Syndic -----
    @GetMapping("/admin/charge-calls")
    public List<ChargeCall> calls(@RequestParam(required = false) Long buildingId) {
        return calls.findAll(Sort.by(Sort.Direction.DESC, "dueDate")).stream()
                .filter(c -> buildingId == null || c.buildingId == null || buildingId.equals(c.buildingId)).toList();
    }

    @PostMapping("/admin/charge-calls")
    @ResponseStatus(HttpStatus.CREATED)
    public ChargeCall create(@Valid @RequestBody ChargeCallRequest r) {
        return service.createCall(r);
    }

    @DeleteMapping("/admin/charge-calls/{id}")
    public ResponseEntity<Void> deleteCall(@PathVariable Long id) {
        service.deleteCall(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/charges")
    public List<ChargeView> charges(@RequestParam(required = false) Long callId,
                                    @RequestParam(required = false) ChargeStatus status,
                                    @RequestParam(required = false) Long buildingId) {
        return service.adminList(callId, status, buildingId);
    }

    @PatchMapping("/admin/charges/{id}/paid")
    public ChargeView paid(@PathVariable Long id, @RequestParam boolean paid) {
        return service.setPaid(id, paid);
    }

    @PostMapping("/admin/charges/{id}/remind")
    public ChargeView remind(@PathVariable Long id) {
        return service.remind(id);
    }

    @PostMapping("/admin/charge-calls/{id}/remind")
    public Map<String, Integer> remindCall(@PathVariable Long id) {
        return Map.of("sent", service.remindCall(id));
    }

    @GetMapping(value = "/admin/charges/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> export(@RequestParam(required = false) Long callId,
                                         @RequestParam(required = false) ChargeStatus status,
                                         @RequestParam(required = false) Long buildingId) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"charges.csv\"")
                .body(service.exportCsv(callId, status, buildingId));
    }

    // ----- Copropriétaire -----
    @GetMapping("/charges")
    public List<ChargeView> mine(Authentication a) {
        return service.forOwner(current.get(a).id);
    }
}
