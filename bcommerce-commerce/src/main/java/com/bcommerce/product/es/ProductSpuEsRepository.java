package com.bcommerce.product.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSpuEsRepository extends ElasticsearchRepository<ProductSpuDocument, Long> {}
