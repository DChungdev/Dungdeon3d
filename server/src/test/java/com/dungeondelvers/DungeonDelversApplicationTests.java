package com.dungeondelvers;

import com.dungeondelvers.persistence.repository.AccountRepository;
import com.dungeondelvers.persistence.repository.CharacterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@MockPersistence
class DungeonDelversApplicationTests {

    @MockitoBean
    AccountRepository accountRepository;

    @MockitoBean
    CharacterRepository characterRepository;

    @Test
    void contextLoads() {
    }
}
