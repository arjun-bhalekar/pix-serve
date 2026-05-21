package com.pixserve.controller;

import com.pixserve.dto.LoginRequest;

import com.pixserve.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Value("${pixserve.auth.username:admin}")
    private String authUsername;

    @Value("${pixserve.auth.password:Test@123}")
    private String authPassword;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        if (authUsername.equals(request.getUsername())
                && authPassword.equals(request.getPassword())) {

            String token = JwtUtil.generateToken(request.getUsername());

            return ResponseEntity.ok(
                    Map.of("token", token)
            );
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }
}
