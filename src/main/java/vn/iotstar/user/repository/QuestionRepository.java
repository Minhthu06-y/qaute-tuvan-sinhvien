package vn.iotstar.user.repository;

import vn.iotstar.user.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository
        extends JpaRepository<Question, Long> {

    List<Question> findByIsPublicTrueAndLockedFalseOrderByCreatedAtDesc();

    List<Question> findByAuthor_IdOrderByCreatedAtDesc(Long userId);
}