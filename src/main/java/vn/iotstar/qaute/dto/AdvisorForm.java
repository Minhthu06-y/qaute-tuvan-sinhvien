package vn.iotstar.qaute.dto;

import vn.iotstar.qaute.entity.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdvisorForm {
    @NotBlank(message = "Vui lòng nhập họ tên")
    private String fullName;
    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String email;
    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;
    @NotNull(message = "Vui lòng chọn khoa")
    private Long departmentId;
    @NotNull(message = "Vui lòng chọn chuyên mục (đối tượng)")
    private AudienceType audience;
    @NotNull(message = "Vui lòng chọn trạng thái")
    private AdvisorStatus status;
    private boolean head;
}

