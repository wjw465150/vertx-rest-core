package org.wjw.vertx.rest.core.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wjw.vertx.rest.core.util.VertxHolder;
import org.wjw.vertx.rest.core.verticle.AsyncRegistryVerticle;
import org.wjw.vertx.rest.core.verticle.RouterRegistryVerticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;

/**
 * 一个使用`vertx-rest-core`模块的Main入口例子
 */
public class AppMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppMain.class);

  public static void main(String[] args) {
    {//@wjw_note: 设置环境为开发环境,关闭文件缓存和模板缓存!
     //1. During development you might want to disable template caching so that the template gets reevaluated on each request. 
     //In order to do this you need to set the system property: vertxweb.environment or environment variable VERTXWEB_ENVIRONMENT to dev or development. 
     //By default caching is always enabled.

      //2. these system properties are evaluated once when the io.vertx.core.file.FileSystemOptions class is loaded, 
      //so these properties should be set before loading this class or as a JVM system property when launching it.

      System.setProperty("vertxweb.environment", "dev");
      //@wjw_note: 调试发现,当有众多的小js,css文件时,,Vertx总是用原始源刷新缓存中存储的版本,严重影响性能      
      System.setProperty("vertx.disableFileCaching", "true");
    }

    //防止调试的时候出现`BlockedThreadChecker`日志信息
    VertxOptions options                    = new VertxOptions();
    long         blockedThreadCheckInterval = 60 * 60 * 1000L;
    if (System.getProperties().getProperty("vertx.options.blockedThreadCheckInterval") != null) {
      blockedThreadCheckInterval = Long.valueOf(System.getProperties().getProperty("vertx.options.blockedThreadCheckInterval"));
    }
    options.setBlockedThreadCheckInterval(blockedThreadCheckInterval);

    //创建Vertx实例
    Vertx vertx = Vertx.vertx(options);

    vertx.deployVerticle(new MainVerticle());
  }
}