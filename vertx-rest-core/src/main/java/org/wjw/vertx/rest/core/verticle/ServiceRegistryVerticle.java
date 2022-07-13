package org.wjw.vertx.rest.core.verticle;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wjw.vertx.rest.core.base.BaseAsyncService;
import org.wjw.vertx.rest.core.util.ReflectionUtil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * 把异步服务注册到EventBus的Verticle.
 */
public class ServiceRegistryVerticle extends AbstractVerticle {
  /** The Constant LOGGER. */
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  /** 要注册的服务的包名字符串(可以是逗号分隔的列表) */
  private String packageAddress;

  private static final List<MessageConsumer<JsonObject>> serviceList = new ArrayList<>();

  ServiceBinder serviceBinder;

  /**
   * Instantiates a new async registry verticle.
   *
   * @param packageAddress the package address
   */
  public ServiceRegistryVerticle(String packageAddress) {
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
      Set<Class<? extends BaseAsyncService>> serviceClasses = ReflectionUtil.getReflections(packageAddress).getSubTypesOf(BaseAsyncService.class);
      serviceBinder = new ServiceBinder(vertx);
      if (null != serviceClasses && serviceClasses.size() > 0) {
        serviceClasses.forEach(serviceClass -> {
          try {
            BaseAsyncService serviceInstance  = serviceClass.newInstance();
            String           address          = serviceInstance.getServiceAddress();
            Class            serviceInterfaceClazz = serviceInstance.getServiceInterfaceClass();

            MessageConsumer<JsonObject> consumer = serviceBinder.setAddress(address).register(serviceInterfaceClazz, serviceInstance);
            serviceList.add(consumer);
            LOGGER.info("Register New Service -> Address:`{}` Instance:`{}`", address, serviceInstance.getClass().getName());
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

  @Override
  public void stop(Promise<Void> stopPromise) {
    try {
      serviceList.stream().forEach(consumer -> {
        serviceBinder.unregister(consumer);
      });
      serviceList.clear();
    } finally {
      stopPromise.complete();
    }
  }
}
