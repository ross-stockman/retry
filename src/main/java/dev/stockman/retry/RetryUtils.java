package dev.stockman.retry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RetryUtils {
    private RetryUtils() {
    }

    static Map<Class<? extends Throwable>, Boolean> retryableExceptions(List<String> retryableExceptions, List<String> nonRetryableExceptions) throws ClassNotFoundException {
        Map<Class<? extends Throwable>, Boolean> exceptions = new HashMap<>();
        throwableList(retryableExceptions).forEach(exceptionClass -> exceptions.put(exceptionClass, Boolean.TRUE));
        throwableList(nonRetryableExceptions).forEach(exceptionClass -> exceptions.put(exceptionClass, Boolean.FALSE));
        return exceptions;
    }

    private static List<Class<? extends Throwable>> throwableList(
            List<String> retryableExceptions
    ) throws ClassNotFoundException {
        List<Class<? extends Throwable>> exceptionClasses = new ArrayList<>();
        if (retryableExceptions != null) {
            for (String exceptionClassName : retryableExceptions) {
                Class<?> clazz = Class.forName(exceptionClassName);
                if (Throwable.class.isAssignableFrom(clazz)) {
                    exceptionClasses.add(clazz.asSubclass(Throwable.class));
                } else {
                    throw new IllegalArgumentException(
                            "Class " + exceptionClassName + " is not a subclass of java.lang.Throwable");
                }
            }
        }
        return exceptionClasses;
    }
}
