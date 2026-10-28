package vn.iotstar.repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.iotstar.model.Answer;
import vn.iotstar.enums.ApprovalStatus;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByQuestionIdOrderByCreatedAtAsc(Long questionId);
    List<Answer> findByApprovalStatusAndQuestionFacultyIdOrderByCreatedAtAsc(ApprovalStatus approvalStatus, Long facultyId);
    List<Answer> findByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus approvalStatus);
    boolean existsByQuestionIdAndApprovalStatus(Long questionId, ApprovalStatus approvalStatus);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);
    long countByApprovalStatus(ApprovalStatus approvalStatus);
    List<Answer> findByQuestionIdAndApprovalStatusOrderByCreatedAtAsc(Long questionId, ApprovalStatus status);
    @Query("select a.createdAt from Answer a where a.createdAt >= :from and a.createdAt < :to")
    List<LocalDateTime> createdTimes(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select a.author.fullName, a.author.email, count(a) from Answer a " +
           "where a.createdAt >= :from and a.createdAt < :to " +
           "group by a.author.fullName, a.author.email order by count(a) desc")
    List<Object[]> topAdvisors(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
