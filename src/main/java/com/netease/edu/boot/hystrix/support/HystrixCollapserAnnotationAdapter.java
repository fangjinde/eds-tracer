package com.netease.edu.boot.hystrix.support;

import com.netease.edu.boot.hystrix.annotation.EduHystrixCollapser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author hzfjd
 * @create 17/12/21
 */
public class HystrixCollapserAnnotationAdapter implements HystrixCollapser {

    private EduHystrixCollapser eduHystrixCollapser = null;

    public HystrixCollapserAnnotationAdapter(EduHystrixCollapser eduHystrixCollapser) {
        this.eduHystrixCollapser = eduHystrixCollapser;
    }

    @Override
    public String collapserKey() {
        return eduHystrixCollapser.collapserKey();
    }

    @Override
    public String batchMethod() {
        return eduHystrixCollapser.batchMethod();
    }

    @Override
    public com.netflix.hystrix.HystrixCollapser.Scope scope() {
        return eduHystrixCollapser.scope();
    }

    @Override
    public HystrixProperty[] collapserProperties() {
        return eduHystrixCollapser.collapserProperties();
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return eduHystrixCollapser.annotationType();
    }

    public class Test {

        @EduHystrixCollapser(batchMethod = "testMethodOne")
        public void testMethods() {

        }

        public void testMethodOne() {

        }
    }

    public static void main(String[] args) {

        Method method = ReflectionUtils.findMethod(Test.class, "testMethods");
        EduHystrixCollapser eduHystrixCollapser1 = AnnotationUtils.getAnnotation(method, EduHystrixCollapser.class);
        System.out.println(eduHystrixCollapser1.batchMethod());
        HystrixCollapser hystrixCollapser = new HystrixCollapserAnnotationAdapter(eduHystrixCollapser1);
        System.out.println(hystrixCollapser.batchMethod());
    }
}
