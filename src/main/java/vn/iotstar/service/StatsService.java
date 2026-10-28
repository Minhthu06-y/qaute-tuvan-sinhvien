package vn.iotstar.service;

import vn.iotstar.dto.StatsDto;
import vn.iotstar.dto.StatsDto.*;
import vn.iotstar.entity.AnswerReview;
import vn.iotstar.entity.AnswerVote;
import vn.iotstar.entity.Department;
import vn.iotstar.enums.Role;
import vn.iotstar.repository.AnswerVoteRepository;
import vn.iotstar.model.Answer;
import vn.iotstar.model.Question;
import vn.iotstar.repository.AnswerRepository;
import vn.iotstar.repository.AnswerReviewRepository;
import vn.iotstar.repository.QuestionRepository;
import vn.iotstar.enums.ApprovalStatus;
import vn.iotstar.enums.QuestionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final AnswerReviewRepository reviews;
    private final AnswerVoteRepository votes;

    private LocalDate startOf(String period, LocalDate today) {
        return "month".equals(period) ? today.withDayOfMonth(1) : today.with(DayOfWeek.MONDAY);
    }

    @Transactional(readOnly = true)
    public StatsDto overview(String period) {
        return overview(period, null);
    }

    @Transactional(readOnly = true)
    public StatsDto overview(String period, Set<Long> allowedDepartments) {
        LocalDate today = LocalDate.now();
        boolean month = "month".equals(period);
        LocalDate fromDate = startOf(period, today);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        List<Question> periodQuestions = questions.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to).stream()
                .filter(question -> !question.isDeleted()).toList();
        periodQuestions = periodQuestions.stream()
                .filter(question -> ScopeService.allows(allowedDepartments, question.getDepartment())).toList();
        long total = periodQuestions.size();
        long answered = periodQuestions.stream().filter(question -> question.getStatus() == QuestionStatus.ANSWERED).count();
        long unanswered = periodQuestions.stream().filter(question -> isUnanswered(question.getStatus())).count();
        List<Answer> periodAnswers = answers.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to).stream()
                .filter(answer -> answer.getCreatedAt() != null && !answer.getCreatedAt().isBefore(from)
                        && answer.getCreatedAt().isBefore(to))
                .filter(answer -> !answer.getQuestion().isDeleted())
                .filter(answer -> ScopeService.allows(allowedDepartments, answer.getQuestion().getDepartment()))
                .toList();
        long totalAnswers = periodAnswers.size();

        Map<LocalDate, Long> questionsByDay = group(periodQuestions.stream().map(Question::getCreatedAt).toList());
        Map<LocalDate, Long> answersByDay = group(periodAnswers.stream().map(Answer::getCreatedAt).toList());
        List<String> labels = new ArrayList<>();
        List<Long> questionSeries = new ArrayList<>();
        List<Long> answerSeries = new ArrayList<>();
        for (LocalDate day = fromDate; !day.isAfter(today); day = day.plusDays(1)) {
            labels.add(day.getDayOfMonth() + "/" + day.getMonthValue());
            questionSeries.add(questionsByDay.getOrDefault(day, 0L));
            answerSeries.add(answersByDay.getOrDefault(day, 0L));
        }

        record AuthorKey(String name, String email) {}
        List<AdvisorStat> topAdvisors = periodAnswers.stream()
                .collect(Collectors.groupingBy(answer -> new AuthorKey(answer.getAuthor().getFullName(),
                        answer.getAuthor().getEmail()), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<AuthorKey, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new AdvisorStat(entry.getKey().name(), entry.getKey().email(), entry.getValue()))
                .toList();

        Map<String, long[]> departmentCounts = new TreeMap<>();
        for (Question question : periodQuestions) {
            String name = question.getDepartment() != null ? question.getDepartment().getName()
                    : question.getFaculty() != null ? question.getFaculty().getName() : "Chưa phân khoa";
            long[] counts = departmentCounts.computeIfAbsent(name, ignored -> new long[3]);
            counts[0]++;
            if (question.getStatus() == QuestionStatus.ANSWERED) counts[1]++;
            if (isUnanswered(question.getStatus())) counts[2]++;
        }
        List<DeptStat> byDepartment = departmentCounts.entrySet().stream()
                .map(entry -> new DeptStat(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .toList();

        List<AnswerReview> periodReviews = reviews
                .findByRatingIsNotNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(from, to)
                .stream().filter(review -> !review.getAnswer().getQuestion().isDeleted())
                .filter(review -> ScopeService.allows(allowedDepartments, review.getAnswer().getQuestion().getDepartment()))
                .toList();
        Map<AuthorKey, java.util.IntSummaryStatistics> ratingsByAuthor = periodReviews.stream()
                .collect(Collectors.groupingBy(review -> new AuthorKey(review.getAnswer().getAuthor().getFullName(),
                        review.getAnswer().getAuthor().getEmail()),
                        Collectors.summarizingInt(AnswerReview::getRating)));
        List<RatingStat> ratings = ratingsByAuthor.entrySet().stream()
                .map(entry -> new RatingStat(entry.getKey().name(), entry.getKey().email(),
                        entry.getValue().getAverage(), entry.getValue().getCount())).toList();
        Double averageRating = periodReviews.isEmpty() ? null
                : periodReviews.stream().mapToInt(AnswerReview::getRating).average().orElse(0.0);

        Map<String, long[]> voteCounts = new TreeMap<>();
        for (AnswerVote vote : votes.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)) {
            Answer answer = vote.getAnswer();
            Question question = answer.getQuestion();
            Department department = question.getDepartment();
            if (vote.getCreatedAt() == null || vote.getCreatedAt().isBefore(from) || !vote.getCreatedAt().isBefore(to)
                    || question.isDeleted()
                    || !EnumSet.of(Role.MANAGER, Role.CONSULTANT, Role.ADMIN).contains(answer.getAuthor().getRole())
                    || !ScopeService.allows(allowedDepartments, department)) continue;
            String departmentName = department == null ? "Chưa phân khoa" : department.getName();
            long[] counts = voteCounts.computeIfAbsent(departmentName, ignored -> new long[2]);
            if (vote.getVoteType() == vn.iotstar.enums.VoteType.LIKE) counts[0]++;
            else counts[1]++;
        }
        List<VoteDeptStat> answerVotesByDepartment = voteCounts.entrySet().stream()
                .map(entry -> new VoteDeptStat(entry.getKey(), entry.getValue()[0], entry.getValue()[1])).toList();
        long answerLikes = answerVotesByDepartment.stream().mapToLong(VoteDeptStat::getLikes).sum();
        long answerDislikes = answerVotesByDepartment.stream().mapToLong(VoteDeptStat::getDislikes).sum();

        return new StatsDto(month ? "month" : "week", fromDate.toString(), today.toString(),
                total, answered, unanswered, totalAnswers, answers.findByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus.PENDING)
                        .stream().filter(answer -> !answer.getQuestion().isDeleted())
                        .filter(answer -> ScopeService.allows(allowedDepartments, answer.getQuestion().getDepartment())).count(),
                averageRating, labels, questionSeries, answerSeries, topAdvisors,
                byDepartment, ratings, answerLikes, answerDislikes, answerVotesByDepartment);
    }

    @Transactional(readOnly = true)
    public List<RatingDetail> ratingDetails(String period) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = startOf(period, today).atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        return reviews.findByRatingIsNotNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(from, to)
                .stream()
                .map(review -> new RatingDetail(review.getCreatedAt().format(formatter),
                        review.getAnswer().getAuthor().getFullName(), review.getAnswer().getQuestion().getTitle(),
                        review.getRating(), review.getReason() == null ? "" : review.getReason(),
                        review.getManager().getFullName()))
                .collect(Collectors.toList());
    }

    private long countUnanswered(LocalDateTime from, LocalDateTime to) {
        return questions.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(QuestionStatus.UNANSWERED, from, to)
                + questions.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(QuestionStatus.OPEN, from, to);
    }

    private boolean isUnanswered(QuestionStatus status) {
        return status == QuestionStatus.UNANSWERED || status == QuestionStatus.OPEN;
    }

    private Map<LocalDate, Long> group(List<LocalDateTime> times) {
        return times.stream().collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));
    }
}
