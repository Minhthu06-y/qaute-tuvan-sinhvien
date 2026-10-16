package vn.iotstar.qaute.repository;
import vn.iotstar.qaute.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    /** DÙNG CHO TRANG CÔNG KHAI (khách chưa đăng nhập): chỉ câu hỏi được public. */
    List<Question> findByPublicVisibleTrueOrderByCreatedAtDesc();
    List<Question> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);
    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(QuestionStatus s, LocalDateTime from, LocalDateTime to);
    @Query("select q.createdAt from Question q where q.createdAt >= :from and q.createdAt < :to")
    List<LocalDateTime> createdTimes(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

