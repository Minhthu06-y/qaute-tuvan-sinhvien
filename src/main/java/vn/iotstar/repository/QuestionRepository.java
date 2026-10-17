package vn.iotstar.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.model.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByFacultyIdOrderByCreatedAtDesc(Long facultyId);
    List<Question> findByFacultyIdAndLockedFalseOrderByCreatedAtDesc(Long facultyId);
    List<Question> findByAskerIdOrderByCreatedAtDesc(Long askerId);
    List<Question> findByFacultyIdAndIsPublicTrueOrderByCreatedAtDesc(Long facultyId);
}
