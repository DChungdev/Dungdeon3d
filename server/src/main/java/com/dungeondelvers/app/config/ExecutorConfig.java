package com.dungeondelvers.app.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorConfig {

    public static final String GAME_PERSISTENCE = "gamePersistenceExecutor";

    /**
     * DB work triggered by the game loop (loot grants, HP/MP saves). Runs off the instance tick
     * threads so a slow query never stalls combat. Virtual threads: blocking JDBC is cheap here.
     */
    @Bean(name = GAME_PERSISTENCE, destroyMethod = "close")
    @Qualifier(GAME_PERSISTENCE)
    ExecutorService gamePersistenceExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("game-db-", 0).factory());
    }
}
