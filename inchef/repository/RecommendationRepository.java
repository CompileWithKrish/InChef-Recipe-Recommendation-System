package com.inchef.repository;

import com.inchef.entity.Recommendation;
import com.inchef.enums.RecommendationMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByMode(RecommendationMode mode);

    @Query("SELECT r.mode, COUNT(r) FROM Recommendation r GROUP BY r.mode")
    List<Object[]> countGroupedByMode();

    @Query("SELECT r FROM Recommendation r ORDER BY r.createdAt DESC")
    List<Recommendation> findRecentRecommendations();
}
