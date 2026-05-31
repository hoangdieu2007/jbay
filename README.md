# jBay — Hệ Thống Đấu Giá Trực Tuyến

[![CI](https://github.com/hoangdieu2007/jbay/actions/workflows/maven.yml/badge.svg)](https://github.com/hoangdieu2007/jbay/actions/workflows/maven.yml)

jBay là hệ thống đấu giá trực tuyến (online auction) cho phép nhiều người dùng tham gia cạnh tranh giá để mua sản phẩm trong khoảng thời gian xác định. Hệ thống áp dụng kiến trúc Client–Server qua TCP Socket, giao diện JavaFX, và cơ sở dữ liệu MySQL.

**Phạm vi hệ thống:** Người dùng (User/Admin), sản phẩm đấu giá, phiên đấu giá (vòng đời OPENING → RUNNING → FINISHED → PAID/CANCELED), đấu giá thủ công và tự động, cập nhật real-time, quản trị hệ thống.

## Công Nghệ Sử Dụng

| Công nghệ | Phiên bản |
|---|---|
| Java (JDK) | 25.0.2 |
| JavaFX | 25.0.1 |
| Maven | 3.x |
| MySQL | 8.0 |
| HikariCP | 6.2.1 (connection pooling) |
| SLF4J + Logback | 2.0.9 / 1.4.14 (logging) |
| JUnit 5 + Mockito | 5.12.1 / 5.14.2 (testing) |

**Hỗ trợ hệ điều hành:** Windows, macOS (Intel & Apple Silicon), Linux.

## Cấu Trúc Thư Mục

```
jbay/
├── DATABASE/                  # SQL schema và dump mẫu
├── src/main/java/a88/jbay/
│   ├── client/                # ServerConnection, ClientSession, ResponseHandler
│   ├── common/                # Model: Auction, Item, User, Request/Response
│   ├── controller/            # JavaFX Controllers
│   ├── dao/                   # Data Access Objects (UserDAO, ItemDAO, ...)
│   ├── data/                  # Repository, cache, factory
│   ├── di/                    # Dependency Injection container
│   ├── server/                # Socket server, ClientConnection, RequestHandler
│   ├── system/                # Business logic: AuctionSystem, BidSystem, ...
│   ├── util/                  # Tiện ích: ChartHelper, StringHash, ...
│   └── view/                  # JavaFX Application, ViewManager
├── src/main/resources/        # FXML, CSS, ảnh, schema SQL, logging config
├── src/test/java/             # Unit test, integration test, stress test
└── pom.xml                    # Cấu hình Maven
```

## Yêu Cầu Cài Đặt

- JDK 25.0.2 (Temurin khuyến nghị)
- Maven hoặc Maven wrapper có sẵn trong dự án
- MySQL 8.0 server
- Tài khoản database có quyền tạo và cập nhật schema

## Cài Đặt Database

```sql
CREATE DATABASE jbay_db;
mysql -u <username> -p jbay_db < DATABASE/schema1.sql
```

Khi chạy server, nhập JDBC URL dạng: `jdbc:mysql://localhost:3306/jbay_db`

## Vị Trí File .jar

Sau khi build, file .jar nằm trong thư mục `target/`:

- `target/jbay-0.3-BETA-client.jar` — Ứng dụng client
- `target/jbay-0.3-BETA-server.jar` — Ứng dụng server

## Hướng Dẫn Chạy

### 1. Build

```bash
# Windows
.\mvnw.cmd clean package

# macOS / Linux
./mvnw clean package
```

### 2. Chạy Server (trước)

```bash
java -jar target/jbay-0.3-BETA-server.jar
```

Trong cửa sổ server:
1. Nhập JDBC URL, username, password → Kết nối database
2. Nhập port → Khởi động service
3. (Tùy chọn) Đăng ký tài khoản admin

### 3. Chạy Client (sau)

```bash
java -jar target/jbay-0.3-BETA-client.jar
```

Trong cửa sổ client:
1. Nhập host (vd: `localhost`) và port của server → Kết nối
2. Đăng nhập hoặc đăng ký tài khoản
3. Bắt đầu tham gia đấu giá

## Danh Sách Chức Năng Đã Hoàn Thành

### Chức năng bắt buộc

| Chức năng | Mô tả |
|---|---|
| **Quản lý người dùng** | Đăng ký, đăng nhập, đăng xuất; Role-based (GUEST, USER, ADMIN); phân quyền theo Permission |
| **Quản lý sản phẩm** | Thêm, sửa sản phẩm (tên, mô tả, giá, ảnh, danh mục) |
| **Đấu giá** | Đặt giá thủ công, kiểm tra tính hợp lệ (>= currentPrice + minIncrement), cập nhật người dẫn đầu |
| **Kết thúc phiên** | Tự động đóng khi hết thời gian (heartbeat 1s); vòng đời OPENING→RUNNING→FINISHED→PAID/CANCELED |
| **Admin** | Xem danh sách user/auction, ban/unban user (hủy session, đóng auction, xóa auto-bid) |
| **Xử lý lỗi** | Giá không hợp lệ, phiên đã đóng, mất kết nối, lỗi DB (rollback + retry) |
| **Giao diện** | JavaFX + FXML; danh sách auction, chi tiết sản phẩm, đấu giá real-time, quản lý sản phẩm |

### Chức năng nâng cao

| Chức năng | Mô tả |
|---|---|
| **Auto-Bidding** | Đặt maxBid + increment, hệ thống tự động trả giá, so sánh nhiều auto-bid, hủy bất kỳ lúc nào |
| **Anti-sniping** | Nếu có bid trong 5 phút cuối, tự động gia hạn thêm 1 giờ |
| **Concurrent Bidding** | Per-auction ReentrantLock, virtual threads, tránh lost update và race condition |
| **Realtime Update** | Push qua TCP socket, không polling; ObservableMap binding UI tự cập nhật |
| **Bid History Chart** | LineChart (thời gian × giá), cập nhật realtime, bộ lọc tăng dần |

## Entry Points

- Client: `a88.jbay.ClientLauncher`
- Server: `a88.jbay.ServerLauncher`
- JavaFX Client: `a88.jbay.view.MainClient`
- JavaFX Server: `a88.jbay.view.MainServer`

## Phát Triển

```bash
./mvnw test          # Chạy test
./mvnw validate      # Checkstyle
./mvnw clean package # Build
```
