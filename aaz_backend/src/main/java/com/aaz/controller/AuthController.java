package com.aaz.controller;

import com.aaz.dto.*;
import com.aaz.entity.User;
import com.aaz.repository.UserRepository;
import com.aaz.security.jwt.JwtUtils;
import com.aaz.security.services.UserDetailsImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

            String jwt = jwtUtils.generateJwtToken(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            JwtResponse response = new JwtResponse(
                    jwt,
                    userDetails.getId(),
                    userDetails.getEmail(),
                    userDetails.getNickname());

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("帳號或密碼錯誤"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Email 已被使用"));
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("註冊成功"));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(new MessageResponse("未授權"));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("找不到使用者"));

        // 如果有修改密碼
        if (request.getOldPassword() != null && !request.getOldPassword().isEmpty()) {
            // 驗證舊密碼
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("舊密碼不符"));
            }

            // 檢查新密碼是否提供
            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("請輸入新密碼"));
            }

            // 更新密碼
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        // 更新暱稱
        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            user.setNickname(request.getNickname());
        }

        // 更新 Email
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            // 檢查新 Email 是否已被其他人使用
            if (!request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Email 已被使用"));
            }
            user.setEmail(request.getEmail());
        }

        userRepository.save(user);

        return ResponseEntity.ok(new UpdateProfileResponse(user.getNickname(), user.getEmail()));
    }
}