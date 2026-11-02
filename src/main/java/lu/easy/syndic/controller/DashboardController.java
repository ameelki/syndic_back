package lu.easy.syndic.controller;

import lu.easy.syndic.dto.Dtos.*;
import lu.easy.syndic.entity.*;
import lu.easy.syndic.repository.*;
import lu.easy.syndic.service.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class DashboardController {

    private final UserRepository users;
    private final LotRepository lots;
    private final ChargeRepository charges;
    private final IncidentRepository incidents;
    private final ExpenseRepository expenses;
    private final AssemblyRepository assemblies;
    private final AnnouncementRepository announcements;
    private final BudgetRepository budgets;
    private final CurrentUser current;

    public DashboardController(UserRepository users, LotRepository lots, ChargeRepository charges,
                               IncidentRepository incidents, ExpenseRepository expenses, AssemblyRepository assemblies,
                               AnnouncementRepository announcements, BudgetRepository budgets, CurrentUser current) {
        this.budgets = budgets;
        this.users = users;
        this.lots = lots;
        this.charges = charges;
        this.incidents = incidents;
        this.expenses = expenses;
        this.assemblies = assemblies;
        this.announcements = announcements;
        this.current = current;
    }

    /** Visible pour ce bâtiment ? (élément général ou du bâtiment) */
    private static boolean visible(Long itemBuilding, Long buildingId, Set<Long> owned) {
        if (itemBuilding == null) return true;
        if (owned != null && !owned.contains(itemBuilding)) return false;
        return buildingId == null || buildingId.equals(itemBuilding);
    }

    private Assembly nextAssembly(Long buildingId, Set<Long> owned) {
        LocalDateTime now = LocalDateTime.now();
        return assemblies.findAll(Sort.by("meetingDate")).stream()
                .filter(a -> a.status != AssemblyStatus.CLOSED && a.meetingDate.isAfter(now.minusDays(1)))
                .filter(a -> visible(a.buildingId, buildingId, owned))
                .findFirst().orElse(null);
    }

    private List<Announcement> latestAnnouncements(Long buildingId, Set<Long> owned) {
        return announcements.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(a -> visible(a.buildingId, buildingId, owned)).limit(3).toList();
    }

    @GetMapping("/admin/dashboard")
    public AdminDashboard admin(@RequestParam(required = false) Long buildingId) {
        List<Lot> all = lots.findAll().stream().filter(l -> buildingId == null || buildingId.equals(l.buildingId)).toList();
        Set<Long> lotIds = all.stream().map(l -> l.id).collect(Collectors.toSet());
        List<Charge> unpaid = charges.findAllByStatus(ChargeStatus.UNPAID).stream()
                .filter(c -> buildingId == null || lotIds.contains(c.lotId)).toList();
        int year = LocalDate.now().getYear();
        List<Expense> exps = expenses.findAll(Sort.by(Sort.Direction.DESC, "expenseDate")).stream()
                .filter(e -> buildingId == null || buildingId.equals(e.buildingId)).toList();
        BigDecimal exp = exps.stream().filter(e -> e.expenseDate.getYear() == year)
                .map(e -> e.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Budget> bs = budgets.findAllByYear(year).stream()
                .filter(b -> buildingId == null || buildingId.equals(b.buildingId)).toList();
        BigDecimal budget = bs.isEmpty() ? null : bs.stream().map(b -> b.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeOwners = buildingId == null
                ? users.findAllByRoleOrderByLastNameAsc(Role.OWNER).stream().filter(u -> u.status == UserStatus.ACTIVE).count()
                : all.stream().map(l -> l.ownerId).filter(Objects::nonNull).distinct().count();
        List<Incident> open = incidents.findAll().stream().filter(i -> i.status != IncidentStatus.RESOLVED)
                .filter(i -> buildingId == null || i.buildingId == null || buildingId.equals(i.buildingId)).toList();
        return new AdminDashboard(users.countByStatus(UserStatus.PENDING), activeOwners,
                all.size(), all.stream().mapToLong(l -> l.tantiemes).sum(),
                unpaid.stream().map(c -> c.amount).reduce(BigDecimal.ZERO, BigDecimal::add), unpaid.size(),
                open.size(), exp, budget,
                open.stream().filter(i -> i.priority == Priority.HIGH).count(),
                exps.stream().limit(5).toList(),
                nextAssembly(buildingId, null), latestAnnouncements(buildingId, null));
    }

    @GetMapping("/dashboard")
    public OwnerDashboard owner(Authentication a) {
        User u = current.get(a);
        List<Lot> mine = lots.findAllByOwnerId(u.id);
        Set<Long> owned = mine.stream().map(l -> l.buildingId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Charge> unpaid = charges.findAllByOwnerIdOrderByIdDesc(u.id).stream()
                .filter(c -> c.status == ChargeStatus.UNPAID).toList();
        return new OwnerDashboard(u.firstName, unpaid.stream().map(c -> c.amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                unpaid.size(), incidents.countByReporterIdAndStatusNot(u.id, IncidentStatus.RESOLVED),
                mine.stream().mapToInt(l -> l.tantiemes).sum(), mine, nextAssembly(null, owned), latestAnnouncements(null, owned));
    }

    @GetMapping("/my/lots")
    public List<Lot> myLots(Authentication a) {
        return lots.findAllByOwnerId(current.get(a).id);
    }
}
