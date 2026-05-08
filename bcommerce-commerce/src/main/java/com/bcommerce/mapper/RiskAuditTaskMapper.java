package com.bcommerce.mapper;

import com.bcommerce.model.RiskAuditTask;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RiskAuditTaskMapper {

    @Insert(
            "INSERT INTO bc_risk_audit_task(order_id, user_id, risk_score, status, remark) "
                    + "VALUES (#{orderId}, #{userId}, #{riskScore}, #{status}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RiskAuditTask row);

    @Select(
            "SELECT id, order_id AS orderId, user_id AS userId, risk_score AS riskScore, status, remark, created_at AS createdAt "
                    + "FROM bc_risk_audit_task WHERE order_id = #{orderId} AND user_id = #{userId} ORDER BY id DESC")
    List<RiskAuditTask> listByOrderAndUser(@Param("orderId") long orderId, @Param("userId") long userId);
}
