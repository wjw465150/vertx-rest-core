/*
 * author: @wjw
 * date:   2022年7月8日 下午3:05:57
 * note: 
 */
package org.wjw.vertx.rest.core.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * 根据服务接口类创建服务类的实例.
 *
 * @author White Stone
 * 
 *         2022年7月8日
 */
public final class AsyncServiceUtil {
  private static final ConcurrentMap<String, Object> serviceMap = new ConcurrentHashMap<>();

  /**
   * Gets the async service instance.
   *
   * @param <T> the generic type
   * @param asClazz the as clazz
   * @param vertx the vertx
   * @return the async service instance
   */
  public static <T> T getAsyncServiceInstance(Vertx vertx, Class<T> asClazz) {
    String address = asClazz.getName();
    if (serviceMap.containsKey(address)) {
      return (T) serviceMap.get(address);
    } else {
      T service = new ServiceProxyBuilder(vertx).setAddress(address).build(asClazz);
      serviceMap.putIfAbsent(address, service);

      return service;
    }
  }

  public static void clearServices() {
    serviceMap.clear();
  }
}
