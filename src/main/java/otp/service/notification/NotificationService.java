package otp.service.notification;

public interface NotificationService {
    void sendCode(String recipient, String code);
}
