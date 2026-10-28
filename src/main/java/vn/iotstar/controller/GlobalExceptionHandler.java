package vn.iotstar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class GlobalExceptionHandler {
    /** ID không tồn tại / dữ liệu sai -> trang thông báo 404 thay vì lỗi 500. AccessDeniedException được Spring Security xử lý (403). */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(IllegalArgumentException e, Model m) {
        m.addAttribute("message", e.getMessage());
        return "error/message";
    }
}

