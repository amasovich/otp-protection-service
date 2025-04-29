package otp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Планировщик, который по расписанию помечает просроченные OTP-коды как EXPIRED.
 */
public class OtpExpirationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OtpExpirationScheduler.class);

    private final OtpService otpService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Интервал в минутах между запусками */
    private final long intervalMinutes;

    public OtpExpirationScheduler(OtpService otpService, long intervalMinutes) {
        this.otpService = otpService;
        this.intervalMinutes = intervalMinutes;
    }

    /**
     * Запускает планировщик.
     * По расписанию будет вызываться метод run().
     */
    public void start() {
        logger.info("Starting OTP-expiration scheduler, interval={} min", intervalMinutes);
        scheduler.scheduleAtFixedRate(
                this::run,           // явно вызываем наш метод run()
                intervalMinutes,     // initial delay
                intervalMinutes,     // period
                TimeUnit.MINUTES
        );
    }

    /**
     * Однократный прогон: помечает все просроченные OTP как EXPIRED.
     */
    public void run() {
        try {
            otpService.markExpiredOtps();
            logger.debug("OtpExpirationScheduler run(): expired codes processed");
        } catch (Exception e) {
            logger.error("Error in OTP-expiration task", e);
        }
    }

    /** Останавливает планировщик */
    public void stop() {
        logger.info("Stopping OTP-expiration scheduler");
        scheduler.shutdownNow();
    }
}

