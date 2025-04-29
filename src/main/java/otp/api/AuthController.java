package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.service.UserService;
import otp.model.UserRole;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;
import java.util.Map;

public class AuthController {
    private final UserService userService = new UserService();

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
            userService.register(req.username, req.password, UserRole.valueOf(req.role));
            HttpUtils.sendEmptyResponse(exchange, 201); // Created
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, "Invalid role or JSON");
        } catch (UserService.UserAlreadyExistsException e) {
            HttpUtils.sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

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
            String json = JsonUtil.toJson(Map.of("token", token));
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (UserService.AuthenticationException e) {
            HttpUtils.sendError(exchange, 401, "Invalid username or password");
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    // DTOs для разбора JSON
    private static class RegisterRequest {
        public String username;
        public String password;
        public String role;
    }
    private static class LoginRequest {
        public String username;
        public String password;
    }
}
