package otp.util;

import otp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Утиль для генерации, хранения и валидации токенов авторизации.
 */
public final class TokenManager {
    private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);

    /** Хранилище: токен → информация о пользователе и времени истечения */
    private static final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    /** Время жизни токена в минутах */
    private static final long TTL_MINUTES = 30;

    private TokenManager() { /* запрет создания экземпляров */ }

    /**
     * Генерирует новый токен для пользователя и сохраняет его в памяти.
     * @param user объект пользователя
     * @return строковое представление токена
     */
    public static String generateToken(User user) {
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plus(TTL_MINUTES, ChronoUnit.MINUTES);
        tokens.put(token, new TokenInfo(user, expiry));
        logger.info("Generated token {} for user {} (expires at {})", token, user.getUsername(), expiry);
        return token;
    }

    /**
     * Проверяет, что токен присутствует и не просрочен.
     * @param token строка токена
     * @return true, если токен валиден
     */
    public static boolean validate(String token) {
        TokenInfo info = tokens.get(token);
        if (info == null) {
            logger.warn("Token validation failed: token not found");
            return false;
        }
        if (Instant.now().isAfter(info.expiry)) {
            tokens.remove(token);
            logger.warn("Token {} expired at {}, removed from store", token, info.expiry);
            return false;
        }
        return true;
    }

    /**
     * Возвращает пользователя, ассоциированного с токеном.
     * @param token валидный токен
     * @return объект User или null, если токен некорректен/просрочен
     */
    public static User getUser(String token) {
        if (!validate(token)) {
            return null;
        }
        return tokens.get(token).user;
    }

    /**
     * Отозвать (удалить) токен досрочно.
     * @param token строка токена
     */
    public static void revoke(String token) {
        if (tokens.remove(token) != null) {
            logger.info("Token {} revoked", token);
        }
    }

    /** Внутренний класс для хранения информации о токене */
    private static class TokenInfo {
        final User user;
        final Instant expiry;

        TokenInfo(User user, Instant expiry) {
            this.user = user;
            this.expiry = expiry;
        }
    }
}
