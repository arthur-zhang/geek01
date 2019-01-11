package me.geek01.javaagent;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created By Arthur Zhang at 11/02/2017
 */
public class SpanModel extends Record {
    
    @JSONField(name = "traceId")
    public String traceId;
    @JSONField(name = "spanId")
    public String spanId;
    @JSONField(name = "parentSpanId")
    public String parentSpanId;
    @JSONField(name = "spanName")
    public String spanName;
    @JSONField(name = "spanType")
    public String spanType;
    @JSONField(name = "spanStartTimeMills")
    public long spanStartTimeMills;
    
    @JSONField(name = "spanStartTimeNanos")
    public long spanStartTimeNanos;
    
    @JSONField(name = "timeCost")
    public int timeCost;
    
    @JSONField(name = "exceptionStackTrace")
    public String exceptionStackTrace;
    
    @JSONField(name = "caughtExceptionStackTrace")
    public String caughtExceptionStackTrace;

    @JSONField(name = "rootType")
    public int rootType;
    @JSONField(name = "extraData")
    public Map<String, Object> extraDataMap;


    public static SpanModel fromSpan(Span span) {
        SpanModel spanModel = new SpanModel();
        spanModel.appId = span.getAppId();
        spanModel.traceId = span.getTraceId();
        spanModel.spanId = span.getSpanId();
        spanModel.parentSpanId = span.getParentSpanId();
        spanModel.spanName = span.getSpanName();
        spanModel.spanType = span.getSpanType().name();
        spanModel.spanStartTimeMills = span.getSpanStartTimeEpochTimeMillis();
        spanModel.spanStartTimeNanos = span.getSpanStartTimeNanos();
        spanModel.rootType = span.getRootType();
        spanModel.timeCost = span.getTimeCost();
        spanModel.exceptionStackTrace = getStackTraceFromThrowable(span.getUncaughtException());
        spanModel.caughtExceptionStackTrace = getCaughtExceptionStackTraceList(span.getCaughtExceptionList());
        spanModel.extraDataMap = span.getExtraDataMap();
        return spanModel;
    }
    public static String getStackTraceFromThrowable(Object uncaughtException) {
        String uncaughtExceptionStackTrace = "";

        try {
            if (uncaughtException != null) {
                uncaughtExceptionStackTrace = ExceptionUtils.getStackTrace((Throwable) uncaughtException);
            }
        } catch (Exception e) {

        }
        return uncaughtExceptionStackTrace;
    }

    public static String getCaughtExceptionStackTraceList(List exceptionList) {
        StringBuilder sb = new StringBuilder();
        if (exceptionList == null || exceptionList.size() <= 0) {
            return sb.toString();
        }
        for (Object item : exceptionList) {
            sb.append(getStackTraceFromThrowable(item));
            sb.append("\n");
        }
        return sb.toString();
    }
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
