package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "reservations")
public class Reservation extends AgencyScoped {
    @Column(nullable = false, length = 100) public String space;
    @Column(nullable = false) public LocalDate date;
    @Column(nullable = false) public LocalTime startTime;
    @Column(nullable = false) public LocalTime endTime;
    public Long ownerId;
    public String ownerName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public ReservationStatus status = ReservationStatus.PENDING;
    @Column(length = 500) public String note;
    @Column(updatable = false) public LocalDateTime createdAt = LocalDateTime.now();
}
