package vn.iotstar.dto;

import vn.iotstar.enums.*;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewForm {
    @NotNull(message = "Chọn duyệt hoặc từ chối") private ReviewDecision decision;
    @NotBlank(message = "Vui lòng nhập lý do") private String reason;
    private String betterAnswer;                 // bắt buộc khi từ chối (kiểm tra ở service)
}

