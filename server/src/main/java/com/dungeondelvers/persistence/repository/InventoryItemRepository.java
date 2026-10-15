package com.dungeondelvers.persistence.repository;

import com.dungeondelvers.persistence.entity.InventoryItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItemEntity, UUID> {

    List<InventoryItemEntity> findByCharacterIdOrderBySlotIndexAsc(UUID characterId);
}
