package com.bcommerce.mapper;

import com.bcommerce.model.ProductSpu;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSpuMapper {

    @Select(
            "SELECT id, category_id AS categoryId, title, subtitle, detail, merchant_id AS merchantId, status, created_at AS createdAt, "
                    + "(SELECT MIN(k.price_cent) FROM bc_product_sku k WHERE k.spu_id = bc_product_spu.id) AS minPriceCent "
                    + "FROM bc_product_spu WHERE status = 'ON_SHELF' ORDER BY id DESC LIMIT #{limit}")
    List<ProductSpu> listOnShelf(@Param("limit") int limit);

    @Select(
            "SELECT id, category_id AS categoryId, title, subtitle, detail, merchant_id AS merchantId, status, created_at AS createdAt, "
                    + "(SELECT MIN(k.price_cent) FROM bc_product_sku k WHERE k.spu_id = bc_product_spu.id) AS minPriceCent "
                    + "FROM bc_product_spu WHERE status = 'ON_SHELF' ORDER BY id DESC")
    List<ProductSpu> listAllOnShelf();

    @Select(
            "SELECT id, category_id AS categoryId, title, subtitle, detail, merchant_id AS merchantId, status, created_at AS createdAt, "
                    + "(SELECT MIN(k.price_cent) FROM bc_product_sku k WHERE k.spu_id = bc_product_spu.id) AS minPriceCent "
                    + "FROM bc_product_spu WHERE id = #{id}")
    ProductSpu findById(Long id);

    @Select(
            "SELECT id, category_id AS categoryId, title, subtitle, detail, merchant_id AS merchantId, status, created_at AS createdAt "
                    + "FROM bc_product_spu WHERE merchant_id = #{merchantId} ORDER BY id DESC")
    List<ProductSpu> findByMerchantId(Long merchantId);

    @Insert(
            "INSERT INTO bc_product_spu(category_id, title, subtitle, detail, merchant_id, status) VALUES (#{categoryId}, #{title}, #{subtitle}, #{detail}, #{merchantId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductSpu row);

    List<ProductSpu> searchByKeyword(@Param("q") String q, @Param("limit") int limit);

    @Update(
            "UPDATE bc_product_spu SET title = #{title}, subtitle = #{subtitle} WHERE id = #{id} AND status = 'ON_SHELF'")
    int updateTitleAndSubtitle(@Param("id") long id, @Param("title") String title, @Param("subtitle") String subtitle);
}
