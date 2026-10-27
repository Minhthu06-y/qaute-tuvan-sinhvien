package vn.iotstar.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class ManagerForm {
    @NotBlank(message = "Vui lòng nhập họ tên") private String fullName;
    @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") private String email;
    @NotBlank(message = "Vui lòng nhập mật khẩu tạm")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
             message = "Mật khẩu ≥ 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt")
    private String password;
    @NotEmpty(message = "Chọn ít nhất 1 khoa phụ trách") private List<Long> departmentIds;
}

