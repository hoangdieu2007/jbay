package a88.jbay.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger wrapper using SLF4J with Logback for jBay auction system.
 */
public class JBayLogger {
    private static final Logger logger = LoggerFactory.getLogger(JBayLogger.class);

    private JBayLogger() {}

    public static synchronized JBayLogger getInstance() {
        return new JBayLogger();
    }

    public void info(String message) {
        logger.info(message);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public void debug(String message, Throwable throwable) {
        logger.debug(message, throwable);
    }

    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }
}
