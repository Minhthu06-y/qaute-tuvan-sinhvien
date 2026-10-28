package vn.iotstar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.iotstar.entity.QuestionReport;
import vn.iotstar.enums.QuestionReportStatus;

public interface QuestionReportRepository extends JpaRepository<QuestionReport, Long> {
    List<QuestionReport> findByStatusOrderByCreatedAtDesc(QuestionReportStatus status);
    List<QuestionReport> findByQuestionIdAndStatus(Long questionId, QuestionReportStatus status);
    boolean existsByQuestionIdAndReporterIdAndStatus(Long questionId, Long reporterId, QuestionReportStatus status);
}
