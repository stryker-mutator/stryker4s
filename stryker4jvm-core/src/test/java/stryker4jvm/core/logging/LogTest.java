package stryker4jvm.core.logging;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


public class LogTest {

    @Test
    public void testDebug() {
        BufferedLogger logger = new BufferedLogger();

        logger.debug("Debug message");

        assertTrue(logger.containsAnywhere(LogLevel.Debug.toString()));
        assertTrue(logger.containsAnywhere("Debug message"));
        logger.clear();
        assertFalse(logger.containsAnywhere(LogLevel.Debug.toString()));
        assertFalse(logger.containsAnywhere("Debug message"));

        logger.debug(generateException("Exception in debug"));
        assertTrue(logger.containsAnywhere(LogLevel.Debug.toString()));
        assertTrue(logger.containsAnywhere("Exception in debug"));
        logger.clear();
        assertTrue(logger.isEmpty());

        logger.debug("Exception with message", generateException("Exception in debug with message"));
        assertTrue(logger.containsAnywhere(LogLevel.Debug.toString()));
        assertTrue(logger.containsAnywhere("Exception with message"));
        assertTrue(logger.containsAnywhere("Exception in debug with message"));
        logger.clear();
        assertTrue(logger.isEmpty());
    }

    @Test
    public void testInfo() {
        BufferedLogger logger = new BufferedLogger();

        logger.info("Info message");
        assertTrue(logger.containsAnywhere(LogLevel.Info.toString()));
        assertTrue(logger.containsAnywhere("Info message"));
        logger.clear();
        assertFalse(logger.containsAnywhere(LogLevel.Info.toString()));
        assertFalse(logger.containsAnywhere("Info message"));

        logger.info(generateException("Exception in Info"));
        assertTrue(logger.containsAnywhere(LogLevel.Info.toString()));
        assertTrue(logger.containsAnywhere("Exception in Info"));
        logger.clear();
        assertTrue(logger.isEmpty());

        logger.info("Exception with message", generateException("Exception in Info with message"));
        assertTrue(logger.containsAnywhere(LogLevel.Info.toString()));
        assertTrue(logger.containsAnywhere("Exception with message"));
        assertTrue(logger.containsAnywhere("Exception in Info with message"));
        logger.clear();
        assertTrue(logger.isEmpty());
    }

    @Test
    public void testWarn() {
        BufferedLogger logger = new BufferedLogger();

        logger.warn("Warn message");
        assertTrue(logger.containsAnywhere(LogLevel.Warn.toString()));
        assertTrue(logger.containsAnywhere("Warn message"));
        logger.clear();
        assertFalse(logger.containsAnywhere(LogLevel.Warn.toString()));
        assertFalse(logger.containsAnywhere("Warn message"));

        logger.warn(generateException("Exception in Warn"));
        assertTrue(logger.containsAnywhere(LogLevel.Warn.toString()));
        assertTrue(logger.containsAnywhere("Exception in Warn"));
        logger.clear();
        assertTrue(logger.isEmpty());

        logger.warn("Exception with message", generateException("Exception in Warn with message"));
        assertTrue(logger.containsAnywhere(LogLevel.Warn.toString()));
        assertTrue(logger.containsAnywhere("Exception with message"));
        assertTrue(logger.containsAnywhere("Exception in Warn with message"));
        logger.clear();
        assertTrue(logger.isEmpty());
    }

    @Test
    public void testError() {
        BufferedLogger logger = new BufferedLogger();

        logger.error("Error message");
        assertTrue(logger.containsAnywhere(LogLevel.Error.toString()));
        assertTrue(logger.containsAnywhere("Error message"));
        logger.clear();
        assertFalse(logger.containsAnywhere(LogLevel.Error.toString()));
        assertFalse(logger.containsAnywhere("Error message"));

        logger.error(generateException("Exception in Error"));
        assertTrue(logger.containsAnywhere(LogLevel.Error.toString()));
        assertTrue(logger.containsAnywhere("Exception in Error"));
        logger.clear();
        assertTrue(logger.isEmpty());

        logger.error("Exception with message", generateException("Exception in Error with message"));
        assertTrue(logger.containsAnywhere(LogLevel.Error.toString()));
        assertTrue(logger.containsAnywhere("Exception with message"));
        assertTrue(logger.containsAnywhere("Exception in Error with message"));
        logger.clear();
        assertTrue(logger.isEmpty());
    }

    @Test
    public void testColorEnabled() {
        BufferedLogger logger = new BufferedLogger(false);
        assertFalse(logger.isColorEnabled());
        assertFalse(logger.colorEnabled);

        logger = new BufferedLogger(true);
        assertTrue(logger.isColorEnabled());
        assertTrue(logger.colorEnabled);
    }

    private Throwable generateException(String message) {
        try {
            throw new RuntimeException(message);
        } catch (Exception e) {
            return e;
        }
    }
}
