package com.bcommerce.mapper;

import com.bcommerce.model.ProductSku;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSkuMapper {

    @Select("SELECT id, spu_id AS spuId, sku_code AS skuCode, spec_json AS specJson, price_cent AS priceCent, stock, sold FROM bc_product_sku WHERE id = #{id}")
    ProductSku findById(Long id);

    @Select(
            "SELECT id, spu_id AS spuId, sku_code AS skuCode, spec_json AS specJson, price_cent AS priceCent, stock, sold FROM bc_product_sku WHERE spu_id = #{spuId}")
    List<ProductSku> findBySpuId(Long spuId);

    @Insert(
            "INSERT INTO bc_product_sku(spu_id, sku_code, spec_json, price_cent, stock, sold) VALUES (#{spuId}, #{skuCode}, #{specJson}, #{priceCent}, #{stock}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductSku row);

    @Update(
            "UPDATE bc_product_sku SET sold = sold + #{qty}, stock = stock - #{qty} WHERE id = #{id} AND stock >= #{qty}")
    int increaseSoldDecreaseStock(@Param("id") Long id, @Param("qty") int qty);
}
