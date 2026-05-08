package com.bcommerce.auth;

import com.bcommerce.mapper.UserAccountMapper;
import com.bcommerce.model.UserAccount;
import com.bcommerce.security.JwtService;
import com.bcommerce.web.dto.AuthLoginRequest;
import com.bcommerce.web.dto.AuthRegisterRequest;
import com.bcommerce.web.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public TokenResponse register(AuthRegisterRequest req) {
        if (userAccountMapper.findByUsername(req.username().trim()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        UserAccount u = new UserAccount();
        u.setUsername(req.username().trim());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole("CUSTOMER");
        u.setDisplayName(req.username().trim());
        userAccountMapper.insert(u);
        return new TokenResponse(jwtService.createToken(u.getId(), u.getUsername(), u.getRole()), u.getRole());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(AuthLoginRequest req) {
        UserAccount u = userAccountMapper.findByUsername(req.username().trim());
        if (u == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return new TokenResponse(jwtService.createToken(u.getId(), u.getUsername(), u.getRole()), u.getRole());
    }
}
