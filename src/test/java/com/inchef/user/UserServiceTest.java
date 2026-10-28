package com.inchef.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.inchef.exception.ConflictException;
import com.inchef.recommendation.RecommendationRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private UserService userService;

    private UserCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = UserCreateRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .phone("+1-555-1234")
                .build();
    }

    @Test
    @DisplayName("Should create new user when email is unique")
    void testCreateUserSuccess() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return u;
        });

        UserResponse response = userService.createUser(createRequest);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Jane Doe", response.getName());
        assertEquals("jane@example.com", response.getEmail());
    }

    @Test
    @DisplayName("Should throw ConflictException when email is already registered")
    void testCreateUserDuplicateEmail() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createUser(createRequest));
        verify(userRepository, never()).save(any());
    }
}
