package vn.iotstar.qaute.service;

import vn.iotstar.qaute.dto.StatsDto;
import vn.iotstar.qaute.entity.*;
import vn.iotstar.qaute.repository.AdvisorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final StatsService stats;
    private final AdvisorProfileRepository advisors;

    @Transactional(readOnly = true)
    public byte[] exportOverview(String period) throws IOException {
        StatsDto s = stats.overview(period);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true); bold.setFont(f);

            Sheet sh = wb.createSheet("Tổng quan");
            row(sh, 0, bold, "Báo cáo tổng quan", s.getFromLabel() + " → " + s.getToLabel());
            row(sh, 2, null, "Tổng câu hỏi", s.getTotalQuestions());
            row(sh, 3, null, "Đã trả lời", s.getAnswered());
            row(sh, 4, null, "Chưa trả lời", s.getUnanswered());
            row(sh, 5, null, "Tổng lượt trả lời", s.getTotalAnswers());
            row(sh, 6, null, "Câu trả lời chờ duyệt", s.getPendingApproval());
            row(sh, 7, null, "Điểm hiệu quả TB", s.getAvgRating() == null ? "-" : String.format("%.2f", s.getAvgRating()));
            row(sh, 9, bold, "Ngày", "Câu hỏi", "Trả lời");
            for (int i = 0; i < s.getLabels().size(); i++)
                row(sh, 10 + i, null, s.getLabels().get(i), s.getQuestionSeries().get(i), s.getAnswerSeries().get(i));
            sh.setColumnWidth(0, 28 * 256);

            Sheet dp = wb.createSheet("Theo khoa");
            row(dp, 0, bold, "Khoa / đơn vị", "Tổng câu hỏi", "Đã trả lời", "Chưa trả lời");
            int d = 1;
            for (StatsDto.DeptStat x : s.getByDepartment())
                row(dp, d++, null, x.getName(), x.getTotal(), x.getAnswered(), x.getUnanswered());
            dp.setColumnWidth(0, 40 * 256);

            Sheet rt = wb.createSheet("Đánh giá hiệu quả");
            row(rt, 0, bold, "Tư vấn viên", "Email", "Điểm TB", "Số lượt đánh giá");
            int r1 = 1;
            for (StatsDto.RatingStat x : s.getAdvisorRatings())
                row(rt, r1++, null, x.getName(), x.getEmail(), String.format("%.2f", x.getAvg()), x.getCount());
            r1++;
            row(rt, r1++, bold, "Thời gian", "Tư vấn viên", "Câu hỏi", "Điểm", "Nhận xét", "Manager");
            for (StatsDto.RatingDetail x : stats.ratingDetails(period))
                row(rt, r1++, null, x.getTime(), x.getAdvisor(), x.getQuestion(), x.getRating(), x.getComment(), x.getManager());
            for (int c = 0; c < 6; c++) rt.setColumnWidth(c, 26 * 256);

            Sheet ad = wb.createSheet("Tư vấn viên");
            row(ad, 0, bold, "Họ tên", "Email", "SĐT", "Khoa", "Đối tượng", "Trạng thái", "Trưởng chuyên viên");
            int r = 1;
            for (AdvisorProfile a : advisors.findAll()) {
                if (a.isDeleted()) continue;           // đã gỡ khỏi đội ngũ thì không đưa vào danh sách hiện hành
                row(ad, r++, null, a.getUser().getFullName(), a.getUser().getEmail(), a.getUser().getPhone(),
                        a.getDepartment().getName(), a.getAudience().name(), a.getStatus().name(), a.isHead() ? "Có" : "");
            }
            for (int c = 0; c < 7; c++) ad.setColumnWidth(c, 24 * 256);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void row(Sheet sh, int idx, CellStyle style, Object... vals) {
        Row row = sh.createRow(idx);
        for (int i = 0; i < vals.length; i++) {
            Cell c = row.createCell(i);
            Object v = vals[i];
            if (v instanceof Number n) c.setCellValue(n.doubleValue()); else c.setCellValue(String.valueOf(v));
            if (style != null) c.setCellStyle(style);
        }
    }
}

