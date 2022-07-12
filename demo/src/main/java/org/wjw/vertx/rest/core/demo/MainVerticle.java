package org.wjw.vertx.rest.core.demo;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wjw.vertx.rest.core.util.AsyncServiceUtil;
import org.wjw.vertx.rest.core.verticle.AsyncRegistryVerticle;
import org.wjw.vertx.rest.core.verticle.RouterRegistryVerticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {
  private Logger logger;

  public MainVerticle() {
    logger = LoggerFactory.getLogger(this.getClass());
  }
  
  @Override
  public void start(Promise<Void> startPromise) {
    String vertx_config_path;
    String profile;
    { //->校验是否指定了`profile`参数,和相应的配置文件是否存在!
      Properties sysProperties = System.getProperties();
      profile = sysProperties.getProperty("profile");
      if (profile == null) {
        System.out.println("Please set 'profile'");
        this.vertx.close();
        return;
      }

      //@wjw_note: 为了从classpath里加载配置文件!
      //也可以通过系统属性`vertx-config-path`来覆盖: java -jar my-vertx-first-app-1.0-SNAPSHOT--prod-fat.jar -Dvertx-config-path=conf/conf-prod.json
      vertx_config_path = sysProperties.getProperty("vertx-config-path");
      if (vertx_config_path == null) { //如果系统属性`vertx-config-path`没有被设置
        vertx_config_path = "conf/conf-" + profile + ".json";
      }

    } //<-校验是否指定了`profile`参数,和相应的配置文件是否存在!

    //加载配置文件
    ConfigStoreOptions classpathStore = new ConfigStoreOptions()
        .setType("file")
        .setConfig(new JsonObject().put("path", vertx_config_path));

    ConfigRetrieverOptions configOptions = new ConfigRetrieverOptions().addStore(classpathStore);
    ConfigRetriever        retriever     = ConfigRetriever.create(vertx, configOptions);
    
    //需要扫描有`@RouteHandler`注解的类的包路径列表(逗号分隔)
    String routerScanPackages = "org.wjw.vertx.rest.core.demo";

    //需要扫描的扩展了`org.wjw.vertx.rest.core.demo.base.BaseAsyncService`的抽象类的包路径列表(逗号分隔)
    String asyncServiceScanPackages = "org.wjw.vertx.rest.core.demo";

    // 异步服务的实例个数
    //为了提高处理速度,可以在同一个地址上重复注册异步服务.其实内部就是在相同的EvenBus地址上添加了新的consumer!
    int asyncServiceInstances = 1;

    retriever.getConfig().onSuccess(confJson -> {
      {//@wjw_note: 加载log的配置文件!
        try {
          String log_config_path = confJson.getString("logging");
          LogBackConfigLoader.load(log_config_path);
          logger.info("Logback configure file: " + log_config_path);
        } catch (Exception e) {
          e.printStackTrace();
          startPromise.fail(e);
        }
      }
      logger.info("!!!!!!=================Vertx App profile:" + profile + "=================!!!!!!");
      
      //部署Web Server的Vertivle和异步服务的Vertivcle
      logger.info("Start Deploy....");

      logger.info("Start registry router....");
      HttpServerOptions httpOptions = new HttpServerOptions();
      httpOptions.setPort(confJson.getInteger("http.port"));

      Future<String> future = vertx.deployVerticle(new RouterRegistryVerticle(httpOptions, routerScanPackages, confJson.getString("http.rootpath")));
      future.onSuccess(s -> {
        logger.info("Start registry service....");
        for (int i = 0; i < asyncServiceInstances; i++) {
          //@wjw_note: 为了提高处理速度,可以在同一个地址上重复注册异步服务.其实内部就是在相同的EvenBus地址上添加了新的consumer!
          vertx.deployVerticle(new AsyncRegistryVerticle(asyncServiceScanPackages), new DeploymentOptions().setWorker(true));
        }
      }).onFailure(ex -> {
        ex.printStackTrace();
        vertx.close();
      });
    });
  }
  
  @Override
  public void stop(Promise<Void> stopPromise) {
    //
  }
  
}
