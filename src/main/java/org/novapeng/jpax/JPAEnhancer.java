package org.novapeng.jpax;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import java.lang.annotation.Annotation;

/**
 * Enhance JPABase entities classes
 *
 * Created by pengchangguo on 15/10/23.
 */
public class JPAEnhancer {

    public byte[] enhanceThisClass(Class jpaClass) throws Exception {

        ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        classPool.appendClassPath(new LoaderClassPath(jpaClass.getClassLoader()));

        //noinspection unused
        String jpaClassName = jpaClass.getName();
        CtClass ctClass = classPool.get(jpaClass.getName());

        // Enhance only JPA entities
        if (!hasAnnotation(ctClass, "javax.persistence.Entity")) {
            return null;
        }
        boolean isModify = false;
        String entityName = ctClass.getName();

        // count
        if (noMethod(jpaClass, "count")) {
            CtMethod count = CtMethod.make("public static Long count() { return com.ctc.zhengxin.framework.jpa.jpax.JPQL.instance.count(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(count);
            isModify = true;
        }


        // count2
        if (noMethod(jpaClass, "count", String.class, Object[].class)) {
            CtMethod count2 = CtMethod.make("public static Long count(String query, Object[] params) { return com.ctc.zhengxin.framework.jpa.jpax.JPQL.instance.count(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(count2);
            isModify = true;
        }

        // findAll
        if (noMethod(jpaClass, "findAll")) {
            CtMethod findAll = CtMethod.make("public static java.util.List findAll() { return com.ctc.zhengxin.framework.jpa.jpax.JPQL.instance.findAll(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(findAll);
            isModify = true;
        }

        // findById
        if (noMethod(jpaClass, "findById", Object.class)) {
            CtMethod findById = CtMethod.make("public static com.ctc.zhengxin.framework.jpa.jpax.JPABase findById(Object id) { return com.ctc.zhengxin.framework.jpa.jpax.JPQL.instance.findById(\"" + entityName + "\", id); }", ctClass);
            ctClass.addMethod(findById);
            isModify = true;
        }

        // find
        if (noMethod(jpaClass, "find", String.class, Object[].class)) {
            CtMethod find = CtMethod.make("public static com.ctc.zhengxin.framework.jpa.jpax.JPAModel.JPAQuery find(String query, Object[] params) { return com.ctc.zhengxin.framework.jpa.jpax.JPQL.instance.find(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(find);
            isModify = true;
        }

        // find
        if (noMethod(jpaClass, "find")) {
            CtMethod find2 = CtMethod.make("public static com.ctc.zhengxin.framework.jpa.jpax.JPAModel.JPAQuery find() { return com.ctc.zhengxin.framework.jpa.jpax.JPQL.instance.find(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(find2);
            isModify = true;
        }

        // all
        if (noMethod(jpaClass, "all")) {
            CtMethod all = CtMethod.make("public static com.ctc.zhengxin.framework.jpa.jpax.JPAModel.JPAQuery all() { return com.ctc.zhengxin.framework.jpa.jpax.JPQL.instance.all(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(all);
            isModify = true;
        }

        // delete
        if (noMethod(jpaClass, "delete", String.class, Object[].class)) {
            CtMethod delete = CtMethod.make("public static int delete(String query, Object[] params) { return com.ctc.zhengxin.framework.jpa.jpax.JPQL.instance.delete(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(delete);
            isModify = true;
        }

        // deleteAll
        if (noMethod(jpaClass, "deleteAll")) {
            CtMethod deleteAll = CtMethod.make("public static int deleteAll() { return com.ctc.zhengxin.framework.jpa.jpax.JPQL.instance.deleteAll(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(deleteAll);
            isModify = true;
        }

        // findOneBy
        if (noMethod(jpaClass, "findOneBy", String.class, Object[].class)) {
            CtMethod findOneBy = CtMethod.make("public static com.ctc.zhengxin.framework.jpa.jpax.JPABase findOneBy(String query, Object[] params) { return com.ctc.zhengxin.framework.jpa.jpax.JPQL.instance.findOneBy(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(findOneBy);
            isModify = true;
        }

        // sql
        if (noMethod(jpaClass, "sql", String.class, Class.class, Object[].class)) {
            CtMethod sql = CtMethod.make("public static java.util.List sql(String sql, Class t, Object[] params) { return com.ctc.zhengxin.framework.jpa.jpax.JPSQL.instance.sql(\"" + entityName + "\", sql, t, params); }", ctClass);
            ctClass.addMethod(sql);
            isModify = true;
        }

        // sql
        if (noMethod(jpaClass, "sql", String.class, Object[].class)) {
            CtMethod sql = CtMethod.make("public static java.util.List sql(String sql, Object[] params) { return com.ctc.zhengxin.framework.jpa.jpax.JPSQL.instance.sql(\"" + entityName + "\", sql, params); }", ctClass);
            ctClass.addMethod(sql);
            isModify = true;
        }

        // sqlFirst
        if (noMethod(jpaClass, "sqlFirst", String.class, Class.class, Object[].class)) {
            CtMethod find2 = CtMethod.make("public static Object sqlFirst(String sql, Class t, Object[] params) { return com.ctc.zhengxin.framework.jpa.jpax.JPSQL.instance.sqlFirst(\"" + entityName + "\", sql, t, params); }", ctClass);
            ctClass.addMethod(find2);
            isModify = true;
        }

        // sqlFirst
        if (noMethod(jpaClass, "sqlFirst", String.class, Object[].class)) {
            CtMethod find2 = CtMethod.make("public static java.util.Map sqlFirst(String sql, Object[] params) { return com.ctc.zhengxin.framework.jpa.jpax.JPSQL.instance.sqlFirst(\"" + entityName + "\", sql, params); }", ctClass);
            ctClass.addMethod(find2);
            isModify = true;
        }

        if (!isModify) return null;
        //noinspection unused
        Class clazz = ctClass.toClass();
        byte[] bytes = ctClass.toBytecode();
        // Done.
        ctClass.defrost();
        return bytes;
    }

    private boolean noMethod(Class clazz, String methodName, Class... parameterTypes) {
        try {
            //noinspection unchecked
            clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return true;
        }
        return false;
    }

    private boolean hasAnnotation(CtClass ctClass, String annotation) throws ClassNotFoundException {
        for (Object object : ctClass.getAvailableAnnotations()) {
            Annotation ann = (Annotation) object;
            if (ann.annotationType().getName().equals(annotation)) {
                return true;
            }
        }
        return false;
    }

}
