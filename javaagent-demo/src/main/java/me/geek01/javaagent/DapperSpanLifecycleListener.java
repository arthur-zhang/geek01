package me.geek01.javaagent;


public class DapperSpanLifecycleListener implements SpanLifecycleListener {

    public DapperSpanLifecycleListener() {
    }

    @Override
    public void spanStarted(Span span) {
        // Do nothing
    }

    @Override
    public void spanSampled(Span span) {
        // Do nothing
    }

    @Override
    public void spanCompleted(Span span) {
        Log.info("spanCompleted: " + span);
        SpanModel record = SpanModel.fromSpan(span);
        MessageQueue.getInstance().add(record);
    }
}