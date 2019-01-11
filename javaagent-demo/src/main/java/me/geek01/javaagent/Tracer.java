package me.geek01.javaagent;


import java.util.*;

public class Tracer {



    private static final ThreadLocal<Deque<Span>> currentSpanStackThreadLocal = new ThreadLocal<>();

    
    private static final Tracer INSTANCE = new Tracer();

    
    private RootSpanSamplingStrategy rootSpanSamplingStrategy = new SampleAllTheThingsStrategy();

    
    private final List<SpanLifecycleListener> spanLifecycleListeners = new ArrayList<>();
    

    private Tracer() { }

    
    public static Tracer getInstance() {
        return INSTANCE;
    }

    
    public Span getCurrentSpan() {
        Deque<Span> spanStack = currentSpanStackThreadLocal.get();
        if (spanStack == null || spanStack.isEmpty()) {
            spanStack = currentSpanStackThreadLocal.get();
        }

        return (spanStack == null) ? null : spanStack.peek();
    }

    
    public Span startRequestWithRootSpan(String spanName, String appId) {
        boolean sampleable = isNextRootSpanSampleable();
        String traceId = TraceAndSpanIdGenerator.generateId();
        return doNewRequestSpan(appId, traceId, null, spanName, sampleable, Span.SpanType.HTTP_REQUEST);
    }
    
    public Span startRootSpan(String spanName, String appId, Span.SpanType spanType) {
        boolean sampleable = isNextRootSpanSampleable();
        String traceId = TraceAndSpanIdGenerator.generateId();
        return doNewRequestSpan(appId, traceId, null, spanName, sampleable, spanType);
    }

    
    public Span startRequestWithChildSpan(String appId, Span parentSpan, String childSpanName) {
        if (parentSpan == null) {
            throw new IllegalArgumentException("parentSpan cannot be null. " +
                            "If you don't have a parent span then you should call one of the startRequestWithRootSpan(...) methods instead.");
        }

        return startRequestWithSpanInfo(appId, parentSpan.getTraceId(), parentSpan.getSpanId(), childSpanName, parentSpan.sampleable(),
                                        Span.SpanType.HTTP_REQUEST);
    }
    
    public Span startWithChildSpan(String appId, Span parentSpan, String childSpanName, Span.SpanType spanType) {
        if (parentSpan == null) {
            throw new IllegalArgumentException("parentSpan cannot be null. " +
                    "If you don't have a parent span then you should call one of the startRequestWithRootSpan(...) methods instead.");
        }
        
        return startRequestWithSpanInfo(appId, parentSpan.getTraceId(), parentSpan.getSpanId(), childSpanName, parentSpan.sampleable(),
                spanType
        );
    }
    
    public Span startRequestWithSpanInfo(String appId, String traceId, String parentSpanId, String newSpanName, boolean sampleable,  Span.SpanType spanType) {
        return doNewRequestSpan(appId, traceId, parentSpanId, newSpanName, sampleable, spanType);
    }

    
    public Span startSubSpan(String spanName, String appId, Span.SpanType spanType) {
        
        Span parentSpan = getCurrentSpan();
        if (parentSpan == null) {
            System.err.println("[" + Thread.currentThread().getName() + "]" + spanName + "\t"  + appId + "\t" + spanType);
        }

        Span childSpan = (parentSpan != null)
                ? parentSpan.generateChildSpan(spanName, appId, spanType)
                : Span.generateRootSpanForNewTrace(spanName, appId, spanType).withSampleable(isNextRootSpanSampleable()).build();

        pushSpanOntoCurrentSpanStack(childSpan);

        notifySpanStarted(childSpan);
        notifyIfSpanSampled(childSpan);

        return childSpan;
    }

    
    protected Span doNewRequestSpan(String appId, String traceId, String parentSpanId, String newSpanName, boolean sampleable,  Span.SpanType spanType) {
        if (newSpanName == null)
            throw new IllegalArgumentException("spanName cannot be null");
    
     
        if (traceId == null)
            traceId = TraceAndSpanIdGenerator.generateId();
        String spanId;
        spanId = TraceAndSpanIdGenerator.generateId();
        Integer durationNanos = null;
        Long spanStartTimeEpochMicros = System.currentTimeMillis();
        Long spanStartTimeNanos = System.nanoTime();
    
        Span span = new Span(appId, traceId, parentSpanId, spanId, newSpanName, sampleable,
                 spanType, spanStartTimeEpochMicros, spanStartTimeNanos, durationNanos);
        
        span.setTraceId(traceId);
        span.setParentSpanId(parentSpanId);
        span.setSpanType(spanType);
        span.setRootType(1);

        startNewSpanStack(span);

        notifySpanStarted(span);
        notifyIfSpanSampled(span);

        return span;
    }

    
    protected void startNewSpanStack(Span firstEntry) {
        Deque<Span> existingStack = currentSpanStackThreadLocal.get();
        if (existingStack != null && !existingStack.isEmpty()) {
            boolean first = true;
            StringBuilder lostTraceIds = new StringBuilder();
            for (Span span : existingStack) {
                if (!first)
                    lostTraceIds.append(',');
                lostTraceIds.append(span.getTraceId());
                first = false;
            }
            System.err.println(
                    "[" + Thread.currentThread().getName() + "]" + "USAGE ERROR existing stack size: "
                            +  existingStack.size() + " lost_trace_ids=" + lostTraceIds.toString() + "\t" + firstEntry
            );
        }

        currentSpanStackThreadLocal.set(new LinkedList<Span>());
        pushSpanOntoCurrentSpanStack(firstEntry);
    }


    
    protected void pushSpanOntoCurrentSpanStack(Span pushMe) {
        Deque<Span> currentStack = currentSpanStackThreadLocal.get();
        if (currentStack == null) {
            currentStack = new LinkedList<>();
            currentSpanStackThreadLocal.set(currentStack);
        }
        currentStack.push(pushMe);
    }

