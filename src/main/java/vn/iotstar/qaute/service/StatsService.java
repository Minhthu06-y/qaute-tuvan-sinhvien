package vn.iotstar.qaute.service;

import vn.iotstar.qaute.dto.StatsDto;
import vn.iotstar.qaute.dto.StatsDto.*;
import vn.iotstar.qaute.entity.*;
import vn.iotstar.qaute.repository.*;
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
    private final QauteQuestionRepository questions;
    private final QauteAnswerRepository answers;
    private final AnswerReviewRepository reviews;

    private LocalDate startOf(String period, LocalDate today) {
        return "month".equals(period) ? today.withDayOfMonth(1) : today.with(DayOfWeek.MONDAY);
    }

    /** period = "week" (tuần này, từ thứ 2) hoặc "month" (tháng này). */
    @Transactional(readOnly = true)
    public StatsDto overview(String period) {
        LocalDate today = LocalDate.now();
        boolean month = "month".equals(period);
        LocalDate fromDate = startOf(period, today);
        LocalDateTime from = fromDate.atStartOfDay(), to = today.plusDays(1).atStartOfDay();

        long total = questions.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to);
        long answered = questions.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(QuestionStatus.ANSWERED, from, to);
        long unanswered = questions.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(QuestionStatus.UNANSWERED, from, to);
        long ans = answers.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to);

        Map<LocalDate, Long> qByDay = group(questions.createdTimes(from, to));
        Map<LocalDate, Long> aByDay = group(answers.createdTimes(from, to));
        List<String> labels = new ArrayList<>();
        List<Long> qs = new ArrayList<>(), as = new ArrayList<>();
        for (LocalDate d = fromDate; !d.isAfter(today); d = d.plusDays(1)) {
            labels.add(d.getDayOfMonth() + "/" + d.getMonthValue());
            qs.add(qByDay.getOrDefault(d, 0L));
            as.add(aByDay.getOrDefault(d, 0L));
        }
        List<AdvisorStat> top = answers.topAdvisors(from, to, PageRequest.of(0, 5)).stream()
                .map(r -> new AdvisorStat((String) r[0], (String) r[1], (Long) r[2])).toList();

        Map<String, long[]> dm = new TreeMap<>();
        for (Question q : questions.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)) {
            long[] c = dm.computeIfAbsent(q.getDepartment() == null ? "(Chưa phân khoa)" : q.getDepartment().getName(), k -> new long[3]);
            c[0]++;
            if (q.getStatus() == QuestionStatus.ANSWERED) c[1]++;
            if (q.getStatus() == QuestionStatus.UNANSWERED) c[2]++;
        }
        List<DeptStat> byDept = dm.entrySet().stream()
                .map(e -> new DeptStat(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2])).toList();

        List<RatingStat> ratings = reviews.advisorRatings(from, to).stream()
                .map(r -> new RatingStat((String) r[0], (String) r[1], ((Number) r[2]).doubleValue(), (Long) r[3])).toList();

        return new StatsDto(month ? "month" : "week", fromDate.toString(), today.toString(),
                total, answered, unanswered, ans, answers.countByApprovalStatus(ApprovalStatus.PENDING),
                reviews.averageRating(from, to), labels, qs, as, top, byDept, ratings);
    }

    /** Từng lượt đánh giá trong kỳ (điểm + nhận xét) cho báo cáo. */
    @Transactional(readOnly = true)
    public List<RatingDetail> ratingDetails(String period) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = startOf(period, today).atStartOfDay(), to = today.plusDays(1).atStartOfDay();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        return reviews.findByRatingIsNotNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(from, to).stream()
                .map(r -> new RatingDetail(r.getCreatedAt().format(f), r.getAnswer().getAdvisor().getFullName(),
                        r.getAnswer().getQuestion().getTitle(), r.getRating(),
                        r.getReason() == null ? "" : r.getReason(), r.getManager().getFullName()))
                .collect(Collectors.toList());
    }

    private Map<LocalDate, Long> group(List<LocalDateTime> times) {
        return times.stream().collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));
    }
}

