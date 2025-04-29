package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.dao.impl.UserDaoImpl;
import otp.model.UserRole;
import otp.service.UserService;
import otp.util.JsonUtil;
import otp.util.HttpUtils;
import otp.util.JwtUtils;  // Импортируем JwtUtils

import java.io.IOException;
import java.util.Map;

/**
 * Контроллер аутентификации и регистрации пользователей.
 * Обрабатывает публичные запросы:
 * <ul>
 *   <li>POST /register — регистрация нового пользователя (username, password, role)</li>
 *   <li>POST /login    — аутентификация и выдача токена (username, password)</li>
 * </ul>
 */
public class AuthController {
    private final UserService userService = new UserService(new UserDaoImpl());

    /**
     * Обрабатывает HTTP POST запрос на регистрацию пользователя.
     * Проверяет метод, Content-Type и формат JSON, затем вызывает UserService.register().
     * Возвращает:
     * <ul>
     *   <li>201 Created — при успешной регистрации</li>
     *   <li>409 Conflict — если имя занято или администратор уже существует</li>
     *   <li>415 Unsupported Media Type — если Content-Type некорректен</li>
     *   <li>405 Method Not Allowed — если метод не POST</li>
     *   <li>500 Internal Server Error — при других ошибках</li>
     * </ul>
     *
     * @param exchange объект HttpExchange для текущего запроса
     * @throws IOException при ошибках чтения/записи
     */
    public void handleRegister(HttpExchange exchange) throws IOException {
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
            RegisterRequest req = JsonUtil.fromJson(exchange.getRequestBody(), RegisterRequest.class);

            // Проверка, не существует ли уже администратор
            if ("ADMIN".equals(req.role) && userService.adminExists()) {
                HttpUtils.sendError(exchange, 409, "Admin already exists");
                return;
            }

            userService.register(req.username, req.password, UserRole.valueOf(req.role));
            HttpUtils.sendEmptyResponse(exchange, 201);
        } catch (IllegalArgumentException | IllegalStateException e) {
            HttpUtils.sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP POST запрос на аутентификацию пользователя.
     * Проверяет метод, Content-Type и формат JSON, затем вызывает UserService.login().
     * Возвращает:
     * <ul>
     *   <li>200 OK — возвращает JSON {"token":"..."}</li>
     *   <li>401 Unauthorized — если логин или пароль неверны</li>
     *   <li>415 Unsupported Media Type — если Content-Type некорректен</li>
     *   <li>405 Method Not Allowed — если метод не POST</li>
     *   <li>500 Internal Server Error — при других ошибках</li>
     * </ul>
     *
     * @param exchange объект HttpExchange для текущего запроса
     * @throws IOException при ошибках чтения/записи
     */
    public void handleLogin(HttpExchange exchange) throws IOException {
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
            LoginRequest req = JsonUtil.fromJson(exchange.getRequestBody(), LoginRequest.class);
            String token = userService.login(req.username, req.password);
            if (token == null) {
                HttpUtils.sendError(exchange, 401, "Unauthorized");
                return;
            }
            String json = JsonUtil.toJson(Map.of("token", token)); // Используем сгенерированный токен
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * DTO для разбора JSON тела запроса регистрации.
     */
    private static class RegisterRequest {
        public String username;
        public String password;
        public String role;
    }

    /**
     * DTO для разбора JSON тела запроса логина.
     */
    private static class LoginRequest {
        public String username;
        public String password;
    }
}
