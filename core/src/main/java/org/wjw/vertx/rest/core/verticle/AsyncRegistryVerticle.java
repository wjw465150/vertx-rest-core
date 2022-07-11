package org.wjw.vertx.rest.core.verticle;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wjw.vertx.rest.core.base.BaseAsyncService;
import org.wjw.vertx.rest.core.util.ReflectionUtil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * 把异步服务注册到EventBus的Verticle.
 */
public class AsyncRegistryVerticle extends AbstractVerticle {
  /** The Constant LOGGER. */
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  /** 要注册的服务的包名字符串(可以是逗号分隔的列表) */
  private String packageAddress;

  /**
   * Instantiates a new async registry verticle.
   *
   * @param packageAddress the package address
   */
  public AsyncRegistryVerticle(String packageAddress) {
    Objects.requireNonNull(packageAddress, "given scan package address is empty");
    this.packageAddress = packageAddress;
  }

  /**
   * Start.
   *
   * @param startPromise the start promise
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public void start(Promise<Void> startPromise) {
    try {
      Set<Class<? extends BaseAsyncService>> handlers = ReflectionUtil.getReflections(packageAddress).getSubTypesOf(BaseAsyncService.class);
      ServiceBinder                          binder   = new ServiceBinder(vertx);
      if (null != handlers && handlers.size() > 0) {
        handlers.forEach(asyncService -> {
          try {
            Object asInstance                   = asyncService.newInstance();
            Method getAddressMethod             = asyncService.getMethod(BaseAsyncService.method_getAddress);
            String address                      = (String) getAddressMethod.invoke(asInstance);
            Method getAsyncInterfaceClassMethod = asyncService.getMethod(BaseAsyncService.method_getAsyncInterfaceClass);
            Class  clazz                        = (Class) getAsyncInterfaceClassMethod.invoke(asInstance);

            binder.setAddress(address).register(clazz, asInstance);
          } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new java.lang.RuntimeException(e);
          }
        });
        LOGGER.info("All async services registered");
      }
      startPromise.complete();
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      startPromise.fail(e);
    }
  }
}
