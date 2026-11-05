package lu.easy.syndic.repository;

import lu.easy.syndic.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PasswordTokenRepository extends JpaRepository<PasswordToken, Long> {
    Optional<PasswordToken> findByTokenHash(String tokenHash);
    Optional<PasswordToken> findFirstByUserIdOrderByIdDesc(Long userId);
    void deleteAllByUserId(Long userId);
}
