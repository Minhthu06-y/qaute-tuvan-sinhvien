# QAUTE — Tư vấn sinh viên

Ứng dụng Spring Boot dùng Thymeleaf, Bootstrap, Spring Data JPA, SQL Server, JWT, WebSocket và Cloudinary.

## Một bộ câu hỏi và trả lời

Toàn bộ luồng tạo, xem, trả lời, duyệt, đánh giá và thống kê hiện dùng hai entity `vn.iotstar.model.Question` và `vn.iotstar.model.Answer`, ánh xạ lần lượt vào `dbo.questions` và `dbo.answers`. Các repository dùng chung là `QuestionRepository` và `AnswerRepository`.

## Nâng cấp database SQL Server đã có dữ liệu

Sao lưu database trước và dừng ứng dụng cũ. Nếu database còn `dbo.user_accounts`, chạy [merge-user-accounts.sql](src/main/resources/db/manual/merge-user-accounts.sql) trước để chuyển tài khoản và các liên kết sang `dbo.users`. Sau đó chạy một lần [merge-question-answer-tables.sql](src/main/resources/db/manual/merge-question-answer-tables.sql) trên database `QAUTE`. Script nhập câu hỏi/trả lời từ `qaute_questions` và `qaute_answers`, cập nhật liên kết lịch sử duyệt, rồi xóa hai bảng cũ trong cùng transaction. Nếu các bảng legacy tương ứng không còn tồn tại, bỏ qua migration đó. Cuối cùng chạy [repair-question-user-foreign-keys.sql](src/main/resources/db/manual/repair-question-user-foreign-keys.sql) để xác nhận liên kết câu hỏi, câu trả lời và lịch sử chuyển khoa đều trỏ tới `dbo.users`; script sẽ dừng nếu phát hiện dữ liệu mồ côi.

Hibernate `ddl-auto=update` không tự gộp dữ liệu hoặc xóa các bảng cũ; cần chạy migration trước khi dùng bản ứng dụng này với database hiện tại.

Để cấu hình bộ tài khoản local hiện có cho kiểm thử, chạy [configure-local-test-accounts.sql](src/main/resources/db/manual/configure-local-test-accounts.sql) trên `QAUTE`. Script đặt mật khẩu ứng dụng thành `123456` bằng BCrypt, phân công ba consultant vào khoa/chuyên đề tương ứng, sửa khoa còn thiếu của câu hỏi cũ và gán manager phạm vi các khoa có dữ liệu kiểm thử.

## Cấu hình chạy

- Java 21 và SQL Server; cấu hình kết nối bằng `DB_USERNAME` và `DB_PASSWORD`.
- Cấu hình mặc định kết nối database `QAUTE`; bộ kiểm thử kết nối SQL Server database `QAUTE_TEST`, có thể đổi bằng `DB_TEST_URL`, `DB_TEST_USERNAME` và `DB_TEST_PASSWORD`.
- Đặt `JWT_SECRET` riêng cho môi trường chạy.
- Đặt `BOOTSTRAP_ADMIN_EMAIL` và `BOOTSTRAP_ADMIN_PASSWORD` để tạo admin ban đầu nếu database chưa có admin.
- Đặt `CLOUDINARY_URL` hoặc cả ba biến `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` để bật upload ảnh/tệp đính kèm lên Cloudinary. Không commit API secret vào repository. Trên PowerShell, đặt các biến trong terminal trước khi chạy ứng dụng, ví dụ `$env:CLOUDINARY_URL = 'cloudinary://<API_KEY>:<API_SECRET>@<CLOUD_NAME>'`.
- Cấu hình `MAIL_USERNAME` và `MAIL_PASSWORD` nếu cần gửi email.

Chạy ứng dụng bằng `mvn spring-boot:run`.

Tài khoản demo cục bộ (Local Demo Accounts)
Khi ứng dụng sử dụng cấu hình mặc định local, hệ thống sẽ tự động tạo tài khoản cho những khoa chưa có Manager đang hoạt động hoặc Consultant được phân công.
- Giữ nguyên tài khoản hiện có: Các tài khoản đang hoạt động sẽ được bảo toàn, không bị ghi đè.
- Quy tắc đặt email:
  - Manager: manager.dept<ID>@qaute.edu.vn
  - Consultant: consultant.dept<ID>@qaute.edu.vn
  - Nếu email đã tồn tại, hệ thống sẽ thêm hậu tố .seedN để tạo địa chỉ email khác.
- Hồ sơ Consultant: Hệ thống tự động tạo hồ sơ cố vấn và gán các chủ đề tư vấn thuộc khoa tương ứng.
- Mật khẩu demo mặc định: 123456. Có thể thay đổi mật khẩu bằng biến môi trường BOOTSTRAP_DEMO_ACCOUNT_PASSWORD.
- Phạm vi hoạt động: Chức năng tạo tài khoản tự động chỉ chạy trong cấu hình local.
- Môi trường production: Khi triển khai ứng dụng thực tế, cần đặt SPRING_PROFILES_ACTIVE=prod để sử dụng cấu hình sản xuất.
- Tạo tài khoản bổ sung: Khởi động lại ứng dụng để hệ thống tạo tài khoản cho những khoa hiện chưa có đủ nhân sự phụ trách theo điều kiện trên.
