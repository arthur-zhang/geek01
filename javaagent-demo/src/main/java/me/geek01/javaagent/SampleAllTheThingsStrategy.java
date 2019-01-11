package me.geek01.javaagent;


public class SampleAllTheThingsStrategy implements RootSpanSamplingStrategy {

    @Override
    public boolean isNextRootSpanSampleable() {
        return true;
    }
}
