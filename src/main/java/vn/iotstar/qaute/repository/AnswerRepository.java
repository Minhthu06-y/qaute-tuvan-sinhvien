package vn.iotstar.qaute.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.iotstar.qaute.entity.Answer;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestion_IdOrderByCreatedAtAsc(
        Long questionId
    );

    List<Answer> findByQuestion_IdAndApprovedTrueOrderByCreatedAtAsc(
        Long questionId
    );
}