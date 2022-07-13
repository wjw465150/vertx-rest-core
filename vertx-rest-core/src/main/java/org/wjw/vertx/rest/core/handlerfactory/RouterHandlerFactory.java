/*
 * author: @wjw
 * date:   2022年7月11日 上午10:53:00
 * note: 
 */
package org.wjw.vertx.rest.core.handlerfactory;

import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

/**
 * Router 对象创建.
 * <p>
 * 根据RouteHandler和RouteMapping注解通过反射机制创建Router的Handler类并注册到Router上
 */
public class RouterHandlerFactory {
  /** The Constant LOGGER. */
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  // 需要扫描注册的Router路径
  private static volatile Reflections reflections;

  // 默认api前缀
  private static final String GATEWAY_PREFIX = "/";

  private volatile String gatewayPrefix = GATEWAY_PREFIX;

  private final Vertx vertx;

  /**
   * Instantiates a new router handler factory.
   *
   * @param routerScanPackage the router scan package
   */
  public RouterHandlerFactory(Vertx vertx,String routerScanPackage) {
    Objects.requireNonNull(routerScanPackage, "The router package address scan is empty.");
    
    this.vertx = vertx;
    reflections = ReflectionUtil.getReflections(routerScanPackage);
  }

  /**
   * Instantiates a new router handler factory.
   *
   * @param routerScanPackages the router scan packages
   */
  public RouterHandlerFactory(Vertx vertx,List<String> routerScanPackages) {
    Objects.requireNonNull(routerScanPackages, "The router package address scan is empty.");
    
    this.vertx = vertx;
    reflections = ReflectionUtil.getReflections(routerScanPackages);
  }

  /**
   * Instantiates a new router handler factory.
   *
   * @param routerScanPackages the router scan packages
   * @param gatewayPrefix the gateway prefix
   */
  public RouterHandlerFactory(Vertx vertx,String routerScanPackages, String gatewayPrefix) {
    Objects.requireNonNull(routerScanPackages, "The router package address scan is empty.");
    
    this.vertx = vertx;
    this.gatewayPrefix = gatewayPrefix;
    reflections = ReflectionUtil.getReflections(routerScanPackages);
  }

  /**
   * 开始扫描并注册handler.
   *
   * @return the router
   */
  public Router createRouter() {
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

    //添加跨域的方法
    Set<HttpMethod> method = new HashSet<HttpMethod>() {
      {
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
        registerNewHandler(router, handler);
      }
    } catch (Exception e) {
      LOGGER.error("Manually Register Handler Fail，Error details：" + e.getMessage());
      throw new java.lang.RuntimeException(e);
    }
    return router;
  }

  /**
   * Register new handler to router.
   *
   * @param router the router
   * @param handler the handler
   * @throws Exception the exception
   */
  private void registerNewHandler(Router router, Class<?> handler) throws Exception {
    String root = gatewayPrefix;
    if (!root.startsWith("/")) {
      root = "/" + root;
    }
    if (!root.endsWith("/")) {
      root = root + "/";
    }
    if (handler.isAnnotationPresent(RouteHandler.class)) {
      RouteHandler routeHandler = handler.getAnnotation(RouteHandler.class);
      root = root + routeHandler.value();
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
        if (!root.endsWith("/")) {
          url = root.concat("/" + routeUrl);
        } else {
          url = root.concat(routeUrl);
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
