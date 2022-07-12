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
 <version>1.1.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
implementation 'com.github.wjw465150:vertx-rest-core:1.1.0'
```

# 编写

## 程序入口类(测试时候用)

```java
package org.wjw.vertx.rest.core.demo;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/**
 * 一个使用`vertx-rest-core`模块的Main入口例子
 */
public class AppMain {
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
```

## 主Verticle类

```java
package org.wjw.vertx.rest.core.demo;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wjw.vertx.rest.core.verticle.ServiceRegistryVerticle;
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
          vertx.deployVerticle(new ServiceRegistryVerticle(asyncServiceScanPackages), new DeploymentOptions().setWorker(true));
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
```

## 异步服务接口

```java
package org.wjw.vertx.rest.core.demo.service;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface SensorDataService {
  void valueFor(String sensorId, Handler<AsyncResult<JsonObject>> handler);

  void average(Handler<AsyncResult<JsonObject>> handler);
} 
```

## 异步服务实现类

```java
package org.wjw.vertx.rest.core.demo.service.impl;

import org.wjw.vertx.rest.core.base.BaseAsyncService;
import org.wjw.vertx.rest.core.demo.service.SensorDataService;
import org.wjw.vertx.rest.core.util.IdWorker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class SensorDataServiceImpl extends BaseAsyncService implements SensorDataService {

  @Override
  public void valueFor(String sensorId, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject data = new JsonObject()
        .put("sensorId", sensorId)
        .put("value", IdWorker.getSnowFlakeIdStr());
    handler.handle(Future.succeededFuture(data));
  }

  @Override
  public void average(Handler<AsyncResult<JsonObject>> handler) {
    JsonObject data = new JsonObject()
        .put("average", "average")
        .put("value", IdWorker.getSnowFlakeIdStr());
    handler.handle(Future.succeededFuture(data));
  }

}
```

## REST类

```java
package org.wjw.vertx.rest.core.demo.rest;

import org.wjw.vertx.rest.core.annotaions.RouteHandler;
import org.wjw.vertx.rest.core.annotaions.RouteMapping;
import org.wjw.vertx.rest.core.annotaions.RouteMethod;
import org.wjw.vertx.rest.core.base.BaseRestApi;
import org.wjw.vertx.rest.core.demo.service.SensorDataService;
import org.wjw.vertx.rest.core.model.JsonResult;
import org.wjw.vertx.rest.core.util.AsyncServiceUtil;
import org.wjw.vertx.rest.core.util.ParamUtil;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@RouteHandler(value = "SensorDataApi")
public class SensorDataApi extends BaseRestApi {

  @RouteMapping(value = "/valueFor/:sensorId", method = RouteMethod.GET)
  public Handler<RoutingContext> valueFor() {
    return ctx -> {
      String sensorId = ctx.pathParam("sensorId");
      if (ParamUtil.isBlank(sensorId)) {
        sendError(400, ctx);
      } else {
        SensorDataService orderService = AsyncServiceUtil.getAsyncServiceInstance(ctx.vertx(),SensorDataService.class);
        orderService.valueFor(sensorId, ar -> {
          if (ar.succeeded()) {
            JsonObject product = ar.result();
            fireJsonResponse(ctx, new JsonResult(product));
          } else {
            fireErrorJsonResponse(ctx, ar.cause().getMessage());
          }
        });
      }
    };
  }

  @RouteMapping(value = "/average/", method = RouteMethod.GET)
  public Handler<RoutingContext> average() {
    return ctx -> {
      SensorDataService orderService = AsyncServiceUtil.getAsyncServiceInstance(ctx.vertx(),SensorDataService.class);
      orderService.average(ar -> {
        if (ar.succeeded()) {
          JsonObject product = ar.result();
          fireJsonResponse(ctx, new JsonResult(product));
        } else {
          fireErrorJsonResponse(ctx, ar.cause().getMessage());
        }
      });
    };
  }

}
```

## 配置文件

在`src\main\resources`目录下建立`conf-dev.json`配置文件,类容如下

```json
{ 
    "http.port" : 8080,
    "http.rootpath" : "/",
    "logging": "conf/log-dev.xml" 
}
```

日志文件`log-dev.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--scan: 当此属性设置为true时，配置文件如果发生改变，将会被重新加载，默认值为true。
scanPeriod: 设置监测配置文件是否有修改的时间间隔，如果没有给出时间单位，默认单位是毫秒。当scan为true时，此属性生效。默认的时间间隔为1分钟。
debug: 当此属性设置为true时，将打印出logback内部日志信息，实时查看logback运行状态。默认值为false。
configuration 子节点为 appender、logger、root
-->
<configuration scan="true" scanPeriod="60 seconds" debug="false">
  <property name="LOG_DIR" value="./logs" />
  <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - [%method,%line] - %msg%n" />
  
  <!-- 控制台输出 -->
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
      <!--格式化输出：%d表示日期，%thread表示线程名，%-5level：级别从左显示5个字符宽度，%msg：日志消息，%n是换行符 -->
      <pattern>${LOG_PATTERN}</pattern>
    </encoder>
  </appender>
  
  <!-- 输出控制台的所有信息到日志文件里 -->
  <appender name="FILE-ALL" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <!-- rollover daily -->
      <fileNamePattern>${LOG_DIR}/all_dev_%d{yyyy-MM-dd}_%i.log</fileNamePattern>
      <!-- each file should be at most 100MB, keep 30 days worth of history, but at most 10GB -->
      <maxFileSize>100MB</maxFileSize>    
      <maxHistory>30</maxHistory>
      <totalSizeCap>10GB</totalSizeCap>
      <cleanHistoryOnStart>true</cleanHistoryOnStart>      
    </rollingPolicy>
    <encoder>
      <pattern>${LOG_PATTERN}</pattern>
      <charset>UTF-8</charset>
    </encoder>
  </appender>
  
