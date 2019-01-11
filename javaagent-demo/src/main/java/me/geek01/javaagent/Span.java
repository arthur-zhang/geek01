package me.geek01.javaagent;


import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Span  {
    
    private String appId;
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String spanName;
    private boolean sampleable;
    private SpanType spanType;
    private long spanStartTimeEpochTimeMillis;
    
    private long spanStartTimeNanos;
    
    private int rootType = 0;
    
    private Integer timeCost;
    
    private Throwable uncaughtException;
    private List<Throwable> caughtExceptionList;
    private boolean rootSpanHasSubSpanError = false;

    private Map<String, Object> extraDataMap;

    public void addExtraData(String key, Object value) {
        if (extraDataMap == null) extraDataMap = new HashMap<>();
        extraDataMap.put(key, value);
    }

    public Map<String, Object> getExtraDataMap() {
        return extraDataMap;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }
    
    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }
    
    public void setSpanName(String spanName) {
        this.spanName = spanName;
    }
    
    public void setSpanType(SpanType spanType) {
        this.spanType = spanType;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    
    public String getSpanId() {
        return spanId;
    }
    
    
    public String getParentSpanId() {
        return parentSpanId;
    }
    
    
    public String getSpanName() {
        return spanName;
    }
    
    public String getAppId() {
        return appId;
    }
    
    
    public void setRootType(int rootType) {
        this.rootType = rootType;
    }
    
    public int getRootType() {
        return rootType;
    }
    
    public boolean isSampleable() {
        return sampleable;
    }
    
    public void setUncaughtException(Throwable uncaughtException) {
        this.uncaughtException = uncaughtException;
    }
    
    public Throwable getUncaughtException() {
        return uncaughtException;
    }
    
    public List<Throwable> getCaughtExceptionList() {
        return caughtExceptionList;
    }
    
    public void setCaughtExceptionList(List caughtExceptionList) {
        try {
            this.caughtExceptionList = caughtExceptionList;
        } catch (Exception e) {
            // ignore
            e.printStackTrace();
        }
    }
    
    public long getSpanStartTimeEpochTimeMillis() {
        return spanStartTimeEpochTimeMillis;
    }
    
    
    public long getSpanStartTimeNanos() {
        return spanStartTimeNanos;
    }
    
    public SpanType getSpanType() {
        return spanType;
    }
    public int getTimeCost() {
        return timeCost;
    }
    
    
    public void setRootSpanHasSubSpanError(boolean hasError) {
        this.rootSpanHasSubSpanError = hasError;
    }
    
    public boolean getRootSpanHasSubSpanError() {
        return rootSpanHasSubSpanError;
    }
    
    public enum SpanType {
        
        HTTP_REQUEST,
        
        CLIENT,
        
        LOCAL_ONLY,
        JEDIS_POOL,
        DRUID_POOL,
        DBCP2_POOL,
        TOMCAT_JDBC_POOL,
        PSD_CONNECTION_POOL,
        OK_HTTP,
        COMMONS_HTTP,
        SPRING_HTTP,
        JEDIS_CMD,
        JDBC_EXECUTE,
        DUBBO_CONSUMER,
        DUBBO_PROVIDER,
        MONGO,
        UNKNOWN
    }
    
    public Span() {
    }
    
    public Span(String appId, String traceId, String parentSpanId, String spanId, String spanName, boolean sampleable,
                SpanType spanType, long spanStartTimeEpochTimeMillis, long spanStartTimeNanos, Integer timeCost) {

        this.appId = appId == null ? "" : appId;
        if (traceId == null)
            throw new IllegalArgumentException("traceId cannot be null");
        
        if (spanId == null)
            throw new IllegalArgumentException("spanId cannot be null");
        
        if (spanName == null)
            throw new IllegalArgumentException("spanName cannot be null");
        
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.spanName = spanName;
        this.sampleable = sampleable;
        this.spanStartTimeEpochTimeMillis = spanStartTimeEpochTimeMillis;
        this.spanStartTimeNanos = spanStartTimeNanos;
        
        this.timeCost = timeCost;
        
        if (spanType == null)
            spanType = SpanType.UNKNOWN;
        
        this.spanType = spanType;
    }
    
    
    
    public static Builder generateRootSpanForNewTrace(String spanName, String appId, SpanType spanType) {
        return Span.newBuilder(spanName, spanType).withAppId(appId);
    }
    
    
    public Span generateChildSpan(String spanName, String appId, SpanType spanType) {
        return Span.newBuilder(this)
                .withAppId(appId)
                .withParentSpanId(this.spanId)
                .withSpanName(spanName)
                .withSpanId(TraceAndSpanIdGenerator.generateId())
                .withSpanStartTimeEpochTimeMillis(System.currentTimeMillis())
                .withSpanStartTimeNanos(System.nanoTime())
                .withTimeCost(null)
                .withSpanPurpose(spanType)
                .build();
    }
    
    
    public static Builder newBuilder(String spanName, SpanType spanType) {
        return new Builder(spanName,   spanType);
    }
    
    
    public static Builder newBuilder(Span copy) {
        Builder builder = new Builder(copy.spanName, copy.spanType);
        builder.traceId = copy.traceId;
        builder.appId = copy.appId;
        builder.spanId = copy.spanId;
        builder.parentSpanId = copy.parentSpanId;
        builder.sampleable = copy.sampleable;
        builder.spanStartTimeEpochTimeMillis = copy.spanStartTimeEpochTimeMillis;
        builder.spanStartTimeNanos = copy.spanStartTimeEpochTimeMillis;
        builder.timeCost = copy.timeCost;
        return builder;
    }
    
    
    public boolean sampleable() {
        return sampleable;
    }
    
    
    
    void complete() {
        if (this.timeCost != null)
            throw new IllegalStateException("This Span is already completed.");
        
        this.timeCost = (int)(System.currentTimeMillis() - spanStartTimeEpochTimeMillis);
    }
    
    
    public boolean hasComplete() {
        return timeCost != null;
    }
    
    
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Span)) {
            return false;
        }
        Span span = (Span) o;
        return sampleable == span.sampleable &&
                spanStartTimeEpochTimeMillis == span.spanStartTimeEpochTimeMillis &&
                spanType == span.spanType &&
                Objects.equals(traceId, span.traceId) &&
                Objects.equals(spanId, span.spanId) &&
                Objects.equals(parentSpanId, span.parentSpanId) &&
                Objects.equals(spanName, span.spanName) &&
                Objects.equals(timeCost, span.timeCost);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(traceId, spanId, parentSpanId, spanName, sampleable, spanType, spanStartTimeEpochTimeMillis, spanStartTimeNanos, timeCost);
    }
    
    public static final class Builder {
        
        private String appId;
        private String traceId;
        private String spanId;
        private String parentSpanId;
        private String spanName;
        private boolean sampleable = true;
        private Long spanStartTimeEpochTimeMillis;
        private Long spanStartTimeNanos;
        private Integer timeCost;
        private SpanType spanType;
        
        private Builder(String spanName, SpanType spanType) {
            this.spanName = spanName;
            this.spanType = spanType;
        }
        
        
        public Builder withTraceId(String traceId) {
            this.traceId = traceId;
            return this;
        }
        
        public Builder withAppId(String appId) {
            this.appId = appId;
            return this;
        }
        
        
        public Builder withSpanId(String spanId) {
            this.spanId = spanId;
            return this;
        }
        
        
        public Builder withParentSpanId(String parentSpanId) {
            this.parentSpanId = parentSpanId;
            return this;
        }
        
        
        public Builder withSpanName(String spanName) {
            this.spanName = spanName;
            return this;
        }
        
        
        public Builder withSampleable(boolean sampleable) {
            this.sampleable = sampleable;
            return this;
        }
        
        
        public Builder withSpanStartTimeEpochTimeMillis(Long spanStartTimeEpochMicros) {
            this.spanStartTimeEpochTimeMillis = spanStartTimeEpochMicros;
            return this;
        }
        public Builder withSpanStartTimeNanos(Long spanStartTimeNanos) {
            this.spanStartTimeNanos = spanStartTimeNanos;
            return this;
        }
        
        
        public Builder withTimeCost(Integer durationNanos) {
            this.timeCost = durationNanos;
            return this;
        }
        
        
        public Builder withSpanPurpose(SpanType spanType) {
            this.spanType = spanType;
            return this;
        }
        
        
        public Span build() {
            if (traceId == null)
                traceId = TraceAndSpanIdGenerator.generateId();
            
            if (spanId == null)
                spanId = TraceAndSpanIdGenerator.generateId();
            
            if (spanStartTimeEpochTimeMillis == null) {
                spanStartTimeEpochTimeMillis = System.currentTimeMillis();
            }
            if (spanStartTimeNanos == null) {
                spanStartTimeNanos = System.nanoTime();
            }
            
            return new Span(appId, traceId, parentSpanId, spanId, spanName, sampleable,  spanType, spanStartTimeEpochTimeMillis, spanStartTimeNanos, timeCost);
        }
    }
}
