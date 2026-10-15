package com.dungeondelvers.persistence.repository;

import com.dungeondelvers.persistence.entity.MobKillRewardEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MobKillRewardRepository extends JpaRepository<MobKillRewardEntity, MobKillRewardEntity.Key> {

    /** Returns 1 if this (kill, character) was recorded now, 0 if it had already been rewarded. */
    @Modifying
    @Query(value = """
            INSERT INTO mob_kill_rewards (mob_entity_id, character_id, mob_def_id)
            VALUES (:mobEntityId, :characterId, :mobDefId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("mobEntityId") UUID mobEntityId,
                       @Param("characterId") UUID characterId,
                       @Param("mobDefId") String mobDefId);
}
