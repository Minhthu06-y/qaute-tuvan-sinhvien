package vn.iotstar.service;

import vn.iotstar.dto.StatsDto;
import vn.iotstar.dto.StatsDto.*;
import vn.iotstar.entity.AnswerReview;
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

    private LocalDate startOf(String period, LocalDate today) {
        return "month".equals(period) ? today.withDayOfMonth(1) : today.with(DayOfWeek.MONDAY);
    }

    @Transactional(readOnly = true)
    public StatsDto overview(String period) {
        LocalDate today = LocalDate.now();
        boolean month = "month".equals(period);
        LocalDate fromDate = startOf(period, today);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        long total = questions.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to);
        long answered = questions.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                QuestionStatus.ANSWERED, from, to);
        long unanswered = countUnanswered(from, to);
        long totalAnswers = answers.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to);

        Map<LocalDate, Long> questionsByDay = group(questions.createdTimes(from, to));
        Map<LocalDate, Long> answersByDay = group(answers.createdTimes(from, to));
        List<String> labels = new ArrayList<>();
        List<Long> questionSeries = new ArrayList<>();
        List<Long> answerSeries = new ArrayList<>();
        for (LocalDate day = fromDate; !day.isAfter(today); day = day.plusDays(1)) {
            labels.add(day.getDayOfMonth() + "/" + day.getMonthValue());
            questionSeries.add(questionsByDay.getOrDefault(day, 0L));
            answerSeries.add(answersByDay.getOrDefault(day, 0L));
        }

        List<AdvisorStat> topAdvisors = answers.topAdvisors(from, to, PageRequest.of(0, 5)).stream()
                .map(row -> new AdvisorStat((String) row[0], (String) row[1], ((Number) row[2]).longValue()))
                .toList();

        Map<String, long[]> departmentCounts = new TreeMap<>();
        for (Question question : questions.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)) {
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

        List<RatingStat> ratings = reviews.advisorRatings(from, to).stream()
                .map(row -> new RatingStat((String) row[0], (String) row[1],
                        ((Number) row[2]).doubleValue(), ((Number) row[3]).longValue()))
                .toList();

        return new StatsDto(month ? "month" : "week", fromDate.toString(), today.toString(),
                total, answered, unanswered, totalAnswers, answers.countByApprovalStatus(ApprovalStatus.PENDING),
                reviews.averageRating(from, to), labels, questionSeries, answerSeries, topAdvisors,
                byDepartment, ratings);
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
