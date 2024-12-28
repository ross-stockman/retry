package dev.stockman.retry;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.ArrayList;
import java.util.List;

@SpringJUnitConfig(classes = RetryConfiguration.class)
@TestPropertySource(properties = {
        "retry.maxAttempts=3",
        "retry.initialInterval=50",
        "retry.multiplier=2",
        "retry.maxInterval=1000",
        "retry.retryableExceptions=java.lang.RuntimeException",
        "retry.nonRetryableExceptions=java.lang.IllegalArgumentException"
})
public class RetryConfigurationTest {

    @Autowired
    private RetryTemplate retryTemplate;

    private RetryableService retryableService = Mockito.mock(RetryableService.class);

    private TestLogAppender logAppender;

    @BeforeEach
    void setup() {
        // Get the logger for the tested class
        Logger logger = (Logger) LoggerFactory.getLogger(RetryLoggerListener.class);

        // Setup the test log appender
        logAppender = new TestLogAppender();
        logAppender.start();

        // Attach the appender to the logger
        logger.addAppender(logAppender);
    }

    @Test
    void testSuccess() {
        // Arrange
        Mockito.when(retryableService.test()).thenReturn("success");

        // Act
        String output = retryTemplate.execute(_ -> retryableService.test(), c -> "fallback");

        // Assert
        Assertions.assertThat(output).isEqualTo("success");
        Mockito.verify(retryableService, Mockito.times(1)).test();

        // validate the retry listener logs
        Assertions.assertThat(logAppender.getEvents()).isEmpty();
    }

    @Test
    void nonRetryableException() {
        // Arrange
        Mockito.when(retryableService.test()).thenThrow(new IllegalArgumentException("Non-retryable exception"));

        // Act
        String output = retryTemplate.execute(_ -> retryableService.test(), c -> "fallback");

        // Assert
        Assertions.assertThat(output).isEqualTo("fallback");
        Mockito.verify(retryableService, Mockito.times(1)).test();

        // validate the retry listener logs
        var logs = logAppender.getEvents();
        Assertions.assertThat(logs.size()).isEqualTo(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(logs.get(0).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 1 of 1. Exception caught: java.lang.IllegalArgumentException: Non-retryable exception");
            softly.assertThat(logs.get(1).getFormattedMessage()).isEqualTo("Retry Listener: Non-retryable exception caught. Will invoke fallback method. Exception caught: java.lang.IllegalArgumentException: Non-retryable exception");
        });
    }

