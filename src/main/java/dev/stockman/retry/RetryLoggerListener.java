package dev.stockman.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;

class RetryLoggerListener implements RetryListener {

    private static final Logger log = LoggerFactory.getLogger(RetryLoggerListener.class);

    private final RetryPolicy retryPolicy;

    RetryLoggerListener(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        boolean nonRetryable = !retryPolicy.canRetry(context) && context.getRetryCount() == 1;
        log.warn("Retry Listener: Attempt: " + context.getRetryCount() + " of " + (nonRetryable ? 1 : retryPolicy.getMaxAttempts()) + ". Exception caught: " + throwable);
        if (nonRetryable) {
            log.info("Retry Listener: Non-retryable exception caught. Will invoke fallback method. Exception caught: " + throwable);
        } else if (!retryPolicy.canRetry(context)) {
            if (context.getRetryCount() == retryPolicy.getMaxAttempts()) {
                log.info("Retry Listener: Retry limit reached. Will invoke fallback method. Exception caught: " + throwable);
            } else {
                log.info("Retry Listener: Retry chain interrupted by non-retryable exception. Will invoke fallback method. Exception caught: " + throwable);
            }
        }
    }

    public <T, E extends Throwable> void onSuccess(RetryContext context, RetryCallback<T, E> callback, T result) {
        if (context.getRetryCount() > 0) {
            log.info("Retry Listener: Attempt: " + (context.getRetryCount() + 1) + " of " + retryPolicy.getMaxAttempts() + ". Success!");
        }
    }
}
