package otp.api;

import otp.util.JwtUtils;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Фильтр для аутентификации с использованием JWT токенов.
 * Проверяет токен в заголовке запроса и разрешает доступ, если токен валиден.
 */
@WebFilter("/api/*")  // Применяется ко всем запросам с префиксом /api
public class JwtAuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Инициализация фильтра (если нужно)
    }

    @Override
    public void doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Извлекаем токен из заголовка Authorization
        String token = httpRequest.getHeader("Authorization");

        // Проверка наличия и валидности токена
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // Убираем "Bearer " из начала токена
            if (JwtUtils.validateToken(token)) {
                // Если токен валидный, передаем запрос дальше
                String username = JwtUtils.extractUsername(token);
                httpRequest.setAttribute("username", username);
                chain.doFilter(request, response);
            } else {
                // Если токен невалидный, возвращаем ошибку 401
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().write("Unauthorized");
            }
        } else {
            // Если токен отсутствует, возвращаем ошибку 401
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("Unauthorized");
        }
    }

    @Override
    public void destroy() {
        // Очистка (если нужно)
    }
}

