package lu.easy.syndic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Lien à usage unique pour définir ou réinitialiser un mot de passe (seul l'empreinte SHA-256 est stockée). */
@Entity
@Table(name = "password_tokens")
public class PasswordToken extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64) public String tokenHash;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false, length = 10) public String purpose;
    @Column(nullable = false) public LocalDateTime expiresAt;
    public LocalDateTime usedAt;
    public LocalDateTime createdAt = LocalDateTime.now();
}
