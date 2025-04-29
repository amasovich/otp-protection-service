package otp.service.notification;

import org.smpp.TCPIPConnection;
import org.smpp.Session;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.SubmitSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Реализация NotificationService для отправки OTP-кодов по SMS
 * через эмулятор SMPP.
 */
public class SmsNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddr;

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
            if (is == null) throw new IllegalStateException("sms.properties not found");
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            logger.error("Failed to load sms.properties", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendCode(String recipientPhone, String code) {
        TCPIPConnection connection = null;
        Session session = null;
        try {
            connection = new TCPIPConnection(host, port);
            session = new Session(connection);

            BindTransmitter bindReq = new BindTransmitter();
            bindReq.setSystemId(systemId);
            bindReq.setPassword(password);
            bindReq.setSystemType(systemType);
            bindReq.setInterfaceVersion((byte) 0x34);
            bindReq.setAddressRange(sourceAddr);

            BindResponse bindResp = session.bind(bindReq);
            if (bindResp.getCommandStatus() != 0) {
                throw new RuntimeException("SMPP bind failed: " + bindResp.getCommandStatus());
            }

            SubmitSM submit = new SubmitSM();
            submit.setSourceAddr(sourceAddr);
            submit.setDestAddr(recipientPhone);
            submit.setShortMessage("Your OTP code: " + code);
            session.submit(submit);

            logger.info("OTP sent via SMS to {}", recipientPhone);
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}", recipientPhone, e);
            throw new RuntimeException(e);
        } finally {
            if (session != null) try { session.unbind(); } catch (Exception ignored) {}
            if (connection != null) try { connection.close(); } catch (IOException ignored) {}
        }
    }
}