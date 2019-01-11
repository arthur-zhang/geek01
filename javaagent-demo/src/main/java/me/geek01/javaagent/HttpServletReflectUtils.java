package me.geek01.javaagent;

import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * Created By Arthur Zhang at 10/02/2017
 */
public class HttpServletReflectUtils {

    public static String getServletPath(Object httpServletRequest) {
        return (String) invokeIgnoreException(httpServletRequest, "getServletPath");
    }

    public static String getRequestURI(Object httpServletRequest) {
        return (String) invokeIgnoreException(httpServletRequest, "getRequestURI");
    }

    public static String getQueryString(Object httpServletRequest) {
        return (String) invokeIgnoreException(httpServletRequest, "getQueryString");
    }

    public static String getMethod(Object httpServletRequest) {
        return (String) invokeIgnoreException(httpServletRequest, "getMethod");
    }

    public static String getHeader(Object httpServletRequest, String headerName) {
        return (String) invokeIgnoreException(httpServletRequest, "getHeader", headerName);
    }

    public static String getContextPath(Object httpServletRequest) {
        return (String) invokeIgnoreException(httpServletRequest, "getContextPath");
    }

    public static String getAttribute(Object httpServletRequest, String headerName) {
        return (String) invokeIgnoreException(httpServletRequest, "getAttribute", headerName);
    }

    public static int getStatus(Object httpServletResponse) {
        return (int) invokeIgnoreException(httpServletResponse, "getStatus");
    }

    public static Object invokeIgnoreException(Object httpServletRequest, String methodName, Object... args) {
        try {
            return MethodUtils.invokeMethod(httpServletRequest, methodName, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
