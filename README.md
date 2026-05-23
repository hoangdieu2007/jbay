# jBay

jBay là ứng dụng đấu giá trực tuyến được xây dựng bằng JavaFX bởi Auction88, Team 12. Dự án gồm ứng dụng client, giao diện điều khiển server, giao tiếp client/server qua socket và hệ quản trị cơ sở dữ liệu tương thích MySQL.

## Tính Năng

- Đăng ký, đăng nhập, đăng xuất và quản lý phiên người dùng
- Đăng ký tài khoản quản trị viên và quản lý người dùng
- Tạo và hủy phiên đấu giá
- Đấu giá trực tiếp và tự động đấu giá
- Cập nhật trạng thái đấu giá theo thời gian thực cho các client đã kết nối
- Hỗ trợ nhiều loại mặt hàng: hàng tổng quát, đồ điện tử, tác phẩm nghệ thuật và phương tiện
- Giao diện JavaFX cho client và server
- Lưu trữ dữ liệu bằng MySQL/MariaDB thông qua các lớp DAO và repository

## Công Nghệ Sử Dụng

- Java 25
- JavaFX 25
- Maven
- MySQL hoặc MariaDB
- HikariCP cho connection pooling
- SLF4J và Logback cho logging
- JUnit 5 và Mockito cho test

## Cấu Trúc Dự Án

```text
jbay/
├── DATABASE/                  # File SQL schema và dump mẫu
├── STRUCTURE/                 # Tài liệu PDF mô tả cấu trúc package
├── src/main/java/a88/jbay/
│   ├── client/                # Kết nối socket phía client và xử lý response
│   ├── common/                # Model dùng chung cho auction, item, user, network
│   ├── controller/            # Controller JavaFX
│   ├── dao/                   # Lớp truy cập cơ sở dữ liệu
│   ├── data/                  # Repository, cache và factory
│   ├── di/                    # Cấu hình dependency cho ứng dụng
│   ├── server/                # Socket server, database và xử lý request
│   ├── system/                # Hệ thống auction, bid, update và user
│   ├── util/                  # Lớp tiện ích
│   └── view/                  # Entry point JavaFX và ViewManager
├── src/main/resources/        # FXML, CSS, hình ảnh, database resource, logging config
├── src/test/java/             # Unit test
└── pom.xml                    # Cấu hình Maven
```

## Yêu Cầu Cài Đặt

- JDK 25
- Maven, hoặc Maven wrapper có sẵn trong dự án
- MySQL hoặc MariaDB server
- Tài khoản database có quyền tạo và cập nhật schema của jBAY

Maven sẽ tự động chọn JavaFX platform classifier phù hợp cho Windows, macOS Intel, macOS Apple Silicon và Linux.

## Cài Đặt Database

Tạo database tên `jbay_db`, sau đó import schema:

```sql
CREATE DATABASE jbay_db;
```

Từ thư mục gốc của dự án, import một trong các file SQL có sẵn:

```bash
mysql -u <username> -p jbay_db < DATABASE/schema1.sql
```

Dự án cũng có bản sao schema trong `src/main/resources/a88/jbay/db/`.

Khi chạy ứng dụng server, nhập JDBC URL theo dạng:

```text
jdbc:mysql://localhost:3306/jbay_db
```

Sau đó nhập username và password của database trong giao diện server.

## Build

Trên Windows:

```powershell
.\mvnw.cmd clean package
```

Trên macOS hoặc Linux:

```bash
./mvnw clean package
```

File build sẽ được tạo trong thư mục `target/`:

- `jbay-0.1-ALPHA-client.jar`
- `jbay-0.1-ALPHA-server.jar`

## Chạy Ứng Dụng

Chạy server trước:

```bash
java -jar target/jbay-0.1-ALPHA-server.jar
```

Trong cửa sổ server:

1. Nhập JDBC URL, username và password của database.
2. Kết nối đến database.
3. Nếu cần, đăng ký tài khoản admin.
4. Nhập port và khởi động service.

Sau đó chạy client:

```bash
java -jar target/jbay-0.1-ALPHA-client.jar
```

Trong cửa sổ client, kết nối đến server bằng host và port của server. Nếu server chạy trên máy local:

```text
Host: localhost
Port: <server port>
```

## Lệnh Phát Triển

Chạy test:

```bash
./mvnw test
```

Chạy Checkstyle:

```bash
./mvnw validate
```

Build tất cả file jar:

```bash
./mvnw clean package
```

Trên Windows, thay `./mvnw` bằng `.\mvnw.cmd`.

## Entry Point Chính

- Client launcher: `a88.jbay.ClientLauncher`
- Server launcher: `a88.jbay.ServerLauncher`
- Ứng dụng JavaFX phía client: `a88.jbay.view.MainClient`
- Ứng dụng JavaFX phía server: `a88.jbay.view.MainServer`

## Các Bảng Database

Schema hiện tại gồm các bảng:

- `users`
- `sessionids`
- `items`
- `auctions`
- `bids`

## CI

GitHub Actions build dự án trên Ubuntu, Windows và macOS bằng JDK 25. Khi push lên branch `main`, workflow sẽ tạo draft release kèm các file jar client và server đã build.

## Ghi Chú

- Log khi chạy ứng dụng được ghi vào các file như `jbay.log`.
- Server phải được khởi động trước khi client có thể đăng nhập hoặc thao tác với các phiên đấu giá.
- Port của server service được chọn trong giao diện server khi khởi động.
