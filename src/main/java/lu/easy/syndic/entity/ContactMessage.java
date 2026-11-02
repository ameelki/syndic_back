package lu.easy.syndic.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
public class ContactMessage extends BaseEntity {
    public Long ownerId;
    public String ownerName;
    @Column(nullable = false, length = 150) public String subject;
    @Column(nullable = false, length = 3000) public String content;
    @Column(length = 3000) public String reply;
    @Column(updatable = false) public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime repliedAt;
}
