package com.dungeondelvers.persistence.repository;

import com.dungeondelvers.persistence.entity.ItemDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemDefinitionRepository extends JpaRepository<ItemDefinitionEntity, String> {
}
