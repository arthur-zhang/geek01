package me.geek01.javaagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AgentMain {

    public static void agentmain(String agentArgs, Instrumentation inst){
        System.out.println("agentmain called: " + agentArgs);
        inst.addTransformer(new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                System.out.println("agentmain load Class  :" + className);
                return classfileBuffer;
            }
        }, true);
    }
}


