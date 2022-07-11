# 简介

通过简单的注解就能让使用Vert.x的用户快速开发出REST风格的HTTP服务.

`vertx-rest-core`模块为REST框架核心模块，核心模块不包含任何业务代码，主要有自定义注解、工具、基础类和框架verticle.

# 准备

要使用此模块，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>com.github.wjw465150</groupId>
 <artifactId>vertx-rest-core</artifactId>
 <version>1.0.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'com.github.wjw465150:vertx-rest-core:1.0.0'
```

# 入口类

程序入口类

```java
package org.wjw.vertx.rest.core.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wjw.vertx.rest.core.util.VertxHolder;
import org.wjw.vertx.rest.core.verticle.AsyncRegistryVerticle;
import org.wjw.vertx.rest.core.verticle.RouterRegistryVerticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;

/**
 * 一个使用`vertx-rest-core`模块的Main入口例子
 */
public class AppMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppMain.class);

  public static void main(String[] args) {
    //需要扫描有`@RouteHandler`注解的类的包路径列表(逗号分隔)
    String routerScanPackages = "org.wjw.web.rest";

    //rest api前缀,也就是URL Path前缀
    String gatewayPrefix = "/";

    //需要扫描的扩展了`org.wjw.vertx.rest.core.demo.base.BaseAsyncService`的抽象类的包路径列表(逗号分隔)
    String asyncServiceScanPackages = "org.wjw.web.service";

    //REST服务端口号
    int port = 8080;

    // 异步服务的实例个数
    //为了提高处理速度,可以在同一个地址上重复注册异步服务.其实内部就是在相同的EvenBus地址上添加了新的consumer!
    int asyncServiceInstances = 1;

    //创建Vertx实例
    Vertx vertx = Vertx.vertx();

    //创建Vertx实例应该里面调用此方法老保存对Vertx实例的引用
    VertxHolder.init(vertx);

    //部署Web Server的Vertivle和异步服务的Vertivcle
    LOGGER.info("Start Deploy....");

    LOGGER.info("Start registry router....");
    HttpServerOptions httpOptions = new HttpServerOptions();
    httpOptions.setPort(port);

    VertxHolder.getVertxInstance().deployVerticle(new RouterRegistryVerticle(httpOptions, routerScanPackages, "/"));

    LOGGER.info("Start registry service....");
    if (asyncServiceInstances < 1) {
      asyncServiceInstances = 1;
    }
    for (int i = 0; i < asyncServiceInstances; i++) {
      //@wjw_note: 为了提高处理速度,可以在同一个地址上重复注册异步服务.其实内部就是在相同的EvenBus地址上添加了新的consumer!
      VertxHolder.getVertxInstance().deployVerticle(new AsyncRegistryVerticle(asyncServiceScanPackages), new DeploymentOptions().setWorker(true));
    }

  }
}
```

