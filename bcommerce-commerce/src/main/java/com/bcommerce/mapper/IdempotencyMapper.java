package com.bcommerce.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IdempotencyMapper {

    @Select("SELECT order_id FROM bc_idempotency WHERE token_key = #{key}")
    Long findOrderIdByKey(String key);

    @Insert("INSERT INTO bc_idempotency(token_key, order_id) VALUES (#{tokenKey}, #{orderId})")
    int insert(@Param("tokenKey") String tokenKey, @Param("orderId") Long orderId);
}
