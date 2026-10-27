package vn.iotstar.controller;

import vn.iotstar.repository.ActivityLogRepository;
import vn.iotstar.dto.StatsDto;
import vn.iotstar.entity.Department;
import vn.iotstar.enums.*;
import vn.iotstar.model.UserAccount;
import vn.iotstar.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final StatsService stats;
    private final ReportService reports;
    private final ActivityLogRepository logRepo;
    private final vn.iotstar.repository.QuestionRepository frontendQuestions;
    private final vn.iotstar.repository.AnswerRepository frontendAnswers;

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(@RequestParam(defaultValue = "week") String period, Model m) {
        var overview = combinedOverview(period);
        m.addAttribute("stats", overview); m.addAttribute("base", "/admin");
        m.addAttribute("chartMax", chartMax(overview));
        return "stats/dashboard";
    }

    @GetMapping("/manager/dashboard")
    @PreAuthorize("hasRole('MANAGER')")
    public String managerDashboard(@RequestParam(defaultValue = "week") String period, Model m) {
        var overview = combinedOverview(period);
        m.addAttribute("stats", overview); m.addAttribute("base", "/manager");
        m.addAttribute("chartMax", chartMax(overview));
        return "stats/dashboard";
    }

    private long chartMax(vn.iotstar.dto.StatsDto overview) {
        return Math.max(1, java.util.stream.Stream.concat(overview.getQuestionSeries().stream(), overview.getAnswerSeries().stream())
                .mapToLong(Long::longValue).max().orElse(1));
    }

    /** The merged app keeps the original student UI data and the QAUTE admin data in separate tables. */
    private StatsDto combinedOverview(String period) {
        StatsDto qaute = stats.overview(period);
        LocalDate from = LocalDate.parse(qaute.getFromLabel());
        LocalDate endExclusive = LocalDate.parse(qaute.getToLabel()).plusDays(1);
        List<vn.iotstar.model.Question> appQuestions = frontendQuestions.findAll().stream()
                .filter(question -> question.getCreatedAt() != null
                        && !question.getCreatedAt().toLocalDate().isBefore(from)
                        && question.getCreatedAt().toLocalDate().isBefore(endExclusive))
                .toList();
        List<vn.iotstar.model.Answer> appAnswers = frontendAnswers.findAll().stream()
                .filter(answer -> answer.getCreatedAt() != null
                        && !answer.getCreatedAt().toLocalDate().isBefore(from)
                        && answer.getCreatedAt().toLocalDate().isBefore(endExclusive))
                .toList();

        List<Long> questionSeries = new ArrayList<>(qaute.getQuestionSeries());
        List<Long> answerSeries = new ArrayList<>(qaute.getAnswerSeries());
        for (int i = 0; i < questionSeries.size(); i++) {
            LocalDate day = from.plusDays(i);
            long questionsOnDay = appQuestions.stream().filter(q -> q.getCreatedAt().toLocalDate().equals(day)).count();
            long answersOnDay = appAnswers.stream().filter(a -> a.getCreatedAt().toLocalDate().equals(day)).count();
            questionSeries.set(i, questionSeries.get(i) + questionsOnDay);
            answerSeries.set(i, answerSeries.get(i) + answersOnDay);
        }

        Map<String, long[]> departments = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (StatsDto.DeptStat row : qaute.getByDepartment()) {
            departments.put(row.getName(), new long[]{row.getTotal(), row.getAnswered(), row.getUnanswered()});
        }
        for (vn.iotstar.model.Question question : appQuestions) {
            String name = question.getFaculty() == null ? "Chưa phân khoa" : question.getFaculty().getName();
            long[] row = departments.computeIfAbsent(name, ignored -> new long[3]);
            row[0]++;
            if (question.getStatus() == vn.iotstar.enums.QuestionStatus.ANSWERED) row[1]++;
            else if (question.getStatus() == vn.iotstar.enums.QuestionStatus.OPEN) row[2]++;
        }
        List<StatsDto.DeptStat> byDepartment = departments.entrySet().stream()
                .map(entry -> new StatsDto.DeptStat(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .toList();

        Map<String, UserAccount> appAdvisorByEmail = new HashMap<>();
        Map<String, Long> appAdvisorAnswers = new HashMap<>();
        for (vn.iotstar.model.Answer answer : appAnswers) {
            UserAccount advisor = answer.getAuthor();
            if (advisor == null) continue;
            appAdvisorByEmail.put(advisor.getEmail(), advisor);
            appAdvisorAnswers.merge(advisor.getEmail(), 1L, Long::sum);
        }
        List<StatsDto.AdvisorStat> topAdvisors = appAdvisorAnswers.entrySet().stream()
                .map(entry -> new StatsDto.AdvisorStat(appAdvisorByEmail.get(entry.getKey()).getDisplayName(), entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(StatsDto.AdvisorStat::getAnswers).reversed())
                .limit(5).toList();
        if (topAdvisors.isEmpty()) topAdvisors = qaute.getTopAdvisors();

        long appAnswered = appQuestions.stream().filter(q -> q.getStatus() == vn.iotstar.enums.QuestionStatus.ANSWERED).count();
        long appUnanswered = appQuestions.stream().filter(q -> q.getStatus() == vn.iotstar.enums.QuestionStatus.OPEN).count();
        long appPending = appAnswers.stream().filter(a -> a.getApprovalStatus() == ApprovalStatus.PENDING).count();
        return new StatsDto(qaute.getPeriod(), qaute.getFromLabel(), qaute.getToLabel(),
                qaute.getTotalQuestions() + appQuestions.size(), qaute.getAnswered() + appAnswered,
                qaute.getUnanswered() + appUnanswered, qaute.getTotalAnswers() + appAnswers.size(),
                qaute.getPendingApproval() + appPending, qaute.getAvgRating(), qaute.getLabels(), questionSeries,
                answerSeries, topAdvisors, byDepartment, qaute.getAdvisorRatings());
    }

    /** Export báo cáo tổng quan (.xlsx) cho cả Admin và Manager */
    @GetMapping("/report/export")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "week") String period) throws IOException {
        String name = "bao-cao-" + period + "-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(reports.exportOverview(period));
    }

    /** Trang báo cáo dạng in: bấm "In / Lưu PDF" để xuất PDF (hỗ trợ tiếng Việt đầy đủ). */
    @GetMapping("/report/print")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String printReport(@RequestParam(defaultValue = "week") String period, Model m) {
        m.addAttribute("stats", stats.overview(period));
        m.addAttribute("ratingDetails", stats.ratingDetails(period));
        return "stats/report-print";
    }

    @GetMapping("/admin/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public String logs(@RequestParam(defaultValue = "") String kw,
                       @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "0") int page, Model m) {
        LocalDateTime f = (from == null ? LocalDate.of(2000, 1, 1) : from).atStartOfDay();
        LocalDateTime t = (to == null ? LocalDate.now() : to).plusDays(1).atStartOfDay();
        m.addAttribute("logs", logRepo.search(kw, f, t, PageRequest.of(page, 20)));
        m.addAttribute("kw", kw); m.addAttribute("from", from); m.addAttribute("to", to);
        return "admin/logs";
    }
}

