package com.dungeondelvers.api.rest;

import com.dungeondelvers.api.rest.dto.CharacterDto;
import com.dungeondelvers.api.rest.dto.CreateCharacterRequest;
import com.dungeondelvers.api.rest.service.CharacterService;
import com.dungeondelvers.auth.jwt.SecurityUtils;
import com.dungeondelvers.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/characters")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping
    public ApiResponse<List<CharacterDto>> listCharacters() {
        return ApiResponse.ok(characterService.listByAccount(SecurityUtils.currentAccountId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CharacterDto> createCharacter(@Valid @RequestBody CreateCharacterRequest request) {
        return ApiResponse.ok(characterService.create(SecurityUtils.currentAccountId(), request));
    }
}
