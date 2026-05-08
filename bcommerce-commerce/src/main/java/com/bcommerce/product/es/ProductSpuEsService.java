package com.bcommerce.product.es;

import com.bcommerce.mapper.ProductSpuMapper;
import com.bcommerce.model.ProductSpu;
import com.bcommerce.web.dto.ProductSpuResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSpuEsService {

    private final ProductSpuEsRepository esRepository;
    private final ProductSpuMapper spuMapper;
    private final ElasticsearchOperations elasticsearchOperations;

    public void reindexAllFromDb() {
        List<ProductSpu> rows = spuMapper.listAllOnShelf();
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductSpuDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();
        if (!rows.isEmpty()) {
            esRepository.saveAll(rows.stream().map(ProductSpuDocument::from).toList());
        }
        if (indexOps.exists()) {
            indexOps.refresh();
        }
        log.info("Elasticsearch product index rebuilt, {} documents", rows.size());
    }

    public void saveSpu(ProductSpu p) {
        if (p == null || p.getId() == null || !"ON_SHELF".equals(p.getStatus())) {
            return;
        }
        esRepository.save(ProductSpuDocument.from(p));
    }

    public List<ProductSpuResponse> search(String q, int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        Criteria text =
                new Criteria("title").matches(q)
                        .or(new Criteria("subtitle").matches(q))
                        .or(new Criteria("detail").matches(q));
        Criteria full = new Criteria("status").is("ON_SHELF").and(text);
        CriteriaQuery query = new CriteriaQuery(full);
        query.setPageable(PageRequest.of(0, size));
        SearchHits<ProductSpuDocument> hits = elasticsearchOperations.search(query, ProductSpuDocument.class);
        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(ProductSpuResponse::from)
                .toList();
    }
}
