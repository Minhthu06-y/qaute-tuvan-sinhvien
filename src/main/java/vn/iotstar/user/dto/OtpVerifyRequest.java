package vn.iotstar.user.dto;

import jakarta.validation.constraints.*;

public class OtpVerifyRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Vui lòng nhập OTP")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP phải gồm 6 chữ số")
    private String otp;

    public String getEmail() { return email; }
    public String getOtp() { return otp; }

    public void setEmail(String email) { this.email = email; }
    public void setOtp(String otp) { this.otp = otp; }
}