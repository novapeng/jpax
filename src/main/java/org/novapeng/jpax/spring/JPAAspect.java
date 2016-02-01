package org.novapeng.jpax.spring;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.novapeng.jpax.JPAX;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 *
 * this aspect used for open connection / close connection
 *
 * Created by pengchangguo on 15/10/23.
 */
@Component
@Aspect()
public class JPAAspect {

    private static ThreadLocal<String> methodFlagThreadLocal = new ThreadLocal<String>();

    @Pointcut("")
    public void point() {}


    @Around(value = "point()")
    public Object invocation(ProceedingJoinPoint joinPoint) throws Throwable {
        /* execute method */
        Object result;
        try {
            if(is(joinPoint)) {
                JPAX.beforeInvocation();
            }
            result = joinPoint.proceed(joinPoint.getArgs());
            if(is(joinPoint)) {
                JPAX.afterInvocation();
            }
        } catch (Throwable t) {
            JPAX.onException();
            throw t;
        } finally {
            if(is(joinPoint)) {
                JPAX.finalInvocation();
                //methodFlagThreadLocal.set(null);
                methodFlagThreadLocal.remove();
            }
        }
        return result;
    }

    private boolean is(JoinPoint joinPoint) {

        /* get methodFlag from joinPoint */
        String joinPointMethodFlag = getMethodFlag(joinPoint);

        /* get methodFlag from threadLocal */
        String threadLocalMethodFlag = methodFlagThreadLocal.get();
        if (threadLocalMethodFlag == null) {
            methodFlagThreadLocal.set(joinPointMethodFlag);
            return true;
        }

        return joinPointMethodFlag.equals(threadLocalMethodFlag);
        /*
        *//* get interfaces *//*
        Class targetClass = joinPoint.getTarget().getClass();
        Class[] interfaces = targetClass.getInterfaces();
        if (interfaces == null || interfaces.length == 0) return false;

        *//* if override method convert *//*
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        for (Class inter : interfaces) {
            if (!inter.getName().startsWith(JPAX.getProperty(Config.JPA_API_PACKAGE, "com.ctc.zhengxin.api"))) continue;
            try {
                if (inter.getDeclaredMethod(method.getName(), method.getParameterTypes()) != null) {
                    return true;
                }
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
        return false;*/
    }

    private String getMethodFlag(JoinPoint joinPoint) {
        Class targetClass = joinPoint.getTarget().getClass();
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        return targetClass.getName() + "." + method.getName() + "(" + Arrays.toString(method.getParameterTypes()) + ")";
    }
}
