package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.dao.impl.OtpCodeDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.service.OtpService;
import otp.service.notification.NotificationChannel;
import otp.service.notification.NotificationServiceFactory;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;

/**
 * Контроллер пользовательских операций для работы с OTP-кодами (роль USER).
 * <p>
 * Доступные маршруты:
 * <ul>
 *   <li>POST /otp/generate — генерирует и отправляет OTP-код</li>
 *   <li>POST /otp/validate — проверяет корректность и статус OTP-кода</li>
 * </ul>
 * </p>
 */
public class UserController {
    private final OtpService otpService = new OtpService(
            new OtpCodeDaoImpl(),
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new NotificationServiceFactory()
    );

    /**
     * Обрабатывает HTTP POST запрос генерации OTP-кода.
     * <p>
     * Ожидает JSON: {"userId": 123, "operationId": "op123", "channel": "EMAIL"}.
     * </p>
     * <ul>
     *   <li>202 Accepted — запрос принят и код отправлен</li>
     *   <li>400 Bad Request — неверные данные или канал</li>
     *   <li>415 Unsupported Media Type — Content-Type не application/json</li>
     *   <li>405 Method Not Allowed — метод не POST</li>
     *   <li>500 Internal Server Error — при других ошибках</li>
     * </ul>
     *
     * @param exchange текущий HTTP-контекст
     * @throws IOException при ошибках ввода-вывода
     */
    public void generateOtp(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            GenerateRequest req = JsonUtil.fromJson(exchange.getRequestBody(), GenerateRequest.class);
            otpService.sendOtpToUser(req.userId, req.operationId,
                    NotificationChannel.valueOf(req.channel));
            HttpUtils.sendEmptyResponse(exchange, 202);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP POST запрос валидации OTP-кода.
     * <p>
     * Ожидает JSON: {"code": "123456"}.
     * </p>
     * <ul>
     *   <li>200 OK — код корректен</li>
     *   <li>400 Bad Request — неверный или просроченный код</li>
     *   <li>415 Unsupported Media Type — Content-Type не application/json</li>
     *   <li>405 Method Not Allowed — метод не POST</li>
     *   <li>500 Internal Server Error — при других ошибках</li>
     * </ul>
     *
     * @param exchange текущий HTTP-контекст
     * @throws IOException при ошибках ввода-вывода
     */
    public void validateOtp(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
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

    /**
     * DTO для разбора JSON тела POST /otp/generate.
     */
    private static class GenerateRequest {
        public Long userId;
        public String operationId;
        public String channel;
    }

    /**
     * DTO для разбора JSON тела POST /otp/validate.
     */
    private static class ValidateRequest {
        public String code;
    }
}
