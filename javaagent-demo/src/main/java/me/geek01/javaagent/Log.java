package me.geek01.javaagent;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created By Arthur Zhang at 05/10/2016
 */
public class Log {

    public static void trace(String message) {
        if (false) System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
                + " " + Thread.currentThread().getName() + " [APM] " + message);
    }

    public static void debug(String message) {
        if (BuildConfig.DEBUG) System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
                + " " + Thread.currentThread().getName() + " [APM] " + message);
    }

    public static void info(String message) {
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
                + " " + Thread.currentThread().getName() + " [APM] " + message);
    }

    public static void info(String message, String... parameters) {
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
                + " " + Thread.currentThread().getName() + " [APM] " + String.format(message, parameters));
    }
}
