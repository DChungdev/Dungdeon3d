package com.dungeondelvers.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "currency_audit_log")
public class CurrencyAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "currency_type", nullable = false)
    private String currencyType;

    @Column(nullable = false)
    private int delta;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(nullable = false)
    private String reason;

    @Column(name = "ref_id")
    private UUID refId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected CurrencyAuditLogEntity() {
    }

    public CurrencyAuditLogEntity(UUID characterId, String currencyType, int delta, int balanceAfter,
                                  String reason, UUID refId) {
        this.characterId = characterId;
        this.currencyType = currencyType;
        this.delta = delta;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.refId = refId;
    }

    public Long getId() {
        return id;
    }

    public UUID getCharacterId() {
        return characterId;
    }

    public int getDelta() {
        return delta;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public String getReason() {
        return reason;
    }
}
