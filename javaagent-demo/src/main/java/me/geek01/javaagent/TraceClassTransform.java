package me.geek01.javaagent;

import com.google.common.base.Splitter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created By Arthur Zhang at 09/02/2017
 */
public class TraceClassTransform extends AdviceAdapter {

    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    private final String appId;
    private Label methodStartLabel;
    private Label endFinallyLabel;

    private String methodName;
    private String className;
    private HashMap<Label, List<String>> matchedHandle = new HashMap<>();
    private int caughtExceptionListLocal;

    public TraceClassTransform(MethodVisitor mv, int acc, String name, String desc, String appId, String className) {
        super(ASM5, mv, acc, name, desc);
        methodStartLabel = new Label();
        endFinallyLabel = new Label();
        methodName = name;
        this.className = className;
        this.appId = appId;

        caughtExceptionListLocal = newLocal(Type.getObjectType("java/util/ArrayList"));
    }

    @Override
    protected void onMethodEnter() {

        visitTypeInsn(NEW, "java/util/ArrayList");
        dup();
        visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        storeLocal(caughtExceptionListLocal, Type.getObjectType("java/util/ArrayList"));

        visitLabel(methodStartLabel);
        push(appId);
        push(className);
        push(methodName);
        invokeStatic(Type.getType(this.getClass()),
                new Method("startSubSpan",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
                ));
    }


    @Override
    protected void onMethodExit(int opcode) {
        Log.trace("on method exit: " + opcode);
        if (opcode != ATHROW) {
            exitMethod(false);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        Log.trace("visit max: " + maxStack + "\t" + maxLocals);

        visitTryCatchBlock(methodStartLabel, endFinallyLabel, endFinallyLabel, THROWABLE_TYPE.getInternalName());
        visitLabel(endFinallyLabel);

        exitMethod(true);
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String exception) {
        super.visitTryCatchBlock(start, end, handler, exception);
        if (exception != null) {
            List<String> handles = matchedHandle.get(handler);
            if (handles == null) handles = new ArrayList<>();
            handles.add(exception);
            matchedHandle.put(handler, handles);

            Log.trace("matched handle: " + handler);
            Log.trace("reach here: visitTryCatchBlock(): " + exception);
        }
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        if (label != null && matchedHandle.get(label) != null && !label.equals(endFinallyLabel)) {

            dup(); // exception

            loadLocal(caughtExceptionListLocal, Type.getObjectType("java/util/ArrayList"));
            swap(); // swap exception <-> list
            invokeVirtual(Type.getType(ArrayList.class), new Method("add", "(Ljava/lang/Object;)Z"));
            pop();
        }
    }

    private void exitMethod(boolean throwing) {

        if (throwing) {
            dup();
        } else {
            push((Type) null);
        }
        loadLocal(caughtExceptionListLocal);

        invokeStatic(Type.getType(this.getClass()),
                new Method("completeSubSpan",
                        "(Ljava/lang/Object;Ljava/lang/Object;)V"
                ));
        if (throwing) {
            visitInsn(ATHROW);
        }
    }

    public static void startSubSpan(String appId, String className, String methodName) {
        try {
            List<String> parts = Splitter.on("/").splitToList(className);
            String simpleClassName = parts.get(parts.size() - 1);

            Span parentSpan = Tracer.getInstance().getCurrentSpan();
            if (parentSpan == null) {
                return;
            }
            Tracer.getInstance().startSubSpan(simpleClassName + "." + methodName + "()", appId, Span.SpanType.LOCAL_ONLY);
        } catch (Exception e) {
        }
    }

    public static void completeSubSpan(Object uncaughtException, Object caughtExceptionList) {
        try {
            Span parentSpan = Tracer.getInstance().getCurrentSpan();
            if (parentSpan == null) {
                return;
            }
            Tracer.getInstance().completeSubSpan((Throwable) uncaughtException, (List<Throwable>)caughtExceptionList, null);
        } catch (Exception e) {

        }
    }
}

