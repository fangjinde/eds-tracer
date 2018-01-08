package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 17/12/22.
 */

import com.google.common.base.Optional;
import com.netease.edu.boot.hystrix.annotation.EduHystrixCommand;
import com.netflix.hystrix.contrib.javanica.exception.FallbackDefinitionException;
import com.netflix.hystrix.contrib.javanica.utils.FallbackMethod;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by dmgcodevil
 * @author hzfjd
 * @create 17/12/22
 */
public final class EduMethodProvider {

    private EduMethodProvider() {

    }

    private static final EduMethodProvider INSTANCE = new EduMethodProvider();

    public static EduMethodProvider getInstance() {
        return INSTANCE;
    }

    public FallbackMethod getFallbackMethod(Class<?> type, Method commandMethod) {
        return getFallbackMethod(type, commandMethod, false);
    }

    /**
     * Gets fallback method for command method.
     *
     * @param type          type
     * @param commandMethod the command method. in the essence it can be a fallback
     *                      method annotated with HystrixCommand annotation that has a fallback as well.
     * @param extended      true if the given commandMethod was derived using additional parameter, otherwise - false
     * @return new instance of {@link FallbackMethod} or {@link FallbackMethod#ABSENT} if there is no suitable fallback method for the given command
     */
    public FallbackMethod getFallbackMethod(Class<?> type, Method commandMethod, boolean extended) {
        if (commandMethod.isAnnotationPresent(EduHystrixCommand.class)) {
            EduHystrixCommand hystrixCommand = commandMethod.getAnnotation(EduHystrixCommand.class);
            if (StringUtils.isNotBlank(hystrixCommand.fallbackMethod())) {
                Class<?>[] parameterTypes = commandMethod.getParameterTypes();
                if (extended && parameterTypes[parameterTypes.length - 1] == Throwable.class) {
                    parameterTypes = ArrayUtils.remove(parameterTypes, parameterTypes.length - 1);
                }
                Class<?>[] exParameterTypes = Arrays.copyOf(parameterTypes, parameterTypes.length + 1);
                exParameterTypes[parameterTypes.length] = Throwable.class;
                Optional<Method> exFallbackMethod = getMethod(type, hystrixCommand.fallbackMethod(), exParameterTypes);
                Optional<Method> fMethod = getMethod(type, hystrixCommand.fallbackMethod(),
                                                     parameterTypes);
                Method method = exFallbackMethod.or(fMethod).orNull();
                if (method == null) {
                    throw new FallbackDefinitionException("fallback method wasn't found: " + hystrixCommand.fallbackMethod() + "(" + Arrays.toString(parameterTypes) + ")");
                }
                return new FallbackMethodEduAdapter(method, exFallbackMethod.isPresent());
            }
        }
        return FallbackMethod.ABSENT;
    }

    /**
     * Gets method by name and parameters types using reflection,
     * if the given type doesn't contain required method then continue applying this method for all super classes up to Object class.
     *
     * @param type           the type to search method
     * @param name           the method name
     * @param parameterTypes the parameters types
     * @return Some if method exists otherwise None
     */
    public Optional<Method> getMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Method[] methods = type.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                return Optional.of(method);
            }
        }
        Class<?> superClass = type.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            return getMethod(superClass, name, parameterTypes);
        } else {
            return Optional.absent();
        }
    }



    private static int getParameterCount(String desc) {
        return parseParams(desc).length;
    }

    private static String[] parseParams(String desc) {
        String params = desc.split("\\)")[0].replace("(", "");
        if (params.length() == 0) {
            return new String[0];
        }
        return params.split(";");
    }

    private static class MethodSignature {
        String name;
        String desc;

        public Class<?>[] getParameterTypes() throws ClassNotFoundException {
            if (desc == null) {
                return new Class[0];
            }
            String[] params = parseParams(desc);
            Class<?>[] parameterTypes = new Class[params.length];

            for (int i = 0; i < params.length; i++) {
                String arg = params[i].substring(1).replace("/", ".");
                parameterTypes[i] = Class.forName(arg);
            }
            return parameterTypes;
        }
    }


    /**
     * Configuration keys are formatted as unresolved <a href= "http://docs.oracle.com/javase/6/docs/jdk/api/javadoc/doclet/com/sun/javadoc/SeeTag.html"
     * >see tags</a>. This method exposes that format, in case you need to create the same value as
     * {@link feign.MethodMetadata#configKey()} for correlation purposes.
     *
     * <p>Here are some sample encodings:
     *
     * <pre>
     * <ul>
     *   <li>{@code Route53}: would match a class {@code route53.Route53}</li>
     *   <li>{@code Route53#list()}: would match a method {@code route53.Route53#list()}</li>
     *   <li>{@code Route53#listAt(Marker)}: would match a method {@code
     * route53.Route53#listAt(Marker)}</li>
     *   <li>{@code Route53#listByNameAndType(String, String)}: would match a method {@code
     * route53.Route53#listAt(String, String)}</li>
     * </ul>
     * </pre>
     *
     * Note that there is no whitespace expected in a key!
     *
     * @param targetType {@link feign.Target#type() type} of the Feign interface.
     * @param method invoked method, present on {@code type} or its super.
     * @see feign.MethodMetadata#configKey()
     */
    public static String getMethodSignature(Class targetType, Method method) {
       return  HystrixKeyUtils.getMethodSignature(targetType, method);
    }



}
