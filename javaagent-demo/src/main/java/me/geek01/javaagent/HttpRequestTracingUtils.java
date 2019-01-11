package me.geek01.javaagent;

public class HttpRequestTracingUtils {


    public static final String UNSPECIFIED_SPAN_NAME = "UNSPECIFIED";

    private HttpRequestTracingUtils() {
        // Nothing to do
    }

    public static Span fromRequestWithHeaders(RequestWithHeaders request, String appId) {
        if (request == null)
            return null;

        String traceId = getTraceId(request);
        if (traceId == null)
            return null;

        String spanName = getHeaderWithAttributeAsBackup(request, TraceHeaders.SPAN_NAME);
        if (spanName == null || spanName.length() == 0)
            spanName = UNSPECIFIED_SPAN_NAME;

        return Span.newBuilder(spanName, Span.SpanType.HTTP_REQUEST)
                .withAppId(appId)
                   .withTraceId(traceId)
                   .withParentSpanId(getSpanIdFromRequest(request, TraceHeaders.PARENT_SPAN_ID, false))
                   .withSpanId(getSpanIdFromRequest(request, TraceHeaders.SPAN_ID, true))
                   .withSampleable(getSpanSampleableFlag(request))
                   .build();
    }

    protected static boolean getSpanSampleableFlag(RequestWithHeaders request) {
        String spanSampleableHeaderStr = getHeaderWithAttributeAsBackup(request, TraceHeaders.TRACE_SAMPLED);
        // Default to true (enabling trace sampling for requests that don't explicitly exclude it)
        boolean result = true;

        if ("0".equals(spanSampleableHeaderStr) || "false".equalsIgnoreCase(spanSampleableHeaderStr))
            result = false;

        return result;
    }

    
    protected static String getSpanIdFromRequest(RequestWithHeaders request, String headerName, boolean generateNewSpanIdIfNotFoundInRequest) {
        String spanIdString = getHeaderWithAttributeAsBackup(request, headerName);
        if (spanIdString == null)
            return generateNewSpanIdIfNotFoundInRequest ? TraceAndSpanIdGenerator.generateId() : null;

        return spanIdString;
    }

    protected static String getTraceId(RequestWithHeaders request) {
        String requestTraceId = getHeaderWithAttributeAsBackup(request, TraceHeaders.TRACE_ID);
        return requestTraceId;
    }

    protected static String getHeaderWithAttributeAsBackup(RequestWithHeaders request, String headerName) {
        Object result = request.getHeader(headerName);

        if (result == null || result.toString().trim().length() == 0)
            result = request.getAttribute(headerName);

        return (result == null) ? null : result.toString().trim();
    }
}
