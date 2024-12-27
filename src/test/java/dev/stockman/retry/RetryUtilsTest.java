package dev.stockman.retry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetryUtilsTest {

    /**
     * Test class for the `retryableExceptions` method in the `RetryUtils` class.
     * <p>
     * The `retryableExceptions` method takes two lists of exception class names (retryable and non-retryable),
     * converts them to class objects, and maps them to a boolean value where:
     * - true = retryable exceptions
     * - false = non-retryable exceptions
     * <p>
     * This test class ensures that the method works correctly for different scenarios.
     */

    @Test
    void testRetryableExceptionsWithValidInput() throws ClassNotFoundException {
        List<String> retryable = List.of("java.io.IOException", "java.net.SocketTimeoutException");
        List<String> nonRetryable = List.of("java.lang.IllegalArgumentException");

        Map<Class<? extends Throwable>, Boolean> result = RetryUtils.retryableExceptions(retryable, nonRetryable);

        assertEquals(3, result.size());
        assertTrue(result.get(IOException.class));
        assertTrue(result.get(java.net.SocketTimeoutException.class));
        assertFalse(result.get(IllegalArgumentException.class));
    }

    @Test
    void testRetryableExceptionsWithEmptyLists() throws ClassNotFoundException {
        List<String> retryable = List.of();
        List<String> nonRetryable = List.of();

        Map<Class<? extends Throwable>, Boolean> result = RetryUtils.retryableExceptions(retryable, nonRetryable);

        assertTrue(result.isEmpty());
    }

    @Test
    void testRetryableExceptionsWithNullLists() throws ClassNotFoundException {
        Map<Class<? extends Throwable>, Boolean> result = RetryUtils.retryableExceptions(null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testRetryableExceptionsWithInvalidClassName() {
        List<String> retryable = List.of("java.InvalidExceptionClass");

        Exception exception = assertThrows(
                ClassNotFoundException.class,
                () -> RetryUtils.retryableExceptions(retryable, null)
        );

        assertTrue(exception.getMessage().contains("java.InvalidExceptionClass"));
    }

    @Test
    void testRetryableExceptionsWithNonThrowableClass() {
        List<String> retryable = List.of("java.lang.String");

        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> RetryUtils.retryableExceptions(retryable, null)
        );

        assertTrue(exception.getMessage().contains("Class java.lang.String is not a subclass of java.lang.Throwable"));
    }

    @Test
    void testRetryableExceptionsWithDuplicates() throws ClassNotFoundException {
        List<String> retryable = List.of("java.io.IOException");
        List<String> nonRetryable = List.of("java.io.IOException");

        Map<Class<? extends Throwable>, Boolean> result = RetryUtils.retryableExceptions(retryable, nonRetryable);

        assertEquals(1, result.size());
        assertFalse(result.get(IOException.class));
    }
}