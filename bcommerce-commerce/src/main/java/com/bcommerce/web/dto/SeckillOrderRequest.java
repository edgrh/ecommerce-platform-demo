package com.bcommerce.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SeckillOrderRequest(@NotNull Long activityId, @NotNull @Min(1) @Max(5) Integer quantity) {}
