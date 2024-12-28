package dev.stockman.retry;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
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

    @ParameterizedTest(name = "untilSuccess={0}, expectedAttempts={1}, expectedOutput={2}")
    @CsvSource({
            "10, 3, fallback",
            "2, 2, success"
    })
    void testRetryableExceptions(int untilSuccess, int expectedAttempts, String expectedOutput) {
        RetryService retryService = new RetryService(untilSuccess);
        String output = retryTemplate.execute(_ -> {
            retryService.incrementCounterNullPointerException();
            return "success";
        }, c -> "fallback");
        Assertions.assertThat(output).isEqualTo(expectedOutput);
        Assertions.assertThat(retryService.getCounter()).isEqualTo(expectedAttempts);
    }

    private class RetryService {
        private final int untilSuccess;
        private int counter = 0;

        private RetryService(int untilSuccess) {
            this.untilSuccess = untilSuccess;
        }

        public void incrementCounterIllegalArgumentException() {
            counter++;
            if (counter < untilSuccess) {
                throw new IllegalArgumentException("test");
            }
        }
        public void incrementCounterNullPointerException() {
            counter++;
            if (counter < untilSuccess) {
                throw new NullPointerException("test");
            }
        }
        public int getCounter() {
            return counter;
        }
    }
}
