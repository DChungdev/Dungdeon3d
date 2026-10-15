package com.dungeondelvers.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Marks that a character has been rewarded for one specific mob kill. Inserted with
 * {@code ON CONFLICT DO NOTHING} so a kill can never pay out twice.
 */
@Entity
@Table(name = "mob_kill_rewards")
@IdClass(MobKillRewardEntity.Key.class)
public class MobKillRewardEntity {

    /** Composite key; field names must match the @Id fields. */
    public static class Key implements Serializable {
        private UUID mobEntityId;
        private UUID characterId;

        public Key() {
        }

        public Key(UUID mobEntityId, UUID characterId) {
            this.mobEntityId = mobEntityId;
            this.characterId = characterId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(mobEntityId, k.mobEntityId)
                    && Objects.equals(characterId, k.characterId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mobEntityId, characterId);
        }
    }

    @Id
    @Column(name = "mob_entity_id")
    private UUID mobEntityId;

    @Id
    @Column(name = "character_id")
    private UUID characterId;

    @Column(name = "mob_def_id", nullable = false)
    private String mobDefId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MobKillRewardEntity() {
    }

    public UUID getMobEntityId() {
        return mobEntityId;
    }

    public UUID getCharacterId() {
        return characterId;
    }

    public String getMobDefId() {
        return mobDefId;
    }
}
