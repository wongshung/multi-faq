package com.hackathon.ceptional.util;

import sun.misc.Unsafe;

/**
 * unsafe utils for thread safe array list
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/13
 */
public class UnsafeUtil {
    final static private Unsafe UNSAFE;
    static {
        Unsafe tmpUnsafe;

        try {
            java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            tmpUnsafe = (sun.misc.Unsafe) field.get(null);
        } catch (java.lang.Exception e) {
            throw new Error(e);
        }

        UNSAFE = tmpUnsafe;
    }
    public static Unsafe unsafe() {
        return UNSAFE;
    }
}
