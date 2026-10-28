package com.inchef.loginhistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    Page<LoginHistory> findByUserIdOrderByLoginAtDesc(Long userId, Pageable pageable);
    long countByUserId(Long userId);
    void deleteByUserId(Long userId);
}

