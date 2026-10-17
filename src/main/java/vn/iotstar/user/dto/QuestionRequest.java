package vn.iotstar.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class QuestionRequest {

    @NotBlank(message = "Vui lòng nhập tiêu đề")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "Vui lòng nhập nội dung câu hỏi")
    @Size(max = 10000)
    private String content;

    public String getTitle() { return title; }
    public String getContent() { return content; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
}