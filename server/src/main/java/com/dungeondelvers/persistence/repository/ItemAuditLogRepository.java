package com.dungeondelvers.persistence.repository;

import com.dungeondelvers.persistence.entity.ItemAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemAuditLogRepository extends JpaRepository<ItemAuditLogEntity, Long> {
}
