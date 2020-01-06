package com.ass;

public class BaseAssert {
    public static AssertUtil UTIL = new AssertUtil();

    public static void isNotNull(Object o) {
        UTIL.isNotNull(o);
    }

    public static void isNotNull(Object o, String msg) {
        UTIL.isNotNull(o, msg);
    }

    public static void isNull(Object o) {
        UTIL.isNull(o);
    }

    public static void isNull(Object o, String msg) {
        UTIL.isNull(o, msg);
    }

    public static void isTrue(boolean b) {
        UTIL.isTrue(b);
    }

    public static void isTrue(boolean b, String msg) {
        UTIL.isTrue(b, msg);
    }

    public static void isFalse(boolean b) {
        UTIL.isFalse(b);
    }

    public static void isFalse(boolean b, String msg) {
        UTIL.isFalse(b, msg);
    }

    public static void hasText(CharSequence str) {
        UTIL.hasText(str);
    }

    public static void hasText(CharSequence str, String msg) {
        UTIL.hasText(str, msg);
    }

    public static void isNotEmpty(Iterable<?> iteratable) {
        UTIL.isNotEmpty(iteratable);
    }

    public static void isNotEmpty(Iterable<?> iteratable, String msg) {
        UTIL.isNotNull(iteratable, msg);
        UTIL.isNotNull(iteratable.iterator().next(), msg);
    }

    public static void isEqual(Object expected, Object actual) {
        UTIL.isEqual(expected, actual);
    }

    public static void isEqual(Object expected, Object actual, String msg) {
        UTIL.isEqual(expected, actual, msg);
    }

    public static boolean testEqual(Object expected, Object actual) {
        return UTIL.testEqual(expected, actual);
    }

}
