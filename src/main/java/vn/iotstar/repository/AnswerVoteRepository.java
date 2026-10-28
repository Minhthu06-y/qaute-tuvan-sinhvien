package vn.iotstar.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.iotstar.entity.AnswerVote;
import vn.iotstar.enums.VoteType;

public interface AnswerVoteRepository extends JpaRepository<AnswerVote, Long> {
    Optional<AnswerVote> findByAnswerIdAndUserId(Long answerId, Long userId);
    List<AnswerVote> findByAnswerQuestionId(Long questionId);
    List<AnswerVote> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);
    long countByAnswerIdAndVoteType(Long answerId, VoteType voteType);
}
