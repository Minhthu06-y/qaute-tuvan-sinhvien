package vn.iotstar.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.iotstar.entity.AnswerVote;
import vn.iotstar.entity.User;
import vn.iotstar.enums.ApprovalStatus;
import vn.iotstar.enums.Role;
import vn.iotstar.enums.VoteType;
import vn.iotstar.model.Answer;
import vn.iotstar.repository.AnswerRepository;
import vn.iotstar.repository.AnswerVoteRepository;

@Service
@RequiredArgsConstructor
public class AnswerVoteService {
    private final AnswerRepository answers;
    private final AnswerVoteRepository votes;

    @Transactional
    public void toggle(Long answerId, VoteType voteType, User user) {
        if (user.getRole() != Role.USER) throw new AccessDeniedException("Chỉ người dùng mới có thể đánh giá câu trả lời");
        Answer answer = answers.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu trả lời"));
        if (answer.getQuestion().isDeleted()) throw new IllegalArgumentException("Câu hỏi này đã được gỡ");
        Role authorRole = answer.getAuthor().getRole();
        if (authorRole != Role.MANAGER && authorRole != Role.CONSULTANT)
            throw new AccessDeniedException("Chỉ đánh giá câu trả lời của Manager hoặc tư vấn viên");
        if (answer.getApprovalStatus() != ApprovalStatus.APPROVED
                && answer.getApprovalStatus() != ApprovalStatus.NOT_REQUIRED)
            throw new IllegalArgumentException("Chỉ đánh giá được câu trả lời đã đăng");
        Optional<AnswerVote> previous = votes.findByAnswerIdAndUserId(answerId, user.getId());
        if (previous.isPresent() && previous.get().getVoteType() == voteType) {
            votes.delete(previous.get());
            return;
        }
        AnswerVote vote = previous.orElseGet(AnswerVote::new);
        vote.setAnswer(answer);
        vote.setUser(user);
        vote.setVoteType(voteType);
        vote.setCreatedAt(LocalDateTime.now());
        votes.save(vote);
    }

    @Transactional(readOnly = true)
    public VoteSummary summary(Long answerId, Long userId) {
        VoteType selected = votes.findByAnswerIdAndUserId(answerId, userId)
                .map(AnswerVote::getVoteType).orElse(null);
        return new VoteSummary(votes.countByAnswerIdAndVoteType(answerId, VoteType.LIKE),
                votes.countByAnswerIdAndVoteType(answerId, VoteType.DISLIKE), selected);
    }

    public record VoteSummary(long likes, long dislikes, VoteType userVote) {}
}
