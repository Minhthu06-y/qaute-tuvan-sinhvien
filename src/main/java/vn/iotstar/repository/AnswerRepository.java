package vn.iotstar.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.model.Answer;
import vn.iotstar.enums.ApprovalStatus;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByQuestionIdOrderByCreatedAtAsc(Long questionId);
    List<Answer> findByApprovalStatusAndQuestionFacultyIdOrderByCreatedAtAsc(ApprovalStatus approvalStatus, Long facultyId);
    List<Answer> findByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus approvalStatus);
    boolean existsByQuestionIdAndApprovalStatus(Long questionId, ApprovalStatus approvalStatus);
}
