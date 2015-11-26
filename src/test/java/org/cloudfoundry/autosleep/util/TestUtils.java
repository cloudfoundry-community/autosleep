package org.cloudfoundry.autosleep.util;


import java.util.function.Function;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

public class TestUtils {

    @FunctionalInterface
    public interface ThrowingFunction extends Function<Void, Void> {

        @Override
        default Void apply(Void notUsed) {
            try {
                applyThrows();
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void applyThrows() throws Exception;
    }

    @FunctionalInterface
    public interface CheckerFunction<T> extends Function<T, Void> {

        @Override
        default Void apply(T parameterChecked) {
            try {
                check(parameterChecked);
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void check(T parameterChecked);
    }

    @SafeVarargs
    public static <T extends Throwable> void verifyThrown(ThrowingFunction executor, Class<T> wantedClass,
                                                          CheckerFunction<T>... checkers) {
        try {
            executor.applyThrows();
            fail();
        } catch (Exception exc) {
            assertThat(exc, is(instanceOf(wantedClass)));
            T expectedClass = wantedClass.cast(exc);
            for (CheckerFunction<T> checker : checkers) {
                checker.check(expectedClass);
            }
        }
    }
}
