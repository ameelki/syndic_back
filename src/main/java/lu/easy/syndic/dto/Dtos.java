package lu.easy.syndic.dto;

import jakarta.validation.constraints.*;
import lu.easy.syndic.entity.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {}

    // ---- Auth ----
    public record RegisterRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 40) String phone,
            @Size(max = 120) String requestedLot,
            @NotNull @AssertTrue(message = "Les conditions doivent être acceptées") Boolean termsAccepted) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record UserDto(Long id, String firstName, String lastName, String email, String phone, String requestedLot,
                          Role role, UserStatus status, LocalDateTime createdAt, LocalDateTime approvedAt) {}
    public record AuthResponse(String token, UserDto user) {}
    public record MessageResponse(String message) {}
    public record ErrorResponse(String message, Map<String, String> fields) {}

    // ---- Charges ----
    public record ChargeCallRequest(@NotBlank @Size(max = 150) String title,
                                    @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
                                    @NotNull LocalDate dueDate, Long buildingId) {}
    public record ChargeView(Long id, Long callId, String callTitle, LocalDate dueDate, Long lotId, String lotNumber,
                             Long ownerId, String ownerName, BigDecimal amount, ChargeStatus status, LocalDate paidAt) {}

    // ---- Incidents ----
    public record IncidentCreate(@NotBlank @Size(max = 150) String title, @Size(max = 2000) String description,
                                 @Size(max = 150) String location, Priority priority, Long buildingId) {}
    public record IncidentUpdate(@NotNull IncidentStatus status, @NotNull Priority priority,
                                 @Size(max = 1000) String adminComment) {}

    // ---- Assemblées ----
    public record ResolutionCreate(@NotBlank @Size(max = 200) String title, @Size(max = 2000) String description,
                                   MajorityType majority) {}
    public record VoterView(Long ownerId, String name, int weight, VoteChoice choice) {}
    public record ResolutionView(Long id, String title, String description, MajorityType majority, int forWeight,
                                 int againstWeight, int abstainWeight, int votersCount, VoteChoice myVote,
                                 String result, List<VoterView> voters) {}
    public record PersonWeight(Long id, String name, int weight) {}
    public record ProxyInfo(PersonWeight givenTo, List<PersonWeight> received, int maxReceived) {}
    public record AssemblyView(Assembly assembly, List<ResolutionView> resolutions, int myWeight, int ownWeight,
                               boolean canVote, boolean votingOpen, int totalWeight, int votedWeight,
                               boolean quorumReached, ProxyInfo proxy) {}
    public record VoteRequest(@NotNull VoteChoice choice) {}
    public record ProxyRequest(@NotNull Long toOwnerId) {}

    // ---- Bâtiments ----
    public record BuildingSummary(Long id, String name, String address, String city, long lots, long totalTantiemes,
                                  long owners, BigDecimal unpaidTotal, long openIncidents) {}

    // ---- Tableaux de bord ----
    public record AdminDashboard(long pendingUsers, long activeOwners, long lots, long totalTantiemes,
                                 BigDecimal unpaidTotal, long unpaidCount, long openIncidents,
                                 BigDecimal expensesThisYear, BigDecimal budgetThisYear, long highPriorityIncidents,
                                 List<Expense> recentExpenses, Assembly nextAssembly, List<Announcement> announcements) {}
    public record OwnerDashboard(String firstName, BigDecimal dueTotal, long unpaidCount, long openIncidents,
                                 int myTantiemes, List<Lot> lots, Assembly nextAssembly, List<Announcement> announcements) {}
}
