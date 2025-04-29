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

public class AdminController {
    private final AdminService adminService = new AdminService(
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new OtpCodeDaoImpl()
    );

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
            HttpUtils.sendEmptyResponse(exchange, 204); // No Content
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

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

    // DTO для PATCH /admin/config
    private static class ConfigRequest {
        public int length;
        public int ttlSeconds;
    }
}

