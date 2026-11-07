
package com.learning.souvenirstorebackend.controller;

import com.learning.souvenirstorebackend.dto.request.UserPasswordUpdateRequest;
import com.learning.souvenirstorebackend.dto.request.UserCreationRequest;
import com.learning.souvenirstorebackend.dto.request.UserUpdateRequest;
import com.learning.souvenirstorebackend.dto.response.ApiResponse;
import com.learning.souvenirstorebackend.dto.response.UserResponse;
import com.learning.souvenirstorebackend.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreationRequest userCreationRequest) {

        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(userCreationRequest))
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long userId) {

        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserById(userId))
                .build();
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long userId,
                                                @Valid @RequestBody UserUpdateRequest userUpdateRequest) {

        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, userUpdateRequest))
                .build();
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsers())
                .build();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable Long userId) {

        userService.deleteUser(userId);

        return ApiResponse.<String>builder()
                .message("User has been deleted successfully")
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {

        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PatchMapping("/me/password")
    public ApiResponse<String> changePassword(
            @Valid @RequestBody UserPasswordUpdateRequest userPasswordUpdateRequest) {

        userService.changePassword(userPasswordUpdateRequest);

        return ApiResponse.<String>builder()
                .message("Password updated successfully")
                .build();
    }

    @PutMapping("/{userId}/activate")
    public ApiResponse<UserResponse> activateUserStatus(@PathVariable Long userId) {

        return ApiResponse.<UserResponse>builder()
                .result(userService.activateUserStatus(userId))
                .build();
    }

    @PutMapping("/{userId}/deactivate")
    public ApiResponse<UserResponse> deactivateUserStatus(@PathVariable Long userId) {

        return ApiResponse.<UserResponse>builder()
                .result(userService.deactivateUserStatus(userId))
                .build();
    }
}