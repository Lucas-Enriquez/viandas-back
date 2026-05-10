package com.viandas.api.invitation.domain;

import com.viandas.api.company.domain.Company;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "global_invitation")
@NoArgsConstructor
@Getter
@Setter
public class GlobalInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Company company;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "max_uses")
    @Positive
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    Instant createdAt = Instant.now();

    @Column(name = "revoked_at")
    Instant revokedAt;


    public GlobalInvitation(
            Company company,
            Instant expiresAt
    ) {
        this.company = company;
        this.expiresAt = expiresAt;
    }
}
