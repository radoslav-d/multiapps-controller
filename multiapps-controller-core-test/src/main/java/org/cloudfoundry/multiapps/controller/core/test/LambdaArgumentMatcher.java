package org.cloudfoundry.multiapps.controller.core.test;

import java.util.function.Predicate;

import org.mockito.ArgumentMatcher;

public class LambdaArgumentMatcher<T> implements ArgumentMatcher<T> {

    private final Predicate<Object> matcher;

    public LambdaArgumentMatcher(Predicate<Object> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(Object argument) {
        return matcher.test(argument);
    }
}