package otp.api;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Filter.Chain;
import otp.model.User;
import otp.model.UserRole;
import otp.util.HttpUtils;
import otp.util.TokenManager;

import java.io.IOException;

/**
 * Фильтр аутентификации и авторизации для HTTP-контроллеров.
 * <p>
 * Проверяет наличие заголовка Authorization: Bearer &lt;token&gt;,
 * валидирует токен через TokenManager и проверяет требуемую роль.
 * Если проверка проходит, сохраняет объект User в
 * exchange.setAttribute("user", user) и передаёт управление дальше.
 * Иначе возвращает соответствующий HTTP-статус:
 * <ul>
 *   <li>401 Unauthorized — при отсутствии или недействительном токене</li>
 *   <li>403 Forbidden — при недостаточности прав</li>
 * </ul>
 * </p>
 */
public class AuthFilter extends Filter {
    private final UserRole requiredRole;

    /**
     * @param requiredRole минимальная роль пользователя для доступа к ресурсу
     */
    public AuthFilter(UserRole requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public String description() {
        return "Фильтр аутентификации и проверки роли (ROLE >= " + requiredRole + ")";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            HttpUtils.sendError(exchange, 401, "Missing or invalid Authorization header");
            return;
        }
        String token = authHeader.substring(7);
        // Получаем пользователя по токену
        User user = TokenManager.getUser(token);
        if (user == null) {
            HttpUtils.sendError(exchange, 401, "Invalid or expired token");
            return;
        }
        if (user.getRole().ordinal() < requiredRole.ordinal()) {
            HttpUtils.sendError(exchange, 403, "Forbidden");
            return;
        }
        exchange.setAttribute("user", user);
        chain.doFilter(exchange);
    }
}
