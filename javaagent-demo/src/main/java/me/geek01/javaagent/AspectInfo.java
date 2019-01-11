package me.geek01.javaagent;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.reflect.Method;

/**
 * Created By Arthur Zhang at 20/02/2017
 */
public class AspectInfo {
    public Class<? extends AdviceAdapter> clz;
    public String methodName;
    public String methodDesc;
    public Method onBeforeMethod;
    public Method onAfterMethod;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
