package com.bcommerce.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Human-readable spec line for SKU cards and order snapshots (demo JSON in bc_product_sku.spec_json). */
public final class SkuSpecSummary {

    private static final ObjectMapper M = new ObjectMapper();

    private SkuSpecSummary() {}

    public static String of(String specJson) {
        if (specJson == null || specJson.isBlank()) {
            return "";
        }
        try {
            JsonNode n = M.readTree(specJson);
            String storage = text(n, "storage");
            String colorName = text(n, "colorName");
            if (colorName.isEmpty()) {
                colorName = colorToCn(text(n, "color"));
            }
            String model = text(n, "model");
            StringBuilder sb = new StringBuilder();
            if (!model.isEmpty()) {
                sb.append(model);
            }
            if (!storage.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append(storage);
            }
            if (!colorName.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append(colorName);
            }
            String series = text(n, "series");
            if (sb.length() == 0 && !series.isEmpty()) {
                sb.append(series);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? "" : v.asText("").trim();
    }

    private static String colorToCn(String colorKey) {
        if (colorKey == null || colorKey.isEmpty()) {
            return "";
        }
        return switch (colorKey) {
            case "silver" -> "银色";
            case "gold" -> "金色";
            case "deep_blue" -> "深蓝色";
            case "black", "space_black" -> "深空黑色";
            case "midnight" -> "午夜色";
            default -> colorKey;
        };
    }
}
