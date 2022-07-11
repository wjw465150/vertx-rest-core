package org.wjw.vertx.rest.core.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wjw.vertx.rest.core.handlerfactory.RouterHandlerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.web.Router;

/**
 * 创建HTTP Web Server和Router的Verticle
 */
public class RouterRegistryVerticle extends AbstractVerticle {
  /** The Constant LOGGER. */
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  private HttpServer server;

  //Represents options used by an HttpServer instance
  private HttpServerOptions httpOptions;

  //需要扫描有`@RouteHandler`注解的类的包路径列表(逗号分隔)
  private String routerScanPackages;

  //rest api前缀
  private String gatewayPrefix;

  public RouterRegistryVerticle(HttpServerOptions httpOptions, String routerScanPackages, String gatewayPrefix) {
    this.httpOptions = httpOptions;
    this.routerScanPackages = routerScanPackages;
    this.gatewayPrefix = gatewayPrefix;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    LOGGER.info("To start listening to port {} ......", httpOptions.getPort());

    try {
      // 根据routerScanPackages & gatewayPrefix 来创建Router的实例
      Router router = new RouterHandlerFactory(routerScanPackages, gatewayPrefix).createRouter();

      server = vertx.createHttpServer(httpOptions)
          .requestHandler(router)
          .listen(ar -> {
            if (ar.succeeded()) {
              startPromise.complete();
            } else {
              startPromise.fail(ar.cause());
            }
          });
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      startPromise.fail(e);
    }
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (server == null) {
      stopPromise.complete();
      return;
    }
    server.close(result -> {
      if (result.failed()) {
        stopPromise.fail(result.cause());
      } else {
        stopPromise.complete();
      }
    });
  }
}
