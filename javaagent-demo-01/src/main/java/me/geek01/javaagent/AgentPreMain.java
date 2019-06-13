package me.geek01.javaagent;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

/**
 * Created By Arthur Zhang at 2018-12-18
 */
public class AgentPreMain {

    public static class ClassLoggerTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            System.out.println("transform: " + className);
            Path path = Paths.get("/tmp/" + className.replaceAll("/", ".") + ".class");
            try {
                Files.write(path, classfileBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return classfileBuffer;
        }
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        ClassFileTransformer classFileTransformer = new ClassLoggerTransformer();
        instrumentation.addTransformer(classFileTransformer, true);
    }
}
