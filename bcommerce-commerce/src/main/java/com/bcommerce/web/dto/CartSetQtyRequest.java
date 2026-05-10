package com.bcommerce.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartSetQtyRequest(@NotNull Long skuId, @NotNull @Min(0) @Max(99) Integer quantity) {}

