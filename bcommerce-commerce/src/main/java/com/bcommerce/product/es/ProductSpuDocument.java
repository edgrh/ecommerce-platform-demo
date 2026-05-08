package com.bcommerce.product.es;

import com.bcommerce.model.ProductSpu;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "bcommerce-product-spu")
public class ProductSpuDocument {

    @Id
    private Long id;

    /** 索引细粒度分词，检索用 ik_smart 更稳 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String subtitle;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String detail;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    public static ProductSpuDocument from(ProductSpu p) {
        ProductSpuDocument d = new ProductSpuDocument();
        d.setId(p.getId());
        d.setTitle(p.getTitle());
        d.setSubtitle(p.getSubtitle());
        d.setDetail(p.getDetail());
        d.setStatus(p.getStatus());
        d.setCreatedAt(p.getCreatedAt());
        return d;
    }
}
