package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.dao.impl.OtpCodeDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.model.User;
import otp.service.AdminService;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Контроллер для административных операций (роль ADMIN).
 * <p>
 * Доступные маршруты:
 * <ul>
 *   <li>PATCH  /admin/config     — изменить длину и время жизни OTP-кодов</li>
 *   <li>GET    /admin/users      — получить список всех пользователей без админов</li>
 *   <li>DELETE /admin/users/{id} — удалить пользователя и связанные OTP-коды</li>
 * </ul>
 * </p>
 */
public class AdminController {
    private final AdminService adminService = new AdminService(
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new OtpCodeDaoImpl()
    );

    /**
     * Обрабатывает HTTP PATCH запрос на изменение конфигурации OTP.
     * <p>
     * Ожидает JSON: {"length": 6, "ttlSeconds": 300}
     * </p>
     * <ul>
     *   <li>204 No Content — успешно обновлено</li>
     *   <li>400 Bad Request — если параметры некорректны</li>
     *   <li>415 Unsupported Media Type — если Content-Type не application/json</li>
     *   <li>405 Method Not Allowed — если метод не PATCH</li>
     *   <li>500 Internal Server Error — другие ошибки</li>
     * </ul>
     *
     * @param exchange HTTP-контекст текущего запроса
     * @throws IOException при ошибках ввода-вывода
     */
    public void updateOtpConfig(HttpExchange exchange) throws IOException {
        if (!"PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        String ct = exchange.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.contains("application/json")) {
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            ConfigRequest req = JsonUtil.fromJson(exchange.getRequestBody(), ConfigRequest.class);
            adminService.updateOtpConfig(req.length, req.ttlSeconds);
            HttpUtils.sendEmptyResponse(exchange, 204);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP GET запрос для получения списка пользователей без админов.
     * <ul>
     *   <li>200 OK — возвращает JSON-массив пользователей</li>
     *   <li>405 Method Not Allowed — если метод не GET</li>
     *   <li>500 Internal Server Error — другие ошибки</li>
     * </ul>
     *
     * @param exchange HTTP-контекст текущего запроса
     * @throws IOException при ошибках ввода-вывода
     */
    public void listUsers(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            List<User> users = adminService.getAllUsersWithoutAdmins();
            String json = JsonUtil.toJson(users);
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP DELETE запрос на удаление пользователя по ID.
     * <ul>
     *   <li>204 No Content — успешно удалено</li>
     *   <li>400 Bad Request — если ID некорректен</li>
     *   <li>404 Not Found — если пользователь не найден</li>
     *   <li>405 Method Not Allowed — если метод не DELETE</li>
     *   <li>500 Internal Server Error — другие ошибки</li>
     * </ul>
     *
     * @param exchange HTTP-контекст текущего запроса
     * @throws IOException при ошибках ввода-вывода
     */
    public void deleteUser(HttpExchange exchange) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            URI uri = exchange.getRequestURI();
            String[] segments = uri.getPath().split("/");
            Long id = Long.valueOf(segments[segments.length - 1]);
            adminService.deleteUserAndCodes(id);
            HttpUtils.sendEmptyResponse(exchange, 204);
        } catch (NumberFormatException e) {
            HttpUtils.sendError(exchange, 400, "Invalid user ID");
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * DTO для разбора JSON тела PATCH запроса /admin/config.
     */
    private static class ConfigRequest {
        public int length;
        public int ttlSeconds;
    }
}
