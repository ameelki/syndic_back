package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Filter(name = "agencyFilter", condition = "agency_id = :agencyId")
@Table(name = "app_users")
public class User extends AgencyScoped {
    @Column(nullable = false, length = 100) public String firstName;
    @Column(nullable = false, length = 100) public String lastName;
    @Column(nullable = false, unique = true, length = 190) public String email;
    @Column(nullable = false) public String password;
    @Column(length = 40) public String phone;
    @Column(length = 120) public String requestedLot;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public Role role = Role.OWNER;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public UserStatus status = UserStatus.PENDING;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime approvedAt;
}
