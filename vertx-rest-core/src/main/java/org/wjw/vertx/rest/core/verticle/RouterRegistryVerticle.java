/*
 * author: @wjw
 * date:   2022年7月15日 上午11:59:49
 * note: 
 */
package org.wjw.vertx.rest.core.verticle;

import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wjw.vertx.rest.core.annotaions.RouteHandler;
import org.wjw.vertx.rest.core.annotaions.RouteMapping;
import org.wjw.vertx.rest.core.annotaions.RouteMethod;
import org.wjw.vertx.rest.core.util.ParamUtil;
import org.wjw.vertx.rest.core.util.ReflectionUtil;
import org.wjw.vertx.rest.core.util.ServiceUtil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

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
  public void stop(Promise<Void> stopPromise) {
    ServiceUtil.clearServices();

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
  
  @Override
  public void start(Promise<Void> startPromise) {
    LOGGER.info("To start listening to port {} ......", httpOptions.getPort());

    try {
      // 根据routerScanPackages & gatewayPrefix 来创建Router的实例
      Router router = this.createRouter();

      Future<HttpServer> arServer = vertx.createHttpServer(httpOptions)
          .requestHandler(router)
          .listen();
      arServer.onComplete(ar -> {
        if (ar.succeeded()) {
          server = ar.result();
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

  /**
   * Creates the router.
   *
   * @return the router
   */
  private Router createRouter() {
    Reflections reflections = ReflectionUtil.getReflections(this.routerScanPackages);

    Router router = Router.router(this.vertx);
    router.route().handler(ctx -> {
      LOGGER.info("The HTTP service request address information ===>path:{}, uri:{}, method:{}",
          ctx.request().path(),
          ctx.request().absoluteURI(),
          ctx.request().method());

      ctx.response().headers().add(CONTENT_TYPE, "application/json; charset=utf-8");
      ctx.response().headers().add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
      ctx.response().headers().add(ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, OPTIONS, PUT, DELETE, HEAD");
      ctx.response().headers().add(ACCESS_CONTROL_ALLOW_HEADERS, "X-PINGOTHER, Origin,Content-Type, Accept, X-Requested-With, Dev, Authorization, Version, Token");
      ctx.response().headers().add(ACCESS_CONTROL_MAX_AGE, "1728000");
      ctx.next();
    });

    //添加CORS跨域的方法
    Set<HttpMethod> method = new HashSet<HttpMethod>() {
      private static final long serialVersionUID = 1L;
      { //这里是实例初始化块，可以直接调用父类的非私有方法或访问非私有成员
        add(HttpMethod.GET);
        add(HttpMethod.POST);
        add(HttpMethod.OPTIONS);
        add(HttpMethod.PUT);
        add(HttpMethod.DELETE);
        add(HttpMethod.HEAD);
      }
    };
    router.route().handler(CorsHandler.create("*").allowedMethods(method));

    //添加正文处理器
    router.route().handler(BodyHandler.create().setBodyLimit(1024 * 1024)); //以字节为单位设置最大正文大小,这里是1M!.

    try {
      Set<Class<?>>        handlers       = reflections.getTypesAnnotatedWith(RouteHandler.class);
      Comparator<Class<?>> comparator     = (c1, c2) -> {
                                            RouteHandler routeHandler1 = c1.getAnnotation(RouteHandler.class);
                                            RouteHandler routeHandler2 = c2.getAnnotation(RouteHandler.class);
                                            return Integer.compare(routeHandler2.order(), routeHandler1.order());
                                          };
      List<Class<?>>       sortedHandlers = handlers.stream().sorted(comparator).collect(Collectors.toList());
      for (Class<?> handler : sortedHandlers) {
        registerRestHandler(router, handler);
      }
    } catch (Exception e) {
      LOGGER.error("Manually Register Handler Fail，Error details：" + e.getMessage());
      throw new java.lang.RuntimeException(e);
    }
    return router;
  }

  /**
   * Register a rest handler.
   *
   * @param router the router
   * @param handler the handler
   * @throws Exception the exception
   */
  private void registerRestHandler(Router router, Class<?> handler) throws Exception {
    String restUrl = this.gatewayPrefix;
    if (!restUrl.startsWith("/")) {
      restUrl = "/" + restUrl;
    }
    if (!restUrl.endsWith("/")) {
      restUrl = restUrl + "/";
    }
    if (handler.isAnnotationPresent(RouteHandler.class)) {
      RouteHandler routeHandler = handler.getAnnotation(RouteHandler.class);
      restUrl = restUrl + routeHandler.value();
    }
    Object             instance   = handler.newInstance();
    Method[]           methods    = handler.getMethods();
    Comparator<Method> comparator = (m1, m2) -> {
                                    RouteMapping mapping1 = m1.getAnnotation(RouteMapping.class);
                                    RouteMapping mapping2 = m2.getAnnotation(RouteMapping.class);
                                    return Integer.compare(mapping2.order(), mapping1.order());
                                  };

    List<Method> methodList = Stream.of(methods)
        .filter(
            method -> method.isAnnotationPresent(RouteMapping.class)
        )
        .sorted(comparator)
        .collect(Collectors.toList());
    for (Method method : methodList) {
      if (method.isAnnotationPresent(RouteMapping.class)) {
        RouteMapping mapping     = method.getAnnotation(RouteMapping.class);
        RouteMethod  routeMethod = mapping.method();
        String       routeUrl;
        if (mapping.value().startsWith("/:")) {
          routeUrl = (method.getName() + mapping.value());
        } else {
          routeUrl = (mapping.value().endsWith(method.getName()) ? mapping.value() : (mapping.isCover() ? mapping.value() : mapping.value() + method.getName()));
          if (routeUrl.startsWith("/")) {
            routeUrl = routeUrl.substring(1);
          }
        }
        String url;
        if (!restUrl.endsWith("/")) {
          url = restUrl.concat("/" + routeUrl);
        } else {
          url = restUrl.concat(routeUrl);
        }
        
        Handler<RoutingContext> methodHandler = (Handler<RoutingContext>) method.invoke(instance);
        String                  mineType      = mapping.mimeType();
        LOGGER.info("Register New Handler -> {}:{}:{}", routeMethod, url, mineType);
        Route route;
        switch (routeMethod) {
          case POST:
            route = router.post(url);
            break;
          case PUT:
            route = router.put(url);
            break;
          case DELETE:
            route = router.delete(url);
            break;
          case ROUTE:
            route = router.route(url);
            break;
          case GET: // fall through,注意后面都没有`break;`语句
            route = router.get(url);
          case OPTIONS:
            route = router.options(url);
          case PATCH:
            route = router.patch(url);
          case TRACE:
            route = router.trace(url);
          default:
            route = router.get(url);
            break;
        }
        if (ParamUtil.isNotBlank(mineType)) {
          route.consumes(mineType);
        }
        route.handler(methodHandler);
      }
    }
  }

}
