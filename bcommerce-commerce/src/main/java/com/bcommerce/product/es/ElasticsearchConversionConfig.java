package com.bcommerce.product.es;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

@Configuration
public class ElasticsearchConversionConfig {

    /**
     * Some existing indices store date-only strings (e.g. "2026-05-10") for fields
     * mapped to {@link LocalDateTime}. Spring Data Elasticsearch will throw a ConversionException
     * when reading search hits, which cascades into DB fallback and severe performance issues.
     */
    @Bean
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(List.of(new StringToLocalDateTimeLenient()));
    }

    static class StringToLocalDateTimeLenient implements Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            if (source == null) {
                return null;
            }
            String s = source.trim();
            if (s.isEmpty()) {
                return null;
            }
            try {
                // "2026-05-10T16:37:00.123"
                return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ignored) {
                // keep trying
            }
            try {
                // "2026-05-10T16:37:00Z" / "2026-05-10T16:37:00+08:00"
                return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
            } catch (Exception ignored) {
                // keep trying
            }
            try {
                // "2026-05-10"
                return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
            } catch (Exception e) {
                // Let Spring decide how to report if still unsupported.
                throw e;
            }
        }
    }
}

