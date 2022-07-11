package org.wjw.vertx.rest.core.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.serviceproxy.ServiceException;

/**
 * 异步服务的抽象基础类,所有异步服务实现类都要扩展自此类!.
 */
public abstract class BaseAsyncService {

    /** The logger. */
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public static final String method_getAddress="getAddress";
    public static final String method_getAsyncInterfaceClass="getAsyncInterfaceClass";
    
    /**
     * Gets the address.
     *
     * @return the address
     */
    public String getAddress() {
        String className = this.getClass().getName();
        return className.substring(0, className.lastIndexOf("Impl")).replace(".impl", "");
    }

    /**
     * Gets the async interface class.
     *
     * @return the async interface class
     * @throws ClassNotFoundException the class not found exception
     */
    public Class<?> getAsyncInterfaceClass() throws ClassNotFoundException {
        String className = this.getClass().getName();
        return Class.forName(className.substring(0, className.lastIndexOf("Impl")).replace(".impl", ""));
    }

    /**
     * Handle exception.
     *
     * @param <T> the generic type
     * @param throwable the throwable
     * @param resultHandler the result handler
     */
    protected <T> void handleException(Throwable throwable, Handler<AsyncResult<T>> resultHandler) {
        LOGGER.error(throwable.getMessage(), throwable);
        resultHandler.handle(ServiceException.fail(1, throwable.getMessage()));
    }
}
