package com.dungeondelvers;

import com.dungeondelvers.persistence.repository.CurrencyAuditLogRepository;
import com.dungeondelvers.persistence.repository.InventoryItemRepository;
import com.dungeondelvers.persistence.repository.ItemAuditLogRepository;
import com.dungeondelvers.persistence.repository.ItemDefinitionRepository;
import com.dungeondelvers.persistence.repository.LootTableEntryRepository;
import com.dungeondelvers.persistence.repository.MobKillRewardRepository;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The "test" profile has no database. Spring Boot tests add this to mock the repositories they
 * don't stub themselves (Account/Character repositories are declared as fields where needed).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {
        InventoryItemRepository.class,
        ItemDefinitionRepository.class,
        LootTableEntryRepository.class,
        CurrencyAuditLogRepository.class,
        ItemAuditLogRepository.class,
        MobKillRewardRepository.class
})
public @interface MockPersistence {
}
