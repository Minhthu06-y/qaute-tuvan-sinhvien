# Phần Người 3 – Admin + Manager (SQL Server)

## Ghép vào repo
1. Copy `src/` vào project (package `com.hcmute.qaute`), thêm dependency từ `pom-dependencies.xml`,
   cấu hình DB từ `application.properties.snippet`.
2. Tạo DB: `CREATE DATABASE qaute COLLATE Vietnamese_100_CI_AS;` (ddl-auto=update tự tạo bảng).
3. Đổi màu giao diện ở `static/css/qaute.css` (biến `--ute-blue`, `--ute-accent`). Đặt logo vào `static/img/logo.png`.
4. Entity `[SHARED]` (User, Department, Question, Answer): khi merge giữ bản của Người 1/2, chỉ cần còn các field:
   - User: role (USER/ADMIN/MANAGER), enabled, managedDepartments
   - Question: title, content, asker, department, status, publicVisible, createdAt
   - Answer: question, advisor, content, needApproval, approvalStatus, published, createdAt
5. Người 1 cung cấp: `@EnableMethodSecurity`, bean `PasswordEncoder`, `Principal.getName()` = email,
   authority `ROLE_ADMIN` / `ROLE_MANAGER`, Principal WebSocket = email.
6. Người 2 gọi:
   - `ActivityLogService.log(email, role, action, target)` cho mọi hành động
   - Khi bật "xin duyệt": `needApproval=true, approvalStatus=PENDING, published=false`
   - Khi chuyển câu hỏi sang khoa khác: câu cũ `status=LOCKED`
   - Kiểm tra `AdvisorProfile.status == ACTIVE` trước khi cho quyền tư vấn

## Route
Admin:   /admin/dashboard, /admin/questions(/{id}), /admin/advisors(/new|/{id}/edit), /admin/managers, /admin/logs
Manager: /manager/dashboard, /manager/questions(/{id}), /manager/reviews(/{id}), /manager/heads
Chung:   /report/export (xlsx), /report/print (in / lưu PDF)

## Kiểm tra / bảo đảm (đối chiếu yêu cầu)
| Yêu cầu | Cách đảm bảo | Test |
|---|---|---|
| Admin xem câu hỏi chưa/đã trả lời + người hỏi + thời gian | /admin/questions: 3 tab kèm số lượng, cột ngày giờ, tên + email người hỏi | (thủ công) |
| Từ chối lưu lý do + đề xuất; thông báo kèm lý do | AnswerReview.reason/betterAnswer; Notification (NVARCHAR(MAX)) + WebSocket chứa cả lý do và đề xuất | ReviewServiceTest |
| Trưởng chuyên viên đúng khoa, không vượt quyền | AdvisorService.toggleHead(…, allowedDepts) -> 403; Manager chưa gán khoa = không có quyền | AdvisorServiceTest |
| Đánh giá lưu & truy xuất | AnswerReview.rating + reason(nhận xét); 1 Manager = 1 đánh giá/câu (chấm lại = cập nhật); dashboard, Excel, trang in PDF | ReviewServiceTest |
| Gỡ tư vấn viên giữ lịch sử | Xoá mềm (deleted/deletedAt/deletedBy), không xoá User/Question/Answer/Log; thêm lại = khôi phục | AdvisorServiceTest |
| Phân quyền URL | SecurityConfig + @PreAuthorize + kiểm tra khoa ở service; CSRF bật | AccessControlTest |

Người 1/2 dùng cho trang công khai: QuestionRepository.findByPublicVisibleTrueOrderByCreatedAtDesc()
và AnswerRepository.findByQuestionIdAndPublishedTrueOrderByCreatedAtAsc() (không lộ câu chờ duyệt / bị từ chối).
Kiểm tra quyền tư vấn viên: AdvisorService.isActiveAdvisor(userId).
