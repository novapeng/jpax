package org.novapeng.jpax;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 *
 * Created by pengchangguo on 15/11/16.
 */
@Deprecated
public class JPAClassFileTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            Class clazz = new ClassUtil.FileClassLoader(Thread.currentThread().getContextClassLoader()).findClass(null, classfileBuffer);
            byte[] bytes = new JPAEnhancer().enhanceThisClass(clazz);
            if (bytes == null) return classfileBuffer;
            return bytes;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
