package com.bcommerce.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 从 SKU 的 spec_json 拼一条可读规格摘要（订单快照、详情页）。 */
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
            String colorName = displayColor(n);
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

    private static String displayColor(JsonNode n) {
        String colorKey = normColorKey(text(n, "color"));
        String rawName = text(n, "colorName");
        if (!rawName.isEmpty() && containsHan(rawName)) {
            return rawName;
        }
        String fromKey = colorToCn(colorKey);
        if (!fromKey.isEmpty()) {
            return fromKey;
        }
        if (!rawName.isEmpty()) {
            String fromName = colorToCn(normColorKey(rawName));
            if (!fromName.isEmpty()) {
                return fromName;
            }
        }
        return colorToCn(normColorKey(rawName));
    }

    private static boolean containsHan(String s) {
        return s.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    private static String normColorKey(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.trim().toLowerCase().replace(' ', '_').replace('-', '_');
    }

    private static String colorToCn(String colorKey) {
        if (colorKey == null || colorKey.isEmpty()) {
            return "";
        }
        String k = normColorKey(colorKey);
        return switch (k) {
            case "silver", "sliver" -> "银色";
            case "gold" -> "金色";
            case "deep_blue", "deepblue" -> "深蓝色";
            case "black", "space_black", "spaceblack" -> "深空黑色";
            case "midnight" -> "午夜色";
            default -> containsHan(colorKey) ? colorKey : "";
        };
    }
}
