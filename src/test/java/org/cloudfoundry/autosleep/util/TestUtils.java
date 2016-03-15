/*
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.autosleep.util;


import java.util.function.Function;

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
            fail("Expected exception " + wantedClass.getName() + " not thrown");
        } catch (Exception exc) {
            if (!wantedClass.isInstance(exc)) {
                throw new RuntimeException("Expected " + wantedClass.getName() + " got " + exc.getClass().getName(),
                        exc);
            }
            T expectedClass = wantedClass.cast(exc);
            for (CheckerFunction<T> checker : checkers) {
                checker.check(expectedClass);
            }
        }
    }
}
