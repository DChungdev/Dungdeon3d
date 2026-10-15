package com.dungeondelvers.persistence.repository;

import com.dungeondelvers.persistence.entity.CurrencyAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyAuditLogRepository extends JpaRepository<CurrencyAuditLogEntity, Long> {
}
