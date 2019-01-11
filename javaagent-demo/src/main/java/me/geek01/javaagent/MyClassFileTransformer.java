package me.geek01.javaagent;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

public class MyClassFileTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classBytes) throws IllegalClassFormatException {

        // 处理常用库注入
        if (AgentMain.pluginClassNameSet.contains(className)) {
            Log.info("transform: servlet");
            ClassReader cr = new ClassReader(classBytes);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new PluginClassVisitor(cw, className, AgentMain.appId);
            cr.accept(cv, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            byte[] bytes = cw.toByteArray();
            File dumpFile = new File("/tmp/" + className.replaceAll("/", "_") + ".class");
            writeByteArrayToFile(bytes, dumpFile);
            Log.info("dump file: " + dumpFile);
            return bytes;
        }

        return classBytes;
    }

    public static class PluginClassVisitor extends ClassVisitor {
        private String className;
        private String appId;
        public PluginClassVisitor(ClassVisitor cv, String className, String appId) {
            super(Opcodes.ASM5, cv);
            this.className = className;
            this.appId = appId;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);
            Log.info("####################visit method:" + className + '\t' + methodName + "\t" + methodDesc + "\t" + appId);
            List<AspectInfo> aspectInfoList = AgentMain.pluginMaps.get(className);
            if (aspectInfoList == null) return mv;
            for (AspectInfo item : aspectInfoList) {
                String aspectMethodName = item.methodName;
                String aspectMethodDesc = item.methodDesc;
                if (!Wildcard.equalsOrMatch(methodName, aspectMethodName) || !Wildcard.equalsOrMatch(methodDesc, aspectMethodDesc)) {
                    continue;
                }
                Class<? extends AdviceAdapter>  clz = item.clz;
                try {
                    return ConstructorUtils.invokeConstructor(clz,
                            mv, access, methodName, methodDesc, appId, item);
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) e.printStackTrace();
                }
                return mv;
            }
            return mv;
        }
    }

    private static void writeByteArrayToFile(byte[] bytes, File file) {

        OutputStream out = null;
        try {
            out = new FileOutputStream(file, false);
            out.write(bytes, 0, bytes.length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
            }
        }
    }
}
