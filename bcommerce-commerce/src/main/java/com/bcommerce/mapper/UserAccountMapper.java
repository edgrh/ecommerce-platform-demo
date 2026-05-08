package com.bcommerce.mapper;

import com.bcommerce.model.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAccountMapper {

    @Select("SELECT COUNT(*) FROM bc_user_account")
    long countAll();

    @Insert(
            "INSERT INTO bc_user_account(username, password_hash, role, display_name) VALUES (#{username}, #{passwordHash}, #{role}, #{displayName})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount row);

    @Select("SELECT id, username, password_hash AS passwordHash, role, display_name AS displayName FROM bc_user_account WHERE username = #{username}")
    UserAccount findByUsername(String username);
}
