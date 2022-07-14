package org.wjw.vertx.rest.demo.rest;

import org.wjw.vertx.rest.core.annotaions.RouteHandler;
import org.wjw.vertx.rest.core.annotaions.RouteMapping;
import org.wjw.vertx.rest.core.annotaions.RouteMethod;
import org.wjw.vertx.rest.core.model.JsonResult;
import org.wjw.vertx.rest.core.util.ServiceUtil;
import org.wjw.vertx.rest.core.util.ParamUtil;
import org.wjw.vertx.rest.core.util.RestApiUtil;

import org.wjw.vertx.rest.demo.service.SensorDataService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@RouteHandler(value = "SensorDataApi")
public class SensorDataApi {

  @RouteMapping(value = "/valueFor/:sensorId",
                method = RouteMethod.GET)
  public Handler<RoutingContext> valueFor() {
    //方法引用的方式
    return this::valueForHandler;
  }

  private void valueForHandler(RoutingContext ctx) {
    String sensorId = ctx.pathParam("sensorId");
    if (ParamUtil.isBlank(sensorId)) {
      RestApiUtil.sendError(400, ctx);
    } else {
      SensorDataService orderService = ServiceUtil.getServiceInstance(ctx.vertx(), SensorDataService.class);

      orderService.valueFor(sensorId, ar -> {
        if (ar.succeeded()) {
          JsonObject product = ar.result();
          RestApiUtil.fireJsonResponse(ctx, new JsonResult(product));
        } else {
          RestApiUtil.fireErrorJsonResponse(ctx, ar.cause().getMessage());
        }
      });
    }
  }

  @RouteMapping(value = "/average",
                method = RouteMethod.GET)
  public Handler<RoutingContext> average() {
    //lambda方式
    return ctx -> {
      SensorDataService orderService = ServiceUtil.getServiceInstance(ctx.vertx(), SensorDataService.class);
      orderService.average(ar -> {
        if (ar.succeeded()) {
          JsonObject product = ar.result();
          RestApiUtil.fireJsonResponse(ctx, new JsonResult(product));
        } else {
          RestApiUtil.fireErrorJsonResponse(ctx, ar.cause().getMessage());
        }
      });
    };
  }

}
