package com.example.quanlychitieu.util;

import android.os.Handler;
import android.os.Looper;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class GMailSender {

    // Vui lòng cấu hình Email và Mật khẩu ứng dụng (App Password 16 ký tự) của bạn tại đây
    // Hướng dẫn tạo App Password: Gmail -> Quản lý tài khoản Google -> Bảo mật -> Xác minh 2 bước -> Mật khẩu ứng dụng
    public static String SENDER_EMAIL = "";
    public static String SENDER_APP_PASSWORD = "";

    public interface Callback {
        void onSuccess();
        void onFailure(String error);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void sendOtpEmail(String recipientEmail, String otpCode, Callback callback) {
        executor.execute(() -> {
            // Nếu chưa cấu hình email gửi hoặc ứng dụng chạy offline, báo lỗi để kích hoạt chế độ Giả lập / Test nhanh
            if (SENDER_EMAIL.isEmpty() || SENDER_APP_PASSWORD.isEmpty()) {
                mainHandler.post(() -> callback.onFailure("NO_CONFIG"));
                return;
            }

            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "Quản Lý Chi Tiêu"));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
                message.setSubject("Mã OTP khôi phục mật khẩu - Quản Lý Chi Tiêu");

                String htmlContent = "<div style=\"font-family: Arial, sans-serif; padding: 20px; background-color: #FFF0F5; border-radius: 12px;\">"
                        + "<h2 style=\"color: #8B1E3F;\">Quản Lý Chi Tiêu Cá Nhân</h2>"
                        + "<p>Xin chào,</p>"
                        + "<p>Bạn vừa yêu cầu mã OTP để đặt lại mật khẩu cho tài khoản <b>" + recipientEmail + "</b>.</p>"
                        + "<p>Mã OTP xác thực của bạn là:</p>"
                        + "<div style=\"font-size: 28px; font-weight: bold; color: #8B1E3F; letter-spacing: 4px; padding: 12px; background-color: #FFFFFF; display: inline-block; border-radius: 8px; border: 1px solid #8B1E3F;\">"
                        + otpCode + "</div>"
                        + "<p style=\"margin-top: 16px; color: #666;\">Mã OTP này có hiệu lực trong vòng <b>5 phút</b>. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>"
                        + "<hr style=\"border: none; border-top: 1px solid #ddd; margin-top: 20px;\">"
                        + "<p style=\"font-size: 12px; color: #999;\">Email này được gửi tự động, vui lòng không phản hồi lại.</p>"
                        + "</div>";

                message.setContent(htmlContent, "text/html; charset=utf-8");

                Transport.send(message);

                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Không thể gửi email OTP";
                mainHandler.post(() -> callback.onFailure(errorMsg));
            }
        });
    }
}
