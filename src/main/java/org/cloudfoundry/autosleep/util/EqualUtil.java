package org.cloudfoundry.autosleep.util;

public class EqualUtil {

    public static boolean areEquals(Object object1, Object object2) {
        return object1 == null && object2 == null || object1 != null && object1.equals(object2);
    }
}
