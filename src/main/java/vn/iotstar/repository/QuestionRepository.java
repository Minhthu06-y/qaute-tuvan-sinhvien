package vn.iotstar.repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.iotstar.model.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByFacultyIdOrderByCreatedAtDesc(Long facultyId);
    List<Question> findAllByOrderByCreatedAtDesc();
    List<Question> findByFacultyIdAndLockedFalseOrderByCreatedAtDesc(Long facultyId);
    List<Question> findByAskerIdOrderByCreatedAtDesc(Long askerId);
    List<Question> findByFacultyIdAndIsPublicTrueOrderByCreatedAtDesc(Long facultyId);
    List<Question> findByIsPublicTrueOrderByCreatedAtDesc();
    List<Question> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);
    List<Question> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);
    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            vn.iotstar.enums.QuestionStatus status, LocalDateTime from, LocalDateTime to);
    @Query("select q.createdAt from Question q where q.createdAt >= :from and q.createdAt < :to")
    List<LocalDateTime> createdTimes(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
