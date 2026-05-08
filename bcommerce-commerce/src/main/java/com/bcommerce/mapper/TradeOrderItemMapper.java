package com.bcommerce.mapper;

import com.bcommerce.model.TradeOrderItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TradeOrderItemMapper {

    int insert(@Param("shard") int shard, @Param("row") TradeOrderItem row);

    List<TradeOrderItem> listByUserAndOrder(
            @Param("shard") int shard, @Param("userId") Long userId, @Param("orderId") Long orderId);
}
