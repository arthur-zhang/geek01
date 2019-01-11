package me.geek01.javaagent;


import com.google.common.base.Strings;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;

@Aspect(className = "javax/servlet/http/HttpServlet",
        method = {"service (Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V"})
public class MyHttpServletAdviceAdapter extends AdviceAdapter {

    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    private Label methodStartLabel;
    private Label endFinallyLabel;
    protected String appId;

    private int startFlagLocal;

    public MyHttpServletAdviceAdapter(MethodVisitor mv, int access, String name, String desc, String appId, AspectInfo aspectInfo) {
        super(ASM5, mv, access, name, desc);
        methodStartLabel = new Label();
        endFinallyLabel = new Label();
        startFlagLocal = newLocal(Type.INT_TYPE);
        this.appId = appId;
    }


    @Override
    protected void onMethodEnter() {
        mv.visitInsn(ICONST_0);
        storeLocal(startFlagLocal);

        visitLabel(methodStartLabel);

        loadOnBeforeArgs();
        invokeOnBeforeMethod();
        storeLocal(startFlagLocal);
    }

    @Override
    protected void onMethodExit(int opcode) {
        Log.info("onMethodExit");
        if (opcode != ATHROW) {
            exitNormal();
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {

        visitTryCatchBlock(methodStartLabel, endFinallyLabel, endFinallyLabel, THROWABLE_TYPE.getInternalName());
        visitLabel(endFinallyLabel);

        exitThrow();

        visitInsn(ATHROW);

        super.visitMaxs(maxStack, maxLocals);
    }

    private void exitThrow() {
        loadOnThrowArgs();
        invokeOnAfterMethod();
    }

    private void exitNormal() {
        loadOnAfterArgs();
        invokeOnAfterMethod();
    }

    public void loadOnBeforeArgs() {
        push(appId);
        loadArg(0);
    }

    public void loadOnAfterArgs() {

        loadLocal(startFlagLocal);
        loadArgs();
        push((Type) null);
    }

    public void loadOnThrowArgs() {
        dup();
        int uncaughtExceptionLocal = newLocal(Type.getType(Throwable.class));
        storeLocal(uncaughtExceptionLocal, Type.getType(Throwable.class));

        loadLocal(startFlagLocal);
        loadArgs();
        loadLocal(uncaughtExceptionLocal);
    }

    public void invokeOnBeforeMethod() {
        invokeStatic(Type.getType(this.getClass()),
                new Method("startRequest",
                        "(Ljava/lang/String;Ljava/lang/Object;)I"
                ));
    }

    private void invokeOnAfterMethod() {
        invokeStatic(Type.getType(this.getClass()),
                new Method("completeRequestSpan",
                        "(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"
                ));
    }


    public static int startRequest(String appId, Object request) {
        try {
            if (Tracer.getInstance() == null || Tracer.getInstance().getCurrentSpan() != null) {
                return 0;
            }
            if (AgentMain.TYPE_TOMCAT_WAR.equals(AgentMain.agentType)) {
                appId = Strings.nullToEmpty((String) MethodUtils.invokeMethod(request, "getContextPath"));
                if (appId.startsWith("/")) {
                    appId = appId.replaceAll("/", "");
                }
            }
            Tracer tracer = Tracer.getInstance();
            final Span parentSpan = HttpSpanFactory.fromHttpServletRequest(request, appId);

            if (parentSpan != null) {
                tracer.startRequestWithChildSpan(appId, parentSpan, HttpSpanFactory.getServletSpanName(request));
            } else {
                tracer.startRequestWithRootSpan(HttpSpanFactory.getServletSpanName(request), appId);
            }
            return 1;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
        }
        return 0;
    }

    public static void completeRequestSpan(int startFlag, Object req, Object resp, Object uncaughtExceptionObj) {

        String url = "";
        String method = "";
        String queryString = "";
        int respCode = 0;
        Throwable uncaughtException = null;
        try {
            method = Strings.nullToEmpty(HttpServletReflectUtils.getMethod(req));
            String servletPath = Strings.nullToEmpty(HttpServletReflectUtils.getServletPath(req));
            String requestURI = Strings.nullToEmpty(HttpServletReflectUtils.getRequestURI(req));
            String contextPath = Strings.nullToEmpty(HttpServletReflectUtils.getContextPath(req));
            queryString = Strings.nullToEmpty(HttpServletReflectUtils.getQueryString(req));
            if (Strings.isNullOrEmpty(servletPath)) {
                if (requestURI.length() > 0 && contextPath.length() > 0 && contextPath.length() < requestURI.length()) {
                    url = requestURI.substring(contextPath.length(), requestURI.length());
                }
            } else {
                url = servletPath;
            }
            respCode = HttpServletReflectUtils.getStatus(resp);

            uncaughtException = (Throwable) uncaughtExceptionObj;

            if (respCode == 0) {
                respCode = 200;
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
        } finally {
            Map<String, Object> map = new HashMap<>();
            map.put("url", url);
            map.put("method", method);
            map.put("queryString", queryString);
            map.put("statusCode", respCode);
            Tracer.getInstance().completeRequestSpan(uncaughtException, map);
        }
    }
}

