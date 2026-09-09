package com.inchef.service;

import com.inchef.dto.request.UserCreateRequest;
import com.inchef.dto.request.UserUpdateRequest;
import com.inchef.dto.response.UserResponse;
import com.inchef.entity.User;
import com.inchef.exception.ConflictException;
import com.inchef.exception.ResourceNotFoundException;
import com.inchef.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with email '" + request.getEmail() + "' already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .avatarUrl(request.getAvatarUrl())
                .build();

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findEntityById(id);
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findEntityById(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    public void deleteUser(Long id) {
        User user = findEntityById(id);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
