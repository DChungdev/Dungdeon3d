package com.dungeondelvers.persistence.repository;

import com.dungeondelvers.persistence.entity.CharacterEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<CharacterEntity, UUID> {

    List<CharacterEntity> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    boolean existsByNameIgnoreCase(String name);

    Optional<CharacterEntity> findByIdAndAccountId(UUID id, UUID accountId);
}
