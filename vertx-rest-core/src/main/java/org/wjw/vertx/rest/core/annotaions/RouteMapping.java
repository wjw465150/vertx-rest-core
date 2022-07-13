package org.wjw.vertx.rest.core.annotaions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解到方法上的URI的请求路径
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RouteMapping {

    String value() default "";

    /**** 是否覆盖 *****/
    boolean isCover() default true;

    /**** 使用http method *****/
    RouteMethod method() default RouteMethod.GET;

    /**** 接口描述 *****/
    String descript() default "";

    /**
     * 注册顺序，数字越大越先注册
     */
    int order() default 0;

    String mimeType() default "";
}
