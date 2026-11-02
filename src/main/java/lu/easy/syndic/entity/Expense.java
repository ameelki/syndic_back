package lu.easy.syndic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
public class Expense extends BuildingScoped {
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) public String label;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public ExpenseCategory category = ExpenseCategory.OTHER;
    @Size(max = 150) public String supplier;
    @NotNull @DecimalMin("0.01") @Column(nullable = false, precision = 12, scale = 2) public BigDecimal amount;
    @NotNull @Column(nullable = false) public LocalDate expenseDate;
    @Size(max = 1000) @Column(length = 1000) public String notes;
}
