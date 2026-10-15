package com.dungeondelvers.game.character;

import com.dungeondelvers.app.config.ExecutorConfig;
import com.dungeondelvers.game.instance.PlayerVitalsSink;
import com.dungeondelvers.persistence.repository.CharacterRepository;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Saves HP/MP when a character leaves an instance (checklist week 5), off the game thread. */
@Service
public class CharacterVitalsService implements PlayerVitalsSink {

    private static final Logger log = LoggerFactory.getLogger(CharacterVitalsService.class);

    private final CharacterRepository characterRepository;
    private final ExecutorService executor;

    public CharacterVitalsService(CharacterRepository characterRepository,
                                  @Qualifier(ExecutorConfig.GAME_PERSISTENCE) ExecutorService executor) {
        this.characterRepository = characterRepository;
        this.executor = executor;
    }

    @Override
    public void save(UUID characterId, int hp, int mp) {
        executor.execute(() -> {
            try {
                characterRepository.findById(characterId).ifPresent(character -> {
                    character.setCurrentHp(hp);
                    character.setCurrentMp(mp);
                    characterRepository.save(character);
                });
            } catch (RuntimeException ex) {
                log.error("Failed to save vitals for character {}", characterId, ex);
            }
        });
    }
}
