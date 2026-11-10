# HƯỚNG DẪN CẤU HÌNH FIREBASE AUTHENTICATION & GOOGLE SIGN-IN

Tài liệu này hướng dẫn chi tiết cách kết nối ứng dụng **Quản Lý Chi Tiêu Cá Nhân (QuanLyChiTieu)** với Firebase để kích hoạt tính năng **Đăng nhập Google** và **Khôi phục mật khẩu qua Email (Firebase Authentication)**.

---

### BƯỚC 1: TẠO DỰ ÁN TRÊN FIREBASE CONSOLE
1. Truy cập vào [Firebase Console](https://console.firebase.google.com/) và đăng nhập bằng tài khoản Google.
2. Nhấn **"Add project"** (Thêm dự án).
3. Đặt tên dự án (ví dụ: `QuanLyChiTieuApp`) và làm theo các bước để tạo dự án (có thể tắt Google Analytics nếu không cần).

---

### BƯỚC 2: BẬT XÁC THỰC (AUTHENTICATION)
1. Trong menu bên trái, chọn **Build** -> **Authentication**.
2. Nhấn **Get started** (Bắt đầu).
3. Tại tab **Sign-in method**, bật 2 nhà cung cấp dịch vụ:
   - **Email/Password**: Nhấn vào, bật công tắc **Enable** đầu tiên, rồi nhấn **Save**.
   - **Google**: Nhấn vào, bật **Enable**, chọn **Support email for project** (email của bạn), rồi nhấn **Save**.

---

### BƯỚC 3: THÊM ỨNG DỤNG ANDROID VÀO FIREBASE
1. Tại màn hình **Project Overview** (Tổng quan dự án), nhấn vào biểu tượng **Android**.
2. Nhập các thông tin:
   - **Android package name**: `com.example.quanlychitieu` (chính xác theo mã nguồn).
   - **App nickname**: `Quản Lý Chi Tiêu`.
   - **Debug signing certificate SHA-1**:
     - Mở terminal/cmd trên máy tính chạy lệnh sau:
       ```bash
       keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
       ```
     - Hoặc trong Android Studio: Mở tab **Gradle** (ở mép phải màn hình) -> `app` -> `Tasks` -> `android` -> nhấp đúp vào `signingReport`. Tìm dòng `SHA1` và copy chuỗi mã đó dán vào ô SHA-1 trên Firebase.
     *(Lưu ý: Bắt buộc phải có SHA-1 thì đăng nhập Google mới hoạt động!)*
3. Nhấn **Register app** (Đăng ký ứng dụng).

---

### BƯỚC 4: TẢI VÀ THAY THẾ FILE `google-services.json`
1. Tải file **`google-services.json`** vừa được Firebase tạo ra về máy.
2. Sao chép và dán đè file này vào thư mục:
   `QLCT/app/google-services.json`
3. Trong Android Studio, nhấn **Sync Project with Gradle Files** (biểu tượng con voi trên thanh công cụ).

---

### BƯỚC 5: LẤY WEB CLIENT ID CHO GOOGLE SIGN-IN (NẾU CẦN)
1. Trên Firebase Console, vào **Authentication** -> **Sign-in method** -> nhấp vào **Google**.
2. Mở rộng mục **Web SDK configuration**.
3. Sao chép chuỗi **Web client ID** (chuỗi có dạng `xxxxxx-xxxxxx.apps.googleusercontent.com`).
4. Chuỗi này đã được ứng dụng tự động nhận diện từ file `google-services.json` thông qua `R.string.default_web_client_id`. Bạn không cần sửa code thêm!

---

### TÓM TẮT CƠ CHẾ HOẠT ĐỘNG:
- **Đăng nhập Google**: Xác thực qua Firebase Auth thực tế. Khi thành công, tài khoản sẽ tự động đồng bộ vào CSDL SQLite của ứng dụng để lưu trữ các giao dịch, danh mục và ngân sách.
- **Quên mật khẩu**: Gửi email đặt lại mật khẩu trực tiếp qua máy chủ của Google/Firebase. Người dùng kiểm tra hộp thư email và bấm vào liên kết để đổi mật khẩu mới.
- **Dữ liệu ngoại tuyến (Offline)**: Đăng ký & đăng nhập truyền thống qua SQLite vẫn hoạt động 100% độc lập, đảm bảo bạn có thể trình bày bài tập lớn mọi lúc mọi nơi ngay cả khi không có kết nối internet!
