package vn.iotstar.qaute.repository;
import vn.iotstar.qaute.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);
    long countByApprovalStatus(ApprovalStatus s);
    /** DÙNG CHO TRANG CÔNG KHAI: không lộ câu trả lời đang chờ duyệt / bị từ chối. */
    List<Answer> findByQuestionIdAndPublishedTrueOrderByCreatedAtAsc(Long questionId);
    List<Answer> findByQuestionIdOrderByCreatedAtAsc(Long questionId);
    @Query("select a.createdAt from Answer a where a.createdAt >= :from and a.createdAt < :to")
    List<LocalDateTime> createdTimes(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select a.advisor.fullName, a.advisor.email, count(a) from Answer a " +
           "where a.createdAt >= :from and a.createdAt < :to group by a.advisor.fullName, a.advisor.email order by count(a) desc")
    List<Object[]> topAdvisors(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable p);
    @Query("select a from Answer a where a.approvalStatus = :s order by a.createdAt desc")
    List<Answer> findByStatus(@Param("s") ApprovalStatus s);
}

