package com.bcommerce.mapper;

import com.bcommerce.model.SettlementEntry;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface SettlementEntryMapper {

    @Insert(
            "INSERT INTO bc_settlement_entry(order_id, merchant_id, amount_cent, fee_cent, status) "
                    + "VALUES (#{orderId}, #{merchantId}, #{amountCent}, #{feeCent}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SettlementEntry row);
}
