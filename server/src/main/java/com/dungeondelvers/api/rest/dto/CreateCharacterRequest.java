package com.dungeondelvers.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCharacterRequest(
        @NotBlank @Size(min = 3, max = 16)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Name must be alphanumeric or underscore")
        String name,
        @NotBlank String classType
) {}
