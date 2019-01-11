package me.geek01.javaagent;

import java.util.UUID;


public class TraceAndSpanIdGenerator {

    
    private TraceAndSpanIdGenerator() {

    }
    
    public static String generateId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