    @Test
    void retryableException() {
        // Arrange
        Mockito.when(retryableService.test()).thenThrow(new RuntimeException("Retryable exception"));

        // Act
        String output = retryTemplate.execute(_ -> retryableService.test(), c -> "fallback");
        Assertions.assertThat(output).isEqualTo("fallback");
        Mockito.verify(retryableService, Mockito.times(3)).test();

        // validate the retry listener logs
        var logs = logAppender.getEvents();
        Assertions.assertThat(logs.size()).isEqualTo(4);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(logs.get(0).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 1 of 3. Exception caught: java.lang.RuntimeException: Retryable exception");
            softly.assertThat(logs.get(1).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 2 of 3. Exception caught: java.lang.RuntimeException: Retryable exception");
            softly.assertThat(logs.get(2).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 3 of 3. Exception caught: java.lang.RuntimeException: Retryable exception");
            softly.assertThat(logs.get(3).getFormattedMessage()).isEqualTo("Retry Listener: Retry limit reached. Will invoke fallback method. Exception caught: java.lang.RuntimeException: Retryable exception");
        });
    }

    @Test
    void testRetryThenSuccess() {
        // Arrange
        Mockito.when(retryableService.test()).thenThrow(new RuntimeException("Retryable exception")).thenReturn("success");

        // Act
        String output = retryTemplate.execute(_ -> retryableService.test(), c -> "fallback");

        // Assert
        Assertions.assertThat(output).isEqualTo("success");
        Mockito.verify(retryableService, Mockito.times(2)).test();

        // validate the retry listener logs
        var logs = logAppender.getEvents();
        Assertions.assertThat(logs.size()).isEqualTo(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(logs.get(0).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 1 of 3. Exception caught: java.lang.RuntimeException: Retryable exception");
            softly.assertThat(logs.get(1).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 2 of 3. Success!");
        });
    }

    @Test
    void testRetryTwiceThenSuccess() {
        // Arrange
        Mockito.when(retryableService.test()).thenThrow(new RuntimeException("Retryable exception")).thenThrow(new RuntimeException("Retryable exception")).thenReturn("success");

        // Act
        String output = retryTemplate.execute(_ -> retryableService.test(), c -> "fallback");

        // Assert
        Assertions.assertThat(output).isEqualTo("success");
        Mockito.verify(retryableService, Mockito.times(3)).test();

        // validate the retry listener logs
        var logs = logAppender.getEvents();
        Assertions.assertThat(logs.size()).isEqualTo(3);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(logs.get(0).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 1 of 3. Exception caught: java.lang.RuntimeException: Retryable exception");
            softly.assertThat(logs.get(1).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 2 of 3. Exception caught: java.lang.RuntimeException: Retryable exception");
            softly.assertThat(logs.get(2).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 3 of 3. Success!");
        });
    }

    @Test
    void testRetryThenNonRetryable() {
        // Arrange
        Mockito.when(retryableService.test()).thenThrow(new RuntimeException("Retryable exception")).thenThrow(new IllegalArgumentException("Non-retryable exception"));

        // Act
        String output = retryTemplate.execute(_ -> retryableService.test(), c -> "fallback");

        // Assert
        Assertions.assertThat(output).isEqualTo("fallback");
        Mockito.verify(retryableService, Mockito.times(2)).test();

        // validate the retry listener logs
        var logs = logAppender.getEvents();
        Assertions.assertThat(logs.size()).isEqualTo(3);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(logs.get(0).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 1 of 3. Exception caught: java.lang.RuntimeException: Retryable exception");
            softly.assertThat(logs.get(1).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 2 of 3. Exception caught: java.lang.IllegalArgumentException: Non-retryable exception");
            softly.assertThat(logs.get(2).getFormattedMessage()).isEqualTo("Retry Listener: Retry chain interrupted by non-retryable exception. Will invoke fallback method. Exception caught: java.lang.IllegalArgumentException: Non-retryable exception");
        });
    }

    @Test
    void testRetryTwiceThenNonRetryable() {
        // Arrange
        Mockito.when(retryableService.test()).thenThrow(new RuntimeException("Retryable exception")).thenThrow(new RuntimeException("Retryable exception")).thenThrow(new IllegalArgumentException("Non-retryable exception"));

        // Act
        String output = retryTemplate.execute(_ -> retryableService.test(), c -> "fallback");

        // Assert
        Assertions.assertThat(output).isEqualTo("fallback");
        Mockito.verify(retryableService, Mockito.times(3)).test();

        // validate the retry listener logs
        var logs = logAppender.getEvents();
        Assertions.assertThat(logs.size()).isEqualTo(4);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(logs.get(0).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 1 of 3. Exception caught: java.lang.RuntimeException: Retryable exception");
            softly.assertThat(logs.get(1).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 2 of 3. Exception caught: java.lang.RuntimeException: Retryable exception");
            softly.assertThat(logs.get(2).getFormattedMessage()).isEqualTo("Retry Listener: Attempt: 3 of 3. Exception caught: java.lang.IllegalArgumentException: Non-retryable exception");
            softly.assertThat(logs.get(3).getFormattedMessage()).isEqualTo("Retry Listener: Retry limit reached. Will invoke fallback method. Exception caught: java.lang.IllegalArgumentException: Non-retryable exception");
        });
    }

    private abstract class RetryableService {
        public abstract String test();
    }

    // Custom appender to capture log events
    private static class TestLogAppender extends AppenderBase<ILoggingEvent> {

        private final List<ILoggingEvent> events = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }

        public List<ILoggingEvent> getEvents() {
            return events;
        }
    }
}
