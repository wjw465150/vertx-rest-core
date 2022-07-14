package org.wjw.vertx.rest.core.annotaions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异步服务类 标识注解.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncService {
    /**
     * 服务要实现的接口类.
     *
     * @return the class
     */
    Class<?> serviceInterface();
}
