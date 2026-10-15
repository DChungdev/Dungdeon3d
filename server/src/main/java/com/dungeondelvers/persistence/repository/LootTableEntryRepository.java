package com.dungeondelvers.persistence.repository;

import com.dungeondelvers.persistence.entity.LootTableEntryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LootTableEntryRepository extends JpaRepository<LootTableEntryEntity, Integer> {

    List<LootTableEntryEntity> findByLootTableIdOrderByIdAsc(String lootTableId);
}
