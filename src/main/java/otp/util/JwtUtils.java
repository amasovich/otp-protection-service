package otp.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;

/**
 * Класс для работы с JWT токенами.
 * Предназначен для генерации и валидации токенов.
 */
public class JwtUtils {

    // Секретный ключ для подписи токенов
    private static final String SECRET_KEY = "mySecretKey";

    // Генерация JWT токена
    /**
     * Генерирует JWT токен для указанного пользователя.
     *
     * @param username Имя пользователя
     * @return Токен
     */
    public static String generateToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000)) // Токен действует 1 час
                .sign(Algorithm.HMAC256(SECRET_KEY)); // Подпись токена
    }

    // Валидация токена
    /**
     * Проверяет, действителен ли токен.
     *
     * @param token Токен
     * @return true, если токен валиден, иначе false
     */
    public static boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(SECRET_KEY))
                    .build()
                    .verify(token); // Проверка подписи и срока действия
            return true;
        } catch (Exception e) {
            return false; // Если токен невалиден
        }
    }

    // Извлечение имени пользователя из токена
    /**
     * Извлекает имя пользователя из токена.
     *
     * @param token Токен
     * @return Имя пользователя
     */
    public static String extractUsername(String token) {
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(SECRET_KEY))
                .build()
                .verify(token); // Верификация токена
        return decodedJWT.getSubject(); // Возвращает имя пользователя
    }
}

