package com.bcommerce.security;

public record AuthenticatedUser(Long id, String username, String role) {}
