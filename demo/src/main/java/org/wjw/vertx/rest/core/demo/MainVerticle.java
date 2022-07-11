package org.wjw.vertx.rest.core.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wjw.vertx.rest.core.util.VertxHolder;
import org.wjw.vertx.rest.core.verticle.AsyncRegistryVerticle;
import org.wjw.vertx.rest.core.verticle.RouterRegistryVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;

public class MainVerticle extends AbstractVerticle {
  private Logger LOGGER;

  public MainVerticle() {
    LOGGER = LoggerFactory.getLogger(this.getClass());
  }
  
  @Override
  public void start(Promise<Void> promise) {
    //创建Vertx实例应该里面调用此方法老保存对Vertx实例的引用
    VertxHolder.init(vertx);
    
    //需要扫描有`@RouteHandler`注解的类的包路径列表(逗号分隔)
    String routerScanPackages = "org.wjw.vertx.rest.core.demo";

    //rest api前缀,也就是URL Path前缀
    String gatewayPrefix = "/";

    //需要扫描的扩展了`org.wjw.vertx.rest.core.demo.base.BaseAsyncService`的抽象类的包路径列表(逗号分隔)
    String asyncServiceScanPackages = "org.wjw.vertx.rest.core.demo";

    //REST服务端口号
    int port = 8080;

    // 异步服务的实例个数
    //为了提高处理速度,可以在同一个地址上重复注册异步服务.其实内部就是在相同的EvenBus地址上添加了新的consumer!
    int asyncServiceInstances = 1;

    //部署Web Server的Vertivle和异步服务的Vertivcle
    LOGGER.info("Start Deploy....");

    LOGGER.info("Start registry router....");
    HttpServerOptions httpOptions = new HttpServerOptions();
    httpOptions.setPort(port);

    Future<String> future = VertxHolder.getVertxInstance().deployVerticle(new RouterRegistryVerticle(httpOptions, routerScanPackages, "/"));
    future.onSuccess(s -> {
      LOGGER.info("Start registry service....");
      for (int i = 0; i < asyncServiceInstances; i++) {
        //@wjw_note: 为了提高处理速度,可以在同一个地址上重复注册异步服务.其实内部就是在相同的EvenBus地址上添加了新的consumer!
        VertxHolder.getVertxInstance().deployVerticle(new AsyncRegistryVerticle(asyncServiceScanPackages), new DeploymentOptions().setWorker(true));
      }
    }).onFailure(ex -> {
      ex.printStackTrace();
      vertx.close();
    });
  }
  
  @Override
  public void stop(Promise<Void> stopPromise) {
  }
  
}
