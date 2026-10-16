package vn.iotstar.qaute.repository;

import vn.iotstar.qaute.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface AnswerReviewRepository extends JpaRepository<AnswerReview, Long> {
    List<AnswerReview> findByAnswerIdOrderByCreatedAtDesc(Long answerId);

    /** Mỗi Manager chỉ có 1 đánh giá cho 1 câu trả lời (chấm lại = cập nhật). */
    Optional<AnswerReview> findFirstByAnswerIdAndManagerIdAndDecision(Long answerId, Long managerId, ReviewDecision d);

    @Query("select avg(r.rating) from AnswerReview r where r.rating is not null and r.createdAt >= :from and r.createdAt < :to")
    Double averageRating(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select r.answer.advisor.fullName, r.answer.advisor.email, avg(r.rating), count(r) from AnswerReview r " +
           "where r.rating is not null and r.createdAt >= :from and r.createdAt < :to " +
           "group by r.answer.advisor.fullName, r.answer.advisor.email order by avg(r.rating) desc")
    List<Object[]> advisorRatings(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<AnswerReview> findByRatingIsNotNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);
}

