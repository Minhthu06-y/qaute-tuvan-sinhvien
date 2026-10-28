# QAUTE — Hệ thống tư vấn sinh viên

QAUTE là ứng dụng web hỗ trợ tư vấn sinh viên, được xây dựng bằng Spring Boot. Hệ thống cho phép sinh viên đặt câu hỏi, theo dõi câu trả lời và đánh giá chất lượng tư vấn. Đồng thời, hệ thống hỗ trợ quản lý tài khoản, phân công tư vấn viên theo khoa và chuyên đề, duyệt câu hỏi và thống kê hoạt động tư vấn.

## 1. Công nghệ sử dụng

- **Backend:** Java 21, Spring Boot, Spring Data JPA
- **Frontend:** Thymeleaf, Bootstrap
- **Cơ sở dữ liệu:** Microsoft SQL Server
- **Xác thực và bảo mật:** JWT
- **Giao tiếp thời gian thực:** WebSocket
- **Lưu trữ hình ảnh và tệp đính kèm:** Cloudinary
- **Quản lý dự án:** Maven, Git và GitHub

## 2. Chức năng chính

### 2.1. Quản lý câu hỏi và câu trả lời

Toàn bộ luồng xử lý câu hỏi và câu trả lời sử dụng hai entity:

- `vn.iotstar.model.Question` ánh xạ đến bảng `dbo.questions`.
- `vn.iotstar.model.Answer` ánh xạ đến bảng `dbo.answers`.

Hai repository dùng chung là `QuestionRepository` và `AnswerRepository`.

Các chức năng chính bao gồm:

- Tạo và xem câu hỏi tư vấn.
- Xem nội dung câu trả lời.
- Cho phép tư vấn viên trả lời câu hỏi theo phạm vi được phân công.
- Duyệt câu hỏi theo quy trình của hệ thống.
- Đánh giá câu trả lời sau khi được tư vấn.
- Theo dõi lịch sử duyệt và thống kê hoạt động tư vấn.

### 2.2. Quản lý tài khoản và phân quyền

Hệ thống tổ chức các vai trò người dùng để kiểm soát quyền truy cập và chức năng tương ứng:

- **Guest:** Truy cập các chức năng công khai được hệ thống cho phép.
- **User:** Sử dụng các chức năng dành cho người dùng, bao gồm đặt câu hỏi và theo dõi quá trình tư vấn.
- **Consultant:** Tiếp nhận và trả lời câu hỏi thuộc khoa, chuyên đề được phân công.
- **Manager:** Quản lý hoạt động tư vấn trong phạm vi các khoa được giao, hỗ trợ điều phối và duyệt câu hỏi theo quyền hạn.
- **Admin:** Quản trị hệ thống, quản lý tài khoản và các cấu hình liên quan.

Quyền thực tế của từng vai trò được xác định theo cấu hình và quá trình triển khai của ứng dụng.

## 3. Phân công công việc các thành viên

Dự án QAUTE được thực hiện theo hình thức phân chia các chức năng chính giữa các thành viên nhằm đảm bảo tiến độ phát triển, thuận tiện trong quá trình phối hợp và tích hợp hệ thống.

### 3.1. Thành viên 1: Đăng nhập và đăng ký

Phụ trách xây dựng các chức năng xác thực người dùng, bao gồm:

- Xây dựng giao diện đăng nhập và đăng ký tài khoản.
- Xử lý logic đăng ký tài khoản mới.
- Xử lý xác thực thông tin đăng nhập.
- Tích hợp JWT để xác thực và quản lý phiên đăng nhập.
- Kiểm tra dữ liệu đầu vào và xử lý các trường hợp đăng nhập, đăng ký không hợp lệ.
- Phối hợp với các thành viên khác để tích hợp chức năng xác thực và phân quyền.

### 3.2. Thành viên 2: User, Consultant và Manager

Phụ trách phát triển các chức năng dành cho người dùng và hoạt động tư vấn sinh viên, bao gồm:

- **User:** Xây dựng các chức năng dành cho sinh viên như xem thông tin, gửi câu hỏi tư vấn, theo dõi câu trả lời và đánh giá chất lượng tư vấn.
- **Consultant:** Xây dựng chức năng tiếp nhận, xem và trả lời câu hỏi của sinh viên theo phạm vi phụ trách.
- **Manager:** Xây dựng chức năng quản lý hoạt động tư vấn, theo dõi và duyệt câu hỏi theo quyền hạn được cấp.
- Xây dựng và tích hợp các chức năng quản lý câu hỏi, câu trả lời và trạng thái xử lý.
- Phối hợp với các thành viên khác để đảm bảo dữ liệu và quyền truy cập hoạt động thống nhất.

### 3.3. Thành viên 3: Admin

Phụ trách xây dựng các chức năng quản trị hệ thống, bao gồm:

- Xây dựng giao diện và chức năng dành cho Admin.
- Quản lý tài khoản người dùng và các vai trò trong hệ thống.
- Quản lý thông tin khoa và các chuyên đề tư vấn.
- Hỗ trợ quản lý, phân công nhân sự tư vấn theo phạm vi chức năng được triển khai.
- Xây dựng chức năng theo dõi và thống kê hoạt động của hệ thống.
- Kiểm tra, quản lý dữ liệu và phối hợp tích hợp các chức năng quản trị với toàn bộ ứng dụng.

