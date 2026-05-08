package com.bcommerce.web;

import com.bcommerce.auth.AuthService;
import com.bcommerce.web.dto.AuthLoginRequest;
import com.bcommerce.web.dto.AuthRegisterRequest;
import com.bcommerce.web.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public TokenResponse register(@Valid @RequestBody AuthRegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody AuthLoginRequest req) {
        return authService.login(req);
    }
}
