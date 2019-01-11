package me.geek01.javaagent;

public interface SpanLifecycleListener {


    void spanStarted(Span span);

    void spanSampled(Span span);

    void spanCompleted(Span span);

}
