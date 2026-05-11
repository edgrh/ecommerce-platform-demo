package com.bcommerce.mapper;

import com.bcommerce.model.SeckillActivity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillActivityMapper {

    @Insert(
            "INSERT INTO bc_seckill_activity(sku_id, name, seckill_price_cent, total_stock, sold_stock, limit_per_user, start_time, end_time, status) "
                    + "VALUES (#{skuId}, #{name}, #{seckillPriceCent}, #{totalStock}, #{soldStock}, #{limitPerUser}, #{startTime}, #{endTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SeckillActivity row);

    @Select(
            "SELECT id, sku_id AS skuId, name, seckill_price_cent AS seckillPriceCent, total_stock AS totalStock, sold_stock AS soldStock, "
                    + "limit_per_user AS limitPerUser, start_time AS startTime, end_time AS endTime, status FROM bc_seckill_activity WHERE id = #{id}")
    SeckillActivity findById(Long id);

    @Select(
            "SELECT id, sku_id AS skuId, name, seckill_price_cent AS seckillPriceCent, total_stock AS totalStock, sold_stock AS soldStock, "
                    + "limit_per_user AS limitPerUser, start_time AS startTime, end_time AS endTime, status FROM bc_seckill_activity WHERE status = 'ONLINE' ORDER BY id DESC")
    List<SeckillActivity> listOnline();

    @Update("UPDATE bc_seckill_activity SET end_time = #{end} WHERE status = 'ONLINE'")
    int extendOnlineEndTime(@Param("end") LocalDateTime end);

    @Update(
            "UPDATE bc_seckill_activity SET sold_stock = sold_stock + #{qty} WHERE id = #{id} AND sold_stock + #{qty} <= total_stock AND status = 'ONLINE'")
    int tryIncreaseSold(@Param("id") Long id, @Param("qty") int qty);
}
