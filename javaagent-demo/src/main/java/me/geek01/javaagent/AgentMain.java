package me.geek01.javaagent;

import com.google.common.base.Splitter;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created By Arthur Zhang at 2018-12-18
 */
public class AgentMain {
    public static final String TYPE_TOMCAT_WAR = "tomcat";
    public static String agentType = "";
    private static final String AGENT_ID_PARAMS_KEY = "agentId";
    private static final String AGENT_TYPE_PARAMS_KEY = "agentType";
    private static final String APP_ID_PARAMS_KEY = "appId";
    public static String agentId = "";
    public static String appId = "";

    public static void premain(String agentArgument, Instrumentation instrumentation) {

        if (agentArgument == null) {
            return;
        }
        String[] parts = agentArgument.split(",");
        for (String item : parts) {
            if (!item.contains(":")) {
                continue;
            }
            String[] keyVals = item.split(":");
            if (keyVals.length != 2) {
                continue;
            }
            if (APP_ID_PARAMS_KEY.equals(keyVals[0])) {
                appId = keyVals[1];
            } else if (AGENT_ID_PARAMS_KEY.equals(keyVals[0])) {
                agentId = keyVals[1];
            } else if (AGENT_TYPE_PARAMS_KEY.equals(keyVals[0])) {
                agentType = keyVals[1];
            }

        }
        System.out.println(appId + "\t" + agentType + "\t" + agentId);
        Tracer.getInstance().addSpanLifecycleListener(new DapperSpanLifecycleListener());

        initPlugins();
        instrumentation.addTransformer(new MyClassFileTransformer(), true);
    }


    public static Set<String> pluginClassNameSet = new HashSet<>();

    public static Map<String, List<AspectInfo>> pluginMaps = new ConcurrentHashMap<>();
    public static void initPlugins() {

        List<Class> clz = new ArrayList<>();


        Class[] clzCommon = new Class[] {
                MyHttpServletAdviceAdapter.class,
                JedisCmdAdviceAdapter.class
        };
        clz.addAll(Arrays.asList(clzCommon));

        for (Class cls : clz) {
            Aspect anno = (Aspect) cls.getAnnotation(Aspect.class);
            if (anno == null) continue;

            String className = anno.className();
            pluginClassNameSet.add(className);
            String[] methodList = anno.method();
            List<AspectInfo> list = new ArrayList<>();

            for (String item : methodList) {
                AspectInfo aspectInfo = new AspectInfo();
                aspectInfo.clz = cls;
                List<String> parts = Splitter.on(" ").splitToList(item);
                if (parts.size() > 2) {
                    continue;
                }

                if (parts.size() == 2) {
                    aspectInfo.methodName = parts.get(0) ;
                    aspectInfo.methodDesc = parts.get(1) ;
                }
                if (parts.size() == 1) {
                    aspectInfo.methodName = parts.get(0) ;
                    aspectInfo.methodDesc = "*";
                }
                list.add(aspectInfo);
            }
            pluginMaps.put(className, list);

            java.lang.reflect.Method[] methods = cls.getDeclaredMethods();
            Method onBeforeMethod = null;
            Method onReturnMethod = null;
            for (Method method : methods) {
                OnBefore onBeforeAnnotation = method.getAnnotation(OnBefore.class);
                OnAfter onAfterAnnotation = method.getAnnotation(OnAfter.class);

                if (onBeforeAnnotation != null) {
                    onBeforeMethod  = method;
                }
                if (onAfterAnnotation != null) {
                    onReturnMethod = method;
                }
            }
            for (AspectInfo classInfo : list) {
                classInfo.onAfterMethod = onReturnMethod;
                classInfo.onBeforeMethod = onBeforeMethod;
            }

        }
    }
}
