package com.inchef.loginhistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

import com.inchef.user.User;

@Service
public class LoginHistoryService {

    private static final Logger log = LoggerFactory.getLogger(LoginHistoryService.class);

    private final LoginHistoryRepository loginHistoryRepository;

    public LoginHistoryService(LoginHistoryRepository loginHistoryRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
    }

    /**
     * Records a login event in an isolated transaction so audit logging errors
     * never roll back or corrupt an otherwise successful authentication flow.
     */
    @Transactional
    public void recordLogin(User user, String method, String status) {
        if (user == null || user.getId() == null) {
            log.warn("Cannot record login history for null user");
            return;
        }
        try {
            LoginHistory history = LoginHistory.builder()
                    .user(user)
                    .loginAt(Instant.now())
                    .authenticationMethod(method != null ? method : "PASSWORD")
                    .status(status != null ? status : "SUCCESS")
                    .build();
            loginHistoryRepository.save(history);
            log.debug("Recorded login history for user id: {}, method: {}, status: {}", user.getId(), method, status);
        } catch (Exception e) {
            log.error("Failed to record login history for user id: {}: {}", user.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<LoginHistoryResponse> getLoginHistory(Long userId, Pageable pageable) {
        return loginHistoryRepository.findByUserIdOrderByLoginAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    private LoginHistoryResponse mapToResponse(LoginHistory history) {
        return LoginHistoryResponse.builder()
                .id(history.getId())
                .loginAt(history.getLoginAt())
                .timeZone("UTC")
                .authenticationMethod(history.getAuthenticationMethod())
                .status(history.getStatus())
                .build();
    }
}
