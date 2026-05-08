package com.bcommerce.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSkuRequest(
        @NotBlank String skuCode, String specJson, @NotNull @Min(1) Integer priceCent, @NotNull @Min(0) Integer stock) {}