    public void completeRequestSpan(Throwable uncaughtException, Map<String, Object> map) {
        Log.info("completeRequestSpan: " + map);
        Deque<Span> currentSpanStack = currentSpanStackThreadLocal.get();
        if (currentSpanStack != null) {
            // Keep track of data as we go in case we need to output an error (we should only have 1 span in the stack)
            int originalSize = currentSpanStack.size();
            StringBuilder badTraceIds = new StringBuilder();

            while (!currentSpanStack.isEmpty()) {
                // Get the next span on the stack.
                Span span = currentSpanStack.pop();
                span.setUncaughtException(uncaughtException);
                for (Map.Entry<String, Object> item : map.entrySet()) {
                    span.addExtraData(item.getKey(), item.getValue());
                }

                // Check if it's a "bad" span (i.e. not the last).
                boolean isBadSpan = false;
                if (!currentSpanStack.isEmpty()) {
                    // There's still at least one more span, so this one is "bad".
                    isBadSpan = true;
                    if (badTraceIds.length() > 0)
                        badTraceIds.append(',');
                    badTraceIds.append(span.getTraceId());
                }

                completeAndLogSpan(span);
            }

            // Output an error message if we had any bad spans.
            if (originalSize > 1) {
                System.err.println(
                        "USAGE ERROR - We were asked to fully complete a request span"
                );
            }
        }

        currentSpanStackThreadLocal.remove();
    }


    public void completeSubSpan(Throwable uncaughtException, List<Throwable> caughtExceptionList, Map<String, Object> map) {
        
        Deque<Span> currentSpanStack = currentSpanStackThreadLocal.get();
        if (currentSpanStack == null || currentSpanStack.size() < 2) {
            int stackSize = (currentSpanStack == null) ? 0 : currentSpanStack.size();
            System.err.println(
                    "[" + Thread.currentThread().getName() + "]" +
                    "USAGE ERROR - Expected to find a child sub-span on the stack to complete," +
                            " but the span stack was size " + stackSize);
            // Nothing to do
            return;
        }

        // We have at least two spans. Pop off the child sub-span and complete/log it.
        Span subSpan = currentSpanStack.pop();

        subSpan.setCaughtExceptionList(caughtExceptionList);
        subSpan.setUncaughtException(uncaughtException);
        if (map != null) {
            for (Map.Entry<String, Object> item : map.entrySet()) {
                subSpan.addExtraData(item.getKey(), item.getValue());
            }
        }
        completeAndLogSpan(subSpan);
    }

    
    protected void completeAndLogSpan(Span span) {
        // Complete the span.
        if (span.hasComplete()) {
            System.err.println(
                "USAGE ERROR - An attempt was made to complete a span that was already completed. This call will be ignored. "
                + "wingtips_usage_error=true, already_completed_span=true, trace_id={}, span_id={}"
            );
            return;
        }
        else
            span.complete();

        // Notify listeners.
        notifySpanCompleted(span);
    }


    protected boolean isNextRootSpanSampleable() {
        return rootSpanSamplingStrategy.isNextRootSpanSampleable();
    }

    
    public void addSpanLifecycleListener(SpanLifecycleListener listener) {
        if (listener != null)
            this.spanLifecycleListeners.add(listener);
    }


    protected void notifySpanStarted(Span span) {
        for (SpanLifecycleListener tll : spanLifecycleListeners) {
            tll.spanStarted(span);
        }
    }

    
    private void notifyIfSpanSampled(Span span) {
        if (span.sampleable()) {
            for (SpanLifecycleListener tll : spanLifecycleListeners) {
                tll.spanSampled(span);
            }
        }
    }

    
    protected void notifySpanCompleted(Span span) {
        for (SpanLifecycleListener tll : spanLifecycleListeners) {
            tll.spanCompleted(span);
        }
    }


}
