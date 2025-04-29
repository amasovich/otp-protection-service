// src/main/java/otp/api/UserController.java
package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.service.OtpService;
import otp.dao.impl.OtpCodeDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.service.notification.NotificationServiceFactory;
import otp.service.notification.NotificationChannel;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;

public class UserController {
    // Передаём в OtpService: код DAO, конфиг DAO, UserDao и фабрику уведомлений
    private final OtpService otpService = new OtpService(
            new OtpCodeDaoImpl(),
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new NotificationServiceFactory()
    );

    public void generateOtp(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        String ct = exchange.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.contains("application/json")) {
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            GenerateRequest req = JsonUtil.fromJson(exchange.getRequestBody(), GenerateRequest.class);
            otpService.sendOtpToUser(req.userId, req.operationId, NotificationChannel.valueOf(req.channel));
            HttpUtils.sendEmptyResponse(exchange, 202); // Accepted
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void validateOtp(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        String ct = exchange.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.contains("application/json")) {
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            ValidateRequest req = JsonUtil.fromJson(exchange.getRequestBody(), ValidateRequest.class);
            boolean valid = otpService.validateOtp(req.code);
            if (valid) {
                HttpUtils.sendEmptyResponse(exchange, 200);
            } else {
                HttpUtils.sendError(exchange, 400, "Invalid or expired code");
            }
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    // DTO для /otp/generate
    private static class GenerateRequest {
        public Long userId;             // пока берём из тела, позже — из токена
        public String operationId;
        public String channel;          // "EMAIL", "SMS", "TELEGRAM", "FILE"
    }
    // DTO для /otp/validate
    private static class ValidateRequest {
        public String code;
    }
}
