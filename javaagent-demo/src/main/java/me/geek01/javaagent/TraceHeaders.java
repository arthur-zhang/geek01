package me.geek01.javaagent;

public interface TraceHeaders {

    
    String TRACE_ID = "X-APM-TraceId";

    
    String SPAN_ID = "X-APM-SpanId";

    
    String PARENT_SPAN_ID = "X-APM-ParentSpanId";

    
    String SPAN_NAME = "X-APM-SpanName";

    
    String TRACE_SAMPLED = "X-APM-Sampled";

}
