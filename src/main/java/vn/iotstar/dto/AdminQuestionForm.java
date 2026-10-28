package vn.iotstar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminQuestionForm {
    @NotNull(message = "Chọn tài khoản người hỏi")
    private Long askerId;

    @NotNull(message = "Chọn khoa")
    private Long facultyId;

    @NotNull(message = "Chọn chuyên đề")
    private Long topicId;

    @NotBlank(message = "Nhập tiêu đề câu hỏi")
    @Size(max = 240, message = "Tiêu đề tối đa 240 ký tự")
    private String subject;

    @NotBlank(message = "Nhập nội dung câu hỏi")
    @Size(max = 10000, message = "Nội dung tối đa 10.000 ký tự")
    private String body;

    private boolean publicQuestion;
}
