package vn.iotstar.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import vn.iotstar.enums.VoteType;
import vn.iotstar.model.Answer;

@Entity
@Table(name = "answer_votes", uniqueConstraints = @UniqueConstraint(
        name = "uk_answer_vote_user", columnNames = {"answer_id", "user_id"}))
public class AnswerVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Answer answer;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoteType voteType;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Answer getAnswer() { return answer; }
    public void setAnswer(Answer answer) { this.answer = answer; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public VoteType getVoteType() { return voteType; }
    public void setVoteType(VoteType voteType) { this.voteType = voteType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
