package dev.stockman.retry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

@Configuration
public class RetryConfiguration {

    @Bean
    public RetryTemplate retryTemplate(
            RetryPolicy retryPolicy,
            BackOffPolicy backOffPolicy,
            RetryListener retryListener
    ) {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.registerListener(retryListener);
        retryTemplate.setThrowLastExceptionOnExhausted(true);
        return retryTemplate;
    }

    @Bean
    public RetryPolicy retryPolicy(
            @Value("${retry.maxAttempts:3}") int maxAttempts,
            @Value("${retry.retryableExceptions:}") List<String> retryableExceptions,
            @Value("${retry.nonRetryableExceptions:}") List<String> nonRetryableExceptions
    ) throws ClassNotFoundException {
        return new SimpleRetryPolicy(maxAttempts, RetryUtils.retryableExceptions(retryableExceptions, nonRetryableExceptions));
    }

    @Bean
    public BackOffPolicy backOffPolicy(
            @Value("${retry.initialInterval:100}") int initialInterval,
            @Value("${retry.multiplier:2}") int multiplier,
            @Value("${retry.maxInterval:5000}") int maxInterval
    ) {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialInterval);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxInterval);
        return backOffPolicy;
    }

    @Bean
    public RetryListener retryListener(
            RetryPolicy retryPolicy
    ) {
        return new RetryLoggerListener(retryPolicy);
    }
}
