package org.cloudfoundry.autosleep.util;


import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

public class TestUtils {

    public interface MethodExecutor {
        void execute() throws Exception;
    }

    public interface ExceptionChecker<T extends Throwable> {
        void check(T exceptionThrown);
    }

    @SafeVarargs
    public static <T extends Throwable> void verifyThrown(MethodExecutor executor, Class<T> wantedClass,
                                                          ExceptionChecker<T>... checkers) {
        try {
            executor.execute();
            fail();
        } catch (Exception exc) {
            assertThat(exc, is(instanceOf(wantedClass)));
            T expectedClass = wantedClass.cast(exc);
            for (ExceptionChecker<T> checker : checkers) {
                checker.check(expectedClass);
            }
        }
    }
}
