package org.cloudfoundry.autosleep.util;

public class EqualUtil {

    public static <T> boolean areEquals(T object1, T object2) {
        return object1 == null && object2 == null || object1 != null && object1.equals(object2);
    }
}
