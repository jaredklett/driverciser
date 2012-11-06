package org.javaforge.messaging;

import org.apache.log4j.Logger;
import org.javaforge.util.Config;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

/**
 * Lett's Law: All programs evolve until they can send e-mail.
 *
 * @author Jared Klett
 */

public class Sendmail {

// Constants //////////////////////////////////////////////////////////////////

    protected static final String X_MAILER_HEADER = "X-Mailer";
    protected static final String X_MAILER_VALUE = "Driverciser using JavaMail";

// Class variables ////////////////////////////////////////////////////////////

    /** Our logging facility. */
    private static Logger log = Logger.getLogger(Sendmail.class);
    /** TODO */
    protected static Properties sessionProps;

// Configuration //////////////////////////////////////////////////////////////

    public static final String CONFIG_NAME = "sendmail";
    public static final String PROPERTY_HOST = "smtp.host";
    public static final String PROPERTY_PORT = "smtp.port";
    public static final String PROPERTY_FROM_NAME = "from.name";
    public static final String PROPERTY_FROM_ADDR = "from.addr";

    /** TODO */
    public static String host;
    /** TODO */
    public static String port;
    /** TODO */
    public static String fromName;
    /** TODO */
    public static String fromAddr;

    /** TODO */
    public static final String DEFAULT_HOST = "localhost";
    /** TODO */
    public static final String DEFAULT_PORT = "25";
    /** TODO */
    public static final String DEFAULT_FROM_NAME = "Driverciser";
    /** TODO */
    public static final String DEFAULT_FROM_ADDR = "do-not-reply@javaforge.org";

// Class initializer   ////////////////////////////////////////////////////////

    static {
         loadConfiguration();
    }

// Configuration //////////////////////////////////////////////////////////////

    /** TODO */
    public static void loadConfiguration() {
        Config config = new Config(CONFIG_NAME);

        synchronized (Sendmail.class) {
            host = config.stringProperty(PROPERTY_HOST, DEFAULT_HOST);
            port = config.stringProperty(PROPERTY_PORT, DEFAULT_PORT);
            fromName = config.stringProperty(PROPERTY_FROM_NAME, DEFAULT_FROM_NAME);
            fromAddr = config.stringProperty(PROPERTY_FROM_ADDR, DEFAULT_FROM_ADDR);

            sessionProps = new Properties();
            sessionProps.setProperty("mail.smtp.host", host);
            //sessionProps.setProperty("mail.smtp.port", port);
        }

        log.info("Loading configuration from file: " + CONFIG_NAME);
        log.info(PROPERTY_HOST + " = " + host);
        log.info(PROPERTY_PORT + " = " + port);
        log.info(PROPERTY_FROM_NAME + " = " + fromName);
        log.info(PROPERTY_FROM_ADDR + " = " + fromAddr);
    }

// Class methods //////////////////////////////////////////////////////////////

    /**
     * TODO
     *
     * @param recipient TODO
     * @param subject TODO
     * @param text TODO
     * @return TODO
     */
    public static boolean send(String recipient, String subject, String text) {
        boolean success = false;
        try {
            Address[] recipientAddresses = new InternetAddress[] { new InternetAddress(recipient, false) };
            InternetAddress[] senderAddresses = InternetAddress.parse(recipient, false);
            Session session = Session.getInstance(sessionProps);
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddr, fromName));
            message.setRecipients(Message.RecipientType.TO, senderAddresses);
            message.setSubject(subject);
            message.setText(text);
            message.setHeader(X_MAILER_HEADER, X_MAILER_VALUE);
            message.setSentDate(new Date());
            Transport.send(message, recipientAddresses);
            success = true;
        } catch (Exception e) {
            log.error("Caught exception while attempting to send mail!", e);
        }
        return success;
    }

} // class Sendmail
