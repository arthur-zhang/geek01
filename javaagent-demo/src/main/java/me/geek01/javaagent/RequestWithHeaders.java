package me.geek01.javaagent;


public interface RequestWithHeaders {

    
    String getHeader(String headerName);

    
    Object getAttribute(String name);
}
