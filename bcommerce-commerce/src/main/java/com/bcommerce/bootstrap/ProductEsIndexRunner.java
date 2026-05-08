package com.bcommerce.bootstrap;

import com.bcommerce.product.es.ProductSpuEsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class ProductEsIndexRunner implements ApplicationRunner {

    private final ProductSpuEsService productSpuEsService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            productSpuEsService.reindexAllFromDb();
        } catch (Exception e) {
            log.warn(
                    "Elasticsearch reindex failed (is docker compose up?). Keyword search will use MySQL LIKE until ES is available. Cause: {}",
                    e.getMessage());
        }
    }
}
