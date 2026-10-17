package vn.iotstar.user.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class RegisterRequest {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,64}$",
        message = "Mật khẩu cần ít nhất 8 ký tự, chữ hoa, chữ thường, số và ký hiệu đặc biệt"
    )
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
        regexp = "^(0[0-9]{9}|\\+84[0-9]{9})$",
        message = "Số điện thoại Việt Nam không hợp lệ"
    )
    private String phone;

    @NotNull(message = "Vui lòng nhập ngày sinh")
    @Past(message = "Ngày sinh phải nằm trong quá khứ")
    private LocalDate dateOfBirth;

    private boolean universityStudent;

    private String studentCode;

    private Boolean currentlyStudying;

    private LocalDate graduationDate;

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getConfirmPassword() { return confirmPassword; }
    public String getPhone() { return phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public boolean isUniversityStudent() { return universityStudent; }
    public String getStudentCode() { return studentCode; }
    public Boolean getCurrentlyStudying() { return currentlyStudying; }
    public LocalDate getGraduationDate() { return graduationDate; }

    public void setFullName(String v) { fullName = v; }
    public void setEmail(String v) { email = v; }
    public void setPassword(String v) { password = v; }
    public void setConfirmPassword(String v) { confirmPassword = v; }
    public void setPhone(String v) { phone = v; }
    public void setDateOfBirth(LocalDate v) { dateOfBirth = v; }
    public void setUniversityStudent(boolean v) { universityStudent = v; }
    public void setStudentCode(String v) { studentCode = v; }
    public void setCurrentlyStudying(Boolean v) { currentlyStudying = v; }
    public void setGraduationDate(LocalDate v) { graduationDate = v; }
}