### 3.4. Phối hợp và tích hợp hệ thống

Các thành viên phối hợp trong quá trình phát triển, kiểm thử và tích hợp các chức năng vào ứng dụng QAUTE. Việc phân chia công việc giúp mỗi thành viên tập trung vào một nhóm chức năng cụ thể, đồng thời đảm bảo hệ thống hoạt động thống nhất về giao diện, cơ sở dữ liệu, xác thực và phân quyền.

## 4. Cấu hình chạy ứng dụng

### 4.1. Yêu cầu môi trường

- Java Development Kit (JDK) 21.
- Microsoft SQL Server.
- Maven.
- Các biến môi trường cần thiết cho kết nối cơ sở dữ liệu và những dịch vụ tích hợp.

### 4.2. Cấu hình cơ sở dữ liệu

Ứng dụng sử dụng SQL Server và mặc định kết nối đến database `QAUTE`.

Cấu hình thông tin kết nối thông qua các biến môi trường:

- `DB_USERNAME`: Tên đăng nhập SQL Server.
- `DB_PASSWORD`: Mật khẩu SQL Server.

Bộ kiểm thử sử dụng database `QAUTE_TEST`. Có thể cấu hình thông qua:

- `DB_TEST_URL`: Địa chỉ kết nối database kiểm thử.
- `DB_TEST_USERNAME`: Tên đăng nhập database kiểm thử.
- `DB_TEST_PASSWORD`: Mật khẩu database kiểm thử.

### 4.3. Cấu hình JWT

Đặt biến môi trường `JWT_SECRET` với giá trị riêng cho từng môi trường chạy ứng dụng.

Không sử dụng khóa bí mật mặc định hoặc đưa khóa bí mật thực tế lên GitHub.

### 4.4. Khởi tạo tài khoản Admin

Có thể thiết lập tài khoản quản trị ban đầu thông qua:

- `BOOTSTRAP_ADMIN_EMAIL`: Email tài khoản Admin.
- `BOOTSTRAP_ADMIN_PASSWORD`: Mật khẩu tài khoản Admin.

Cơ chế khởi tạo được áp dụng khi database chưa có tài khoản Admin theo điều kiện của ứng dụng.

### 4.5. Cấu hình Cloudinary

Để sử dụng chức năng tải hình ảnh và tệp đính kèm lên Cloudinary, cấu hình một trong hai phương án:

- Sử dụng biến `CLOUDINARY_URL`.
- Hoặc cấu hình đầy đủ `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` và `CLOUDINARY_API_SECRET`.

Ví dụ thiết lập trên PowerShell:

```powershell
$env:CLOUDINARY_URL = 'cloudinary://<API_KEY>:<API_SECRET>@<CLOUD_NAME>'
```

Không commit thông tin API secret vào repository.

### 4.6. Cấu hình email

Nếu cần sử dụng chức năng gửi email, thiết lập:

- `MAIL_USERNAME`: Tài khoản email gửi.
- `MAIL_PASSWORD`: Mật khẩu hoặc thông tin xác thực tương ứng.

## 5. Tài khoản demo cục bộ

Khi ứng dụng chạy với profile `local`, hệ thống tự động kiểm tra các khoa và tạo tài khoản demo cho những khoa chưa có Manager đang hoạt động hoặc Consultant theo điều kiện được cấu hình.

### Quy tắc tạo tài khoản

- Email Manager: `manager.dept<ID>@qaute.edu.vn`
- Email Consultant: `consultant.dept<ID>@qaute.edu.vn`
- Nếu email đã được sử dụng, hệ thống có thể thêm hậu tố `.seedN`.
- Tài khoản đang hoạt động được giữ nguyên.
- Consultant được tạo hồ sơ cố vấn và gán các chuyên đề thuộc khoa tương ứng.

Mật khẩu demo mặc định là `123456`. Có thể thay đổi bằng biến môi trường `BOOTSTRAP_DEMO_ACCOUNT_PASSWORD`.

Cơ chế tạo tài khoản demo chỉ chạy trong profile `local`. Khi cần bổ sung tài khoản cho các khoa chưa đủ nhân sự phụ trách, khởi động lại ứng dụng để hệ thống kiểm tra theo cấu hình hiện tại.

Khi triển khai môi trường thực tế, thiết lập:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
```

Không sử dụng mật khẩu demo cho tài khoản thực tế.

## 6. Khởi chạy dự án

Sau khi hoàn tất cấu hình Java, SQL Server và các biến môi trường cần thiết, mở terminal tại thư mục dự án và chạy:

```bash
mvn spring-boot:run
```

Theo dõi log khởi động để kiểm tra trạng thái kết nối cơ sở dữ liệu và các dịch vụ tích hợp.

## 7. Lưu ý bảo mật

- Không đưa `JWT_SECRET`, `DB_PASSWORD`, `CLOUDINARY_API_SECRET` hoặc mật khẩu email thực tế lên GitHub.
- Không sử dụng tài khoản và mật khẩu demo trong môi trường production.
- Cấu hình riêng các biến môi trường cho từng môi trường phát triển, kiểm thử và triển khai.
- Kiểm tra quyền truy cập theo vai trò để đảm bảo người dùng chỉ thực hiện được các chức năng thuộc phạm vi được cấp phép.

