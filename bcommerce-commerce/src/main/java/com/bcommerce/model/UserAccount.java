package com.bcommerce.model;

import lombok.Data;

@Data
public class UserAccount {
    private Long id;
    private String username;
    private String passwordHash;
    private String role;
    private String displayName;
}
