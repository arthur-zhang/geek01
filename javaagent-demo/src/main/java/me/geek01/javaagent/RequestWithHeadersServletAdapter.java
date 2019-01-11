package me.geek01.javaagent;


public class RequestWithHeadersServletAdapter implements RequestWithHeaders {

    private final Object httpServletRequest;

    public RequestWithHeadersServletAdapter(Object httpServletRequest) {
        if (httpServletRequest == null)
            throw new IllegalArgumentException("httpServletRequest cannot be null");

        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public String getHeader(String headerName) {
        return HttpServletReflectUtils.getHeader(httpServletRequest, headerName);
    }

    @Override
    public Object getAttribute(String name) {
        return HttpServletReflectUtils.getAttribute(httpServletRequest, name);
    }
}
