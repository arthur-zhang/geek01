package me.geek01.javaagent;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;

/**
 * Created By Arthur Zhang at 21/02/2017
 */
public abstract class BaseAdviceAdapter extends AdviceAdapter {
    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    private Label methodStartLabel;
    private Label endFinallyLabel;
    protected String appId;
    protected AspectInfo aspectInfo;
    
    public BaseAdviceAdapter(MethodVisitor mv, int access, String name, String desc, String appId, AspectInfo aspectInfo) {
        super(ASM5, mv, access, name, desc);
        methodStartLabel = new Label();
        endFinallyLabel = new Label();
        this.appId = appId;
        this.aspectInfo = aspectInfo;
    }
    
    public abstract void loadOnBeforeArgs();
    
    public abstract void loadOnAfterArgs();
    
    public abstract void loadOnThrowArgs();
    
    public void beforeMethodEnter() {
    
    }
    public void afterMethodEnter() {
    
    }
    
    public void beforeMethodExit() {
    
    }
    
    public void afterMethodExit() {
    
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
    protected void onMethodEnter() {
        beforeMethodEnter();
        
        visitLabel(methodStartLabel);
        
        loadOnBeforeArgs();
        invokeOnBeforeMethod();
        
        afterMethodEnter();
    }
    
    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != ATHROW) {
            exitNormal();
        }
    }
    
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        
        visitTryCatchBlock(methodStartLabel, endFinallyLabel, endFinallyLabel, THROWABLE_TYPE.getInternalName());
        visitLabel(endFinallyLabel);
        
        exitThrow();
        super.visitMaxs(maxStack, maxLocals);
    }
    
    private void exitThrow() {
        loadOnThrowArgs();
        invokeOnAfterMethod();
        visitInsn(ATHROW);
    }
    
    private void exitNormal() {
        loadOnAfterArgs();
        invokeOnAfterMethod();
    }
    
    public void invokeOnBeforeMethod() {
        invokeStatic(Type.getType(this.getClass()), org.objectweb.asm.commons.Method.getMethod(aspectInfo.onBeforeMethod));
    }
    
    public void invokeOnAfterMethod() {
        invokeStatic(Type.getType(this.getClass()), Method.getMethod(aspectInfo.onAfterMethod));
    }
}
