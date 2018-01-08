package com.netease.edu.boot.hystrix.annotation;/**
 * Created by hzfjd on 17/12/21.
 */

/**
 * @author hzfjd
 * @create 17/12/21
 */

import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import java.lang.annotation.*;

/**
 * This annotation is used to collapse some commands into a single backend dependency call.
 * This annotation should be used together with {@link com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand} annotation.
 * <p/>
 * Example:
 * <pre>
 *     @HystrixCollapser(batchMethod = "getUserByIds"){
 *          public Future<User> getUserById(String id) {
 *          return null;
 * }
 *  @HystrixCommand
 *      public List<User> getUserByIds(List<String> ids) {
 *          List<User> users = new ArrayList<User>();
 *          for (String id : ids) {
 *              users.add(new User(id, "name: " + id));
 *          }
 *      return users;
 * }
 *   </pre>
 *
 * A method annotated with {@link EduHystrixCollapser} annotation can return any
 * value with compatible type, it does not affect the result of collapser execution,
 * collapser method can even return {@code null} or another stub.
 * Pay attention that if a collapser method returns parametrized Future then generic type must be equal to generic type of List,
 * for instance:
 * <pre>
 *     Future<User> - return type of collapser method
 *     List<User> - return type of batch command method
 * </pre>
 * <p/>
 * Note: batch command method must be annotated with {@link com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand} annotation.
 */
@Target({ ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EduHystrixCollapser {

    /**
     * Specifies a collapser key.
     * <p/>
     * default => the name of annotated method.
     *
     * @return collapser key.
     */
    String collapserKey() default "";

    /**
     * Method name of batch command.
     * <p/>
     * Method must have the following signature:
     * <pre>
     *     java.util.List method(java.util.List)
     * </pre>
     * NOTE: batch method can have only one argument.
     *
     * @return method name of batch command
     */
    String batchMethod();

    /**
     * Defines what scope the collapsing should occur within.
     * <p/>
     * default => the {@link com.netflix.hystrix.HystrixCollapser.Scope#REQUEST}.
     *
     * @return {@link com.netflix.hystrix.HystrixCollapser.Scope}
     */
    com.netflix.hystrix.HystrixCollapser.Scope scope() default com.netflix.hystrix.HystrixCollapser.Scope.REQUEST;

    /**
     * Specifies collapser properties.
     *
     * @return collapser properties
     */
    HystrixProperty[] collapserProperties() default {};

}
