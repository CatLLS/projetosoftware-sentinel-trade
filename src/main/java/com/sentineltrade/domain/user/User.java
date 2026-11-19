package com.sentineltrade.domain.user;

import com.sentineltrade.infrastructure.security.SecretConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** <<critical>> {secrecy}: the password is only stored hashed and the MFA secret encrypted. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Convert(converter = SecretConverter.class)
    @Column(name = "mfa_secret", nullable = false)
    private String mfaSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {
    }

    public User(String username, String passwordHash, String mfaSecret, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.mfaSecret = mfaSecret;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getMfaSecret() { return mfaSecret; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
}
