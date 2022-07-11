package org.wjw.vertx.rest.core.util;

import java.util.Objects;

import io.vertx.core.Vertx;

/**
 * 保存Vertx实例的工具类.
 *
 * @author Ian
 */
public final class VertxHolder {

    /** The singleton vertx. */
    private static Vertx singletonVertx;

    /**
     * Inits the.
     *
     * @param vertx the vertx
     */
    public static void init(Vertx vertx) {
        Objects.requireNonNull(vertx, "未初始化Vertx");
        singletonVertx = vertx;
    }

    /**
     * Gets the vertx instance.
     *
     * @return the vertx instance
     */
    public static Vertx getVertxInstance() {
        Objects.requireNonNull(singletonVertx, "未初始化Vertx");
        return singletonVertx;
    }
}
