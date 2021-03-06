package org.wjw.vertx.rest.core.annotaions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Web Router API类 标识注解.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RouteHandler {

    /**
     * URL中的path,缺省是空字符串.
     *
     * @return the string
     */
    String value() default "";

    /**
     * 注册顺序，数字越大越先注册. 缺省是:0
     *
     * @return the int
     */
    int order() default 0;
}
