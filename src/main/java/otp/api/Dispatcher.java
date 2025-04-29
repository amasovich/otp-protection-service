package otp.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/**
 * Dispatcher отвечает за регистрацию HTTP-контекстов (маршрутов) и их привязку к методам контроллеров.
 * <p>
 * Список маршрутов:
 * <ul>
 *   <li>POST   /register           → AuthController.handleRegister()  (публичный)</li>
 *   <li>POST   /login              → AuthController.handleLogin()     (публичный)</li>
 *   <li>POST   /otp/generate       → UserController.generateOtp()     (роль USER)</li>
 *   <li>POST   /otp/validate       → UserController.validateOtp()     (роль USER)</li>
 *   <li>PATCH  /admin/config       → AdminController.updateOtpConfig() (роль ADMIN)</li>
 *   <li>GET    /admin/users        → AdminController.listUsers()       (роль ADMIN)</li>
 *   <li>DELETE /admin/users/{id}   → AdminController.deleteUser()      (роль ADMIN)</li>
 * </ul>
 * </p>
 */
public class Dispatcher {
    private final AuthController authController = new AuthController();
    private final UserController userController = new UserController();
    private final AdminController adminController = new AdminController();

    /**
     * Регистрирует все HTTP-контексты на переданном сервере.
     *
     * @param server экземпляр HttpServer для регистрации маршрутов
     */
    public void registerRoutes(HttpServer server) {
        // Публичные маршруты
        server.createContext("/register", authController::handleRegister);
        server.createContext("/login",    authController::handleLogin);

        // Маршруты для пользователей (роль USER)
        server.createContext("/otp/generate", userController::generateOtp);
        server.createContext("/otp/validate", userController::validateOtp);

        // Маршруты для администратора (роль ADMIN)
        server.createContext("/admin/config", authControllerWrapper -> {
            adminController.updateOtpConfig(authControllerWrapper);
        });
        server.createContext("/admin/users", exchange -> {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                adminController.listUsers(exchange);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                adminController.deleteUser(exchange);
            } else {
                try {
                    exchange.sendResponseHeaders(405, -1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}

