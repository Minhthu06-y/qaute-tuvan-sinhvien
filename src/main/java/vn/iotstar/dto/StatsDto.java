package vn.iotstar.dto;

import lombok.Value;
import java.util.List;

@Value
public class StatsDto {
    String period, fromLabel, toLabel;
    long totalQuestions, answered, unanswered, totalAnswers, pendingApproval;
    Double avgRating;                       // điểm hiệu quả TB trong kỳ
    List<String> labels;
    List<Long> questionSeries, answerSeries;
    List<AdvisorStat> topAdvisors;
    List<DeptStat> byDepartment;
    List<RatingStat> advisorRatings;        // điểm TB theo tư vấn viên trong kỳ
    long managerAnswerLikes, managerAnswerDislikes;
    List<VoteDeptStat> managerAnswerVotes;

    @Value public static class AdvisorStat { String name; String email; long answers; }
    @Value public static class DeptStat { String name; long total; long answered; long unanswered; }
    @Value public static class RatingStat { String name; String email; double avg; long count; }
    @Value public static class VoteDeptStat { String name; long likes; long dislikes; }
    /** Từng lượt đánh giá (điểm + nhận xét) để đưa vào báo cáo. */
    @Value public static class RatingDetail {
        String time; String advisor; String question; int rating; String comment; String manager;
    }
}

