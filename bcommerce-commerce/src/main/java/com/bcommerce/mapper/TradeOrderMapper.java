package com.bcommerce.mapper;

import com.bcommerce.model.TradeOrder;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TradeOrderMapper {

    int insert(@Param("shard") int shard, @Param("row") TradeOrder row);

    TradeOrder findByUserAndOrder(
            @Param("shard") int shard, @Param("userId") Long userId, @Param("orderId") Long orderId);

    List<TradeOrder> listByUser(@Param("shard") int shard, @Param("userId") Long userId, @Param("limit") int limit);

    List<TradeOrder> listByMerchant(@Param("merchantId") Long merchantId, @Param("limit") int limit);
}
