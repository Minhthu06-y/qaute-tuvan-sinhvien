package vn.iotstar.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.iotstar.enums.AdvisorType;
import vn.iotstar.enums.AudienceType;

@Data
public class AdvisorApplicationForm {
    @NotNull(message = "Chọn khoa / đơn vị muốn tham gia")
    private Long departmentId;

    @NotNull(message = "Chọn chuyên đề tư vấn")
    private Long topicId;

    @NotNull(message = "Chọn chức danh tư vấn")
    private AdvisorType advisorType;

    @NotNull(message = "Chọn nhóm người cần tư vấn")
    private AudienceType audience;

    @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 2000, message = "Giới thiệu không được dài quá 2.000 ký tự")
    private String motivation;
}
