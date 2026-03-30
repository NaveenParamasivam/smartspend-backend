package com.smartspend.service;

import com.smartspend.dto.DTOs.*;
import com.smartspend.entity.User;
import com.smartspend.exception.AppException;
import com.smartspend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public ApiResponse<UserResponse> updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found", 404));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        userRepository.save(user);

        return ApiResponse.success("Profile updated successfully", mapToResponse(user));
    }

    @Transactional(readOnly = true)
    public ApiResponse<UserResponse> getProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found", 404));
        return ApiResponse.success(mapToResponse(user));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .role(user.getRole())
            .enabled(user.isEnabled())
            .emailVerified(user.getEmailVerified())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
