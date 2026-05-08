package com.bcommerce.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSpuRequest(
        @NotNull Long categoryId,
        @NotBlank String title,
        String subtitle,
        String detail) {}
