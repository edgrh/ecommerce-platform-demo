package com.bcommerce.mapper;

import com.bcommerce.model.FulfillmentTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FulfillmentTaskMapper {

    @Insert("INSERT INTO bc_fulfillment_task(order_id, status) VALUES (#{orderId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FulfillmentTask row);

    @Update("UPDATE bc_fulfillment_task SET status = #{status}, logistics_no = #{logisticsNo} WHERE order_id = #{orderId}")
    int updateByOrderId(FulfillmentTask row);

    @Select(
            "SELECT id, order_id AS orderId, status, logistics_no AS logisticsNo, updated_at AS updatedAt FROM bc_fulfillment_task WHERE order_id = #{orderId}")
    FulfillmentTask findByOrderId(Long orderId);
}
