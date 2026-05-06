package com.bookhub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "admin_users")
public class AdminUser extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String role = "Admin";

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}
