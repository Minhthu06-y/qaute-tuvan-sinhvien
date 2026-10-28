package vn.iotstar.controller;

import vn.iotstar.repository.ActivityLogRepository;
import vn.iotstar.dto.StatsDto;
import vn.iotstar.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final StatsService stats;
    private final ReportService reports;
    private final ActivityLogRepository logRepo;
    private final ScopeService scope;
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
    public String managerDashboard(@RequestParam(defaultValue = "week") String period, Principal principal, Model m) {
        var overview = stats.overview(period, scope.managerDepartments(principal.getName()));
        m.addAttribute("stats", overview); m.addAttribute("base", "/manager");
        m.addAttribute("chartMax", chartMax(overview));
        return "stats/dashboard";
    }

    private long chartMax(vn.iotstar.dto.StatsDto overview) {
        return Math.max(1, java.util.stream.Stream.concat(overview.getQuestionSeries().stream(), overview.getAnswerSeries().stream())
                .mapToLong(Long::longValue).max().orElse(1));
    }

    private StatsDto combinedOverview(String period) {
        return stats.overview(period);
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

