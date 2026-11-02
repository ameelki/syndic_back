package lu.easy.syndic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
public class Announcement extends BuildingScoped {
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) public String title;
    @NotBlank @Size(max = 4000) @Column(nullable = false, length = 4000) public String content;
    public boolean important = false;
    @Column(updatable = false) public LocalDateTime createdAt = LocalDateTime.now();
}
