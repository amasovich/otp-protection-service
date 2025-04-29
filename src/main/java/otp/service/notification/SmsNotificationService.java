package otp.service.notification;

import org.opensmpp.Connection;
import org.opensmpp.Session;
import org.opensmpp.pdu.BindResponse;
import org.opensmpp.pdu.BindTransmitter;
import org.opensmpp.pdu.SubmitSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Реализация NotificationService для отправки OTP-кодов по SMS
 * через эмулятор SMPP (например, SMPPsim).
 * Конфигурация берётся из файла sms.properties в resources.
 */
public class SmsNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddr;

    /**
     * Конструктор загружает настройки SMPP из sms.properties.
     */
    public SmsNotificationService() {
        Properties props = loadConfig();
        this.host = props.getProperty("smpp.host");
        this.port = Integer.parseInt(props.getProperty("smpp.port"));
        this.systemId = props.getProperty("smpp.system_id");
        this.password = props.getProperty("smpp.password");
        this.systemType = props.getProperty("smpp.system_type");
        this.sourceAddr = props.getProperty("smpp.source_addr");
    }

    private Properties loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sms.properties")) {
            if (is == null) {
                throw new IllegalStateException("sms.properties not found in classpath");
            }
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            logger.error("Failed to load sms.properties", e);
            throw new RuntimeException("Could not load SMS configuration", e);
        }
    }

    /**
     * Отправляет SMS с кодом на указанный номер телефона.
     *
     * @param recipientPhone номер телефона получателя (в формате E.164 или близком)
     * @param code           OTP-код для отправки
     */
    @Override
    public void sendCode(String recipientPhone, String code) {
        Connection connection = null;
        Session session = null;
        try {
            connection = new Connection(host, port);
            session = new Session(connection);

            // 1. Подготовка BindTransmitter
            BindTransmitter bindRequest = new BindTransmitter();
            bindRequest.setSystemId(systemId);
            bindRequest.setPassword(password);
            bindRequest.setSystemType(systemType);
            bindRequest.setInterfaceVersion((byte) 0x34); // SMPP v3.4
            bindRequest.setAddressRange(sourceAddr);

            // 2. Привязка
            BindResponse bindResponse = session.bind(bindRequest);
            if (bindResponse.getCommandStatus() != 0) {
                throw new RuntimeException("SMPP bind failed with status: " + bindResponse.getCommandStatus());
            }

            // 3. Отправка сообщения
            SubmitSM submit = new SubmitSM();
            submit.setSourceAddr(sourceAddr);
            submit.setDestAddr(recipientPhone);
            submit.setShortMessage("Your OTP code: " + code);

            session.submit(submit);
            logger.info("OTP code sent via SMS to {}", recipientPhone);
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}", recipientPhone, e);
            throw new RuntimeException("SMS sending failed", e);
        } finally {
            if (session != null) {
                try {
                    session.unbind();
                } catch (Exception ignored) {}
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException ignored) {}
            }
        }
    }
}

