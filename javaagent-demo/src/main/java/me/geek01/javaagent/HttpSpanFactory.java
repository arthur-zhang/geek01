package me.geek01.javaagent;


/**
 *
 */
public class HttpSpanFactory {

    /**
     * Intentionally private to force all access through static methods.
     */
    private HttpSpanFactory() {
        // Nothing to do
    }

    public static Span fromHttpServletRequest(Object servletRequest, String appId) {
        if (servletRequest == null)
            return null;

        return HttpRequestTracingUtils.fromRequestWithHeaders(new RequestWithHeadersServletAdapter(servletRequest), appId);
    }

    /**
     *
     */
    public static Span fromHttpServletRequestOrCreateRootSpan(Object servletRequest, String appId) {
        Span span = fromHttpServletRequest(servletRequest, appId);

        if (span == null) {
            span = Span
                           .generateRootSpanForNewTrace(getSpanName(servletRequest), appId, Span.SpanType.HTTP_REQUEST)
                           .build();
        }

        return span;
    }

    /**
     * Attempts to pull a valid ID for the user making the request.
     *
     * @return The HTTP Header value of the user ID if it exists, null otherwise. The request's headers will be inspected for the user ID using the given list of userIdHeaderKeys
     *          in list order - the first one found that is not null/empty will be returned.
     */

    /**
     * @return Span name appropriate for a new root span for this request
     */
    public static String getSpanName(Object request) {
        // Try the servlet path first, and fall back to the raw request URI.

        String path = HttpServletReflectUtils.getServletPath(request);
        if (path == null || path.trim().length() == 0)
            path = HttpServletReflectUtils.getRequestURI(request);

        // Include the HTTP method in the returned value to help delineate which endpoint this request represents.
        return HttpServletReflectUtils.getMethod(request) + '_' + path;
    }

    public static String getServletSpanName(Object request) {
        // Try the servlet path first, and fall back to the raw request URI.

        String path = HttpServletReflectUtils.getServletPath(request);
        if (path == null || path.trim().length() == 0)
            path = HttpServletReflectUtils.getRequestURI(request);

        // Include the HTTP method in the returned value to help delineate which endpoint this request represents.
        return HttpServletReflectUtils.getMethod(request) + ' ' + path;
    }

}
