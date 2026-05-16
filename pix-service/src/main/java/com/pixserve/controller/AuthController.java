package com.pixserve.controller;

import com.pixserve.dto.LoginRequest;

import com.pixserve.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        // Temporary hardcoded credentials
        if ("admin".equals(request.getUsername())
                && "Test@123".equals(request.getPassword())) {

            String token = JwtUtil.generateToken(request.getUsername());

            return ResponseEntity.ok(
                    Map.of("token", token)
            );
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }
}