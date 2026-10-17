package vn.iotstar.user.repository;

import vn.iotstar.user.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByQuestion_IdOrderByCreatedAtAsc(Long questionId);
}