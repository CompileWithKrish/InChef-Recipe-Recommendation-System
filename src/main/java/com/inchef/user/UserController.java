package com.inchef.user;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.inchef.common.ApiResponse;
import com.inchef.loginhistory.LoginHistoryResponse;
import com.inchef.loginhistory.LoginHistoryService;
import com.inchef.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final LoginHistoryService loginHistoryService;

    public UserController(UserService userService) {
        this(userService, null);
    }

    @Autowired
    public UserController(UserService userService, LoginHistoryService loginHistoryService) {
        this.userService = userService;
        this.loginHistoryService = loginHistoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return new ResponseEntity<>(ApiResponse.created("User created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        SecurityUtils.getRequiredCurrentUserId();
        List<UserResponse> list = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        SecurityUtils.validateUserAccess(id);
        UserResponse user = userService.getUserById(authUserId);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id,
                                                                @Valid @RequestBody UserUpdateRequest request) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        SecurityUtils.validateUserAccess(id);
        UserResponse updated = userService.updateUser(authUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        SecurityUtils.validateUserAccess(id);
        userService.deleteUser(authUserId);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }

    @GetMapping("/me/login-history")
    public ResponseEntity<ApiResponse<Page<LoginHistoryResponse>>> getMyLoginHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        Page<LoginHistoryResponse> history = loginHistoryService != null
                ? loginHistoryService.getLoginHistory(authUserId, pageable)
                : Page.empty(pageable);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/{id}/login-history")
    public ResponseEntity<ApiResponse<Page<LoginHistoryResponse>>> getUserLoginHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        SecurityUtils.validateUserAccess(id);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        Page<LoginHistoryResponse> history = loginHistoryService != null
                ? loginHistoryService.getLoginHistory(authUserId, pageable)
                : Page.empty(pageable);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
