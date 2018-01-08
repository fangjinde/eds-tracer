package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/3.
 */

import com.netease.edu.boot.hystrix.core.OriginApplicationNameResolver;
import com.netease.edu.boot.hystrix.core.constants.OriginApplicationConstants;
import com.netflix.hystrix.contrib.javanica.command.MetaHolder;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hzfjd
 * @create 18/1/3
 */
public class HystrixKeyUtils {

    public static MetaHolder.Builder setDefaultKeyBySignatureAndOrigin(MetaHolder.Builder builder, Method method,
                                                                       Object obj,
                                                                       OriginApplicationNameResolver originApplicationNameResolver) {
        //ThreadPoolKey默认：RemoteAppcationName+Class+Method+ArgumentTypes
        String defaultCommandGroupKey = obj.getClass().getName();
        String defaultCommandKey = getDefaultCommandKey(method, obj);
        String defaultThreadPoolKey = defaultCommandKey;
        if (originApplicationNameResolver != null) {
            String originApplicationName = originApplicationNameResolver.getOriginApplicationName();
            if (StringUtils.isNotBlank(originApplicationName)) {
                defaultThreadPoolKey = new StringBuilder(originApplicationName).append(
                        OriginApplicationConstants.SEPARATOR).append(
                        defaultThreadPoolKey).toString();
            }
        }
        builder.defaultGroupKey(defaultCommandGroupKey).defaultCommandKey(defaultCommandKey).defaultThreadPoolKey(
                defaultThreadPoolKey);
        return builder;
    }

    public static String getDefaultCommandKey(Method method, Object obj) {
        return EduMethodProvider.getMethodSignature(obj.getClass(), method);
    }

    public static String parseOriginApplicationName(String originKeyName) {
        if (StringUtils.isBlank(originKeyName)) {
            return null;
        }
        int index = originKeyName.indexOf(OriginApplicationConstants.SEPARATOR);
        if (index > -1) {
            return originKeyName.substring(0, index);
        }
        return null;

    }

    public static String getHystrixFallbackThreadPoolKey(String originMethodThreadPoolKey, String currentCommandKey) {
        String originApplicationName = parseOriginApplicationName(originMethodThreadPoolKey);

        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(originApplicationName)) {
            sb.append(originApplicationName).append(OriginApplicationConstants.SEPARATOR);
        }
        sb.append(currentCommandKey);
        return sb.toString();
    }

    /**
     * Configuration keys are formatted as unresolved <a href= "http://docs.oracle.com/javase/6/docs/jdk/api/javadoc/doclet/com/sun/javadoc/SeeTag.html"
     * >see tags</a>. This method exposes that format, in case you need to create the same value as
     * {@link feign.MethodMetadata#configKey()} for correlation purposes.
     * <p>Here are some sample encodings:
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
     * Note that there is no whitespace expected in a key!
     *
     * @param targetType {@link feign.Target#type() type} of the Feign interface.
     * @param method     invoked method, present on {@code type} or its super.
     * @see feign.MethodMetadata#configKey()
     */
    public static String getMethodSignature(Class targetType, Method method) {
        List<String> paraClassSimpleNames=new ArrayList<String>();
        for (Type param : method.getGenericParameterTypes()) {
            param = Types.resolve(targetType, targetType, param);
            paraClassSimpleNames.add(Types.getRawType(param).getSimpleName());
        }
        return getMethodSignature(targetType.getName(),method.getName(), paraClassSimpleNames);
    }

    public static String getMethodSignature(Class targetType, String methodName, Class<?>[] parameterTypes) {
        List<String> paraClassSimpleNames=new ArrayList<String>();
        for (Class<?> paramClass:parameterTypes){
            paraClassSimpleNames.add(paramClass.getSimpleName());
        }
       return getMethodSignature(targetType.getName(),methodName, paraClassSimpleNames);
    }

    public static String getCommandKey(String prefix,String rawCommand){

        StringBuilder sb=new StringBuilder(prefix).append(".");
        sb.append(rawCommand);
        return sb.toString();

    }

    public static String getThreadPoolKey(String prefix,String rawCommand,String originApplicationName){

        StringBuilder sb=new StringBuilder(prefix).append(".");
        if (StringUtils.isNotBlank(originApplicationName)){
            sb.append(originApplicationName).append(OriginApplicationConstants.SEPARATOR);
        }
        sb.append(rawCommand);
        return sb.toString();

    }

    private  static String getMethodSignature(String className,String methodName,List<String> paraClassSimpleNames){
        StringBuilder builder = new StringBuilder();
        builder.append(className);
        builder.append('#').append(methodName).append('(');
        for (String paraClassSimpleName : paraClassSimpleNames) {
            builder.append(paraClassSimpleName).append(',');
        }
        if (paraClassSimpleNames.size() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.append(')').toString();
    }


    public static void main(String[] args) throws NoSuchMethodException {

        //testParseOriginApplicationName();
    }

    public static void testParseOriginApplicationName() {

        String testcase1 = "com.netease.edu.member.api.controller.ApiStratchController#testDoubleFallback(Integer)";
        String testcase2 = "CourseSevice@com.netease.edu.member.api.controller.ApiStratchController#testDoubleFallback(Integer)";
        String testcase3 = "@com.netease.edu.member.api.controller.ApiStratchController#testDoubleFallback(Integer)";
        String testcase4 = "CourseSevice@";
        System.out.println(parseOriginApplicationName(testcase1));
        System.out.println(parseOriginApplicationName(testcase2));
        System.out.println(parseOriginApplicationName(testcase3));
        System.out.println(parseOriginApplicationName(testcase4));
    }

}
