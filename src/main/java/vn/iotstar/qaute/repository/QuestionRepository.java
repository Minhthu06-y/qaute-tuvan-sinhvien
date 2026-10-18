package vn.iotstar.qaute.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.iotstar.qaute.entity.Question;

public interface QuestionRepository
        extends JpaRepository<Question, Long> {

    List<Question> findByPublicQuestionTrueOrderByCreatedAtDesc();

    List<Question> findByAuthor_IdOrderByCreatedAtDesc(Long userId);
}