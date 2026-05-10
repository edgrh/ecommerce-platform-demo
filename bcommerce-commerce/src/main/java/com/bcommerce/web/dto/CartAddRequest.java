package com.bcommerce.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartAddRequest(@NotNull Long skuId, @NotNull @Min(1) @Max(99) Integer quantity) {}