  <!-- 按照每天生成INFO级别日志文件 -->
  <appender name="FILE-INFO" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <!-- 
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
      <level>INFO</level>
    </filter>
    -->
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
      <level>INFO</level>
      <onMatch>ACCEPT</onMatch>
      <onMismatch>DENY</onMismatch>
    </filter>
    
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <!-- rollover daily -->
      <fileNamePattern>${LOG_DIR}/info_dev_%d{yyyy-MM-dd}_%i.log</fileNamePattern>
      <!-- each file should be at most 100MB, keep 30 days worth of history, but at most 10GB -->
      <maxFileSize>100MB</maxFileSize>    
      <maxHistory>30</maxHistory>
      <totalSizeCap>10GB</totalSizeCap>
      <cleanHistoryOnStart>true</cleanHistoryOnStart>      
    </rollingPolicy>
    <encoder>
      <pattern>${LOG_PATTERN}</pattern>
      <charset>UTF-8</charset>
    </encoder>
  </appender>
  
  <!-- 按照每天生成WARN级别日志文件 -->
  <appender name="FILE-WARN" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
      <level>WARN</level>
      <onMatch>ACCEPT</onMatch>
      <onMismatch>DENY</onMismatch>
    </filter>
    
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <!-- rollover daily -->
      <fileNamePattern>${LOG_DIR}/warn_dev_%d{yyyy-MM-dd}_%i.log</fileNamePattern>
      <!-- each file should be at most 100MB, keep 30 days worth of history, but at most 10GB -->
      <maxFileSize>100MB</maxFileSize>    
      <maxHistory>30</maxHistory>
      <totalSizeCap>10GB</totalSizeCap>
      <cleanHistoryOnStart>true</cleanHistoryOnStart>      
    </rollingPolicy>
    <encoder>
      <pattern>${LOG_PATTERN}</pattern>
      <charset>UTF-8</charset>
    </encoder>
  </appender>
  
  <!-- 按照每天生成ERROR级别日志文件 -->
  <appender name="FILE-ERROR" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
      <level>ERROR</level>
      <onMatch>ACCEPT</onMatch>
      <onMismatch>DENY</onMismatch>
    </filter>
    
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <!-- rollover daily -->
      <fileNamePattern>${LOG_DIR}/error_dev_%d{yyyy-MM-dd}_%i.log</fileNamePattern>
      <!-- each file should be at most 100MB, keep 30 days worth of history, but at most 10GB -->
      <maxFileSize>100MB</maxFileSize>    
      <maxHistory>30</maxHistory>
      <totalSizeCap>10GB</totalSizeCap>
      <cleanHistoryOnStart>true</cleanHistoryOnStart>      
    </rollingPolicy>
    <encoder>
      <pattern>${LOG_PATTERN}</pattern>
      <charset>UTF-8</charset>
    </encoder>
  </appender>

  <!--  LOGSTASH -->
  <![CDATA[
  <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>INFO</level>
    </filter>
      
    <!--  这是是logstash服务器地址 端口-->
    <destination>127.0.0.1:4567</destination>
    <!-- 可以连接多个LogStash
    <destination>destination1.domain.com:4560</destination> 
    <destination>destination2.domain.com:4560</destination> 
    <destination>destination3.domain.com:4560</destination>
    -->
    <connectionStrategy>  <!-- 连接策略  -->
      <roundRobin>  <!-- 轮询 -->
        <connectionTTL>5 minutes</connectionTTL>
      </roundRobin>
    </connectionStrategy>      
      
    <!-- encoder必须配置,有多种可选 -->
    <encoder class="net.logstash.logback.encoder.LogstashEncoder" charset="UTF-8">
      <customFields>{"appname":"Erupt-MtConsole"}</customFields>  <!-- 添加一个自定义字段"appname",表明应用的名字来方便查询 -->
    </encoder>
  </appender>
  ]]>
    
  <!--myibatis log configure -->
  <logger name="com.apache.ibatis" level="TRACE" />
  <logger name="java.sql.Connection" level="DEBUG" />
  <logger name="java.sql.Statement" level="DEBUG" />
  <logger name="java.sql.PreparedStatement" level="DEBUG" />

  <!--hibernate log configure -->
  <logger name="org.hibernate.SQL" level="DEBUG" />  <!-- 输出生成的SQL语句 -->
  <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE" />  <!-- 输出绑定参数值 -->
  <![CDATA[
  <logger name="org.hibernate.type.descriptor.sql.BasicExtractor" level="TRACE" />  <!-- 输出SELECT中获取的值 -->
  ]]>
  <logger name="org.hibernate.engine.QueryParameters" level="DEBUG" />  <!-- 输出查询中命名参数的值 -->
  <logger name="org.hibernate.engine.query.HQLQueryPlan" level="DEBUG" />  <!-- 输出查询中命名参数的值 -->

  <!-- 日志输出级别 -->
  <root level="INFO">
    <appender-ref ref="STDOUT" />
    
    <appender-ref ref="FILE-ALL" />
    <appender-ref ref="FILE-INFO" />
    <appender-ref ref="FILE-WARN" />
    <appender-ref ref="FILE-ERROR" />
    
    <!-- <appender-ref ref="LOGSTASH" />  -->
  </root>
  
</configuration>
```



> **注意:** 运行时要传入属性参数`-Dprofile`,例如:`-Dprofile=dev`来表明激活的是那个环境!

# 测试

可以通过URL来测试

1. `http://localhost:8080/SensorDataApi/valueFor/123`

2. `http://localhost:8080/SensorDataApi/average/`

------

全部源代码在: https://github.com/wjw465150/vertx-rest-core

