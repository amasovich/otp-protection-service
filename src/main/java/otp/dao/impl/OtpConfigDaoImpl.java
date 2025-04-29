package otp.dao.impl;

import otp.config.DatabaseManager;
import otp.dao.OtpConfigDao;
import otp.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * JDBC-реализация OtpConfigDao.
 * Управляет единственной записью в таблице otp_config.
 */
public class OtpConfigDaoImpl implements OtpConfigDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpConfigDaoImpl.class);

    private static final String SELECT_CONFIG_SQL =
            "SELECT id, length, ttl_seconds FROM otp_config LIMIT 1";
    private static final String UPDATE_CONFIG_SQL =
            "UPDATE otp_config SET length = ?, ttl_seconds = ? WHERE id = ?";
    private static final String INSERT_DEFAULT_SQL =
            "INSERT INTO otp_config (length, ttl_seconds) VALUES (?, ?)";

    @Override
    public OtpConfig getConfig() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_CONFIG_SQL);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                OtpConfig cfg = new OtpConfig();
                cfg.setId(rs.getLong("id"));
                cfg.setLength(rs.getInt("length"));
                cfg.setTtlSeconds(rs.getInt("ttl_seconds"));
                logger.info("Loaded OTP config: {}", cfg);
                return cfg;
            }
        } catch (SQLException e) {
            logger.error("Error loading OTP config: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        logger.warn("No OTP config found in database");
        return null;
    }

    @Override
    public void updateConfig(OtpConfig config) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_CONFIG_SQL)) {

            ps.setInt(1, config.getLength());
            ps.setInt(2, config.getTtlSeconds());
            ps.setLong(3, config.getId());
            int affected = ps.executeUpdate();
            logger.info("Updated OTP config (id={}): length={}, ttlSeconds={} ({} rows)",
                    config.getId(), config.getLength(), config.getTtlSeconds(), affected);
        } catch (SQLException e) {
            logger.error("Error updating OTP config [{}]: {}", config, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initDefaultConfigIfEmpty() {
        // проверяем, есть ли запись
        OtpConfig existing = getConfig();
        if (existing != null) {
            logger.info("OTP config already initialized: {}", existing);
            return;
        }
        // вставляем дефолтные значения (6 цифр, 300 секунд)
        int defaultLength = 6;
        int defaultTtl = 300;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_DEFAULT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, defaultLength);
            ps.setInt(2, defaultTtl);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Inserting default OTP config failed, no rows affected.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long newId = keys.getLong(1);
                    logger.info("Initialized default OTP config id={} (length={}, ttlSeconds={})",
                            newId, defaultLength, defaultTtl);
                }
            }
        } catch (SQLException e) {
            logger.error("Error initializing default OTP config: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}

