package com.dungeondelvers.persistence.repository;

import com.dungeondelvers.persistence.entity.CharacterEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CharacterRepository extends JpaRepository<CharacterEntity, UUID> {

    List<CharacterEntity> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    boolean existsByNameIgnoreCase(String name);

    Optional<CharacterEntity> findByIdAndAccountId(UUID id, UUID accountId);

    /** SELECT … FOR UPDATE: serializes gold / inventory changes for one character. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CharacterEntity c where c.id = :id")
    Optional<CharacterEntity> lockById(@Param("id") UUID id);
}
