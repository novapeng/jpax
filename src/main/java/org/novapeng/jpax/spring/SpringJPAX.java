package org.novapeng.jpax.spring;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import org.aspectj.lang.annotation.Pointcut;
import org.novapeng.jpax.ClassUtil;
import org.novapeng.jpax.Config;
import org.novapeng.jpax.JPAX;
import org.novapeng.jpax.UnexpectedException;

import java.util.Set;

/**
 *
 * 启动入口，通过 spring 自动注入 EntityManagerFactory 对象
 *
 * 只要开启spring的 @ComponentScan, 即可自动装配
 *
 * Created by pengchangguo on 15/10/26.
 */

public class SpringJPAX extends JPAX {
    

    /* jpaAspect class package */
    private static final String ASPECT_PACKAGE = "org.novapeng.jpax.spring";

    /* jpaAspect class name */
    private static final String ASPECT_CLASS_NAME = "org.novapeng.jpax.spring.JPAAspect";

    static void go() {

        /* inject aop point */
        injectAopPoint();

        /*  beforeInit~  */
        beforeInit();

        init();

    }

    private static void injectAopPoint() {
        /* modify jpaAspect's point value  */
        Set<Class> classSet = ClassUtil.getClasses(ASPECT_PACKAGE);
        Class jpaAspect = null;
        for (Class cls : classSet) {
            if (cls.getName().equals(ASPECT_CLASS_NAME)) jpaAspect = cls;
        }
        if (jpaAspect == null) {
            throw new UnexpectedException("no class " + ASPECT_CLASS_NAME);
        }

        ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        classPool.appendClassPath(new LoaderClassPath(jpaAspect.getClassLoader()));
        CtClass ct;
        try {
            ct = classPool.get(ASPECT_CLASS_NAME);
        } catch (NotFoundException e) {
            throw new UnexpectedException("class " + ASPECT_CLASS_NAME + " not found!", e);
        }
        ClassFile classFile = ct.getClassFile();
        ConstPool constPool = classFile.getConstPool();
        CtMethod point;
        try {
            point = ct.getDeclaredMethod("point");
        } catch (NotFoundException e) {
            throw new UnexpectedException("method point not found!", e);
        }
        MethodInfo methodInfo = point.getMethodInfo();
        AnnotationsAttribute attribute =  new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(Pointcut.class.getName(), constPool);
        annotation.addMemberValue("value", new StringMemberValue(getAopPackage(), constPool));
        attribute.setAnnotation(annotation);
        methodInfo.addAttribute(attribute);
        try {
            @SuppressWarnings("unused") Class cls = ct.toClass();
        } catch (CannotCompileException e) {
            throw new UnexpectedException("class " + ASPECT_CLASS_NAME + " can not compile!", e);
        }
    }

    private static String getAopPackage() {
        return Config.getProperty(Config.JPA_AOP_POINT, null);
    }

}
