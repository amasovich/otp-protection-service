// src/main/java/otp/api/AuthController.java
package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;

public class AuthController {

    // TODO: инжектить (или создавать) ваш UserService
    // private final UserService userService = new UserService();

    public void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            RegisterRequest req = JsonUtil.fromJson(exchange.getRequestBody(), RegisterRequest.class);
            // TODO: userService.register(req.username, req.password, req.role);
            HttpUtils.sendEmptyResponse(exchange, 201); // Created
        } catch (IOException e) {
            HttpUtils.sendError(exchange, 400, "Invalid JSON body");
        }
    }

    public void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            LoginRequest req = JsonUtil.fromJson(exchange.getRequestBody(), LoginRequest.class);
            // TODO: String token = userService.login(req.username, req.password);
            String dummyToken = "TODO-token";
            String responseJson = String.format("{\"token\":\"%s\"}", dummyToken);
            HttpUtils.sendJsonResponse(exchange, 200, responseJson);
        } catch (IOException e) {
            HttpUtils.sendError(exchange, 400, "Invalid JSON body");
        }
    }

    // DTO для разбора JSON-запроса /register
    private static class RegisterRequest {
        public String username;
        public String password;
        public String role;
    }

    // DTO для разбора JSON-запроса /login
    private static class LoginRequest {
        public String username;
        public String password;
    }
}

