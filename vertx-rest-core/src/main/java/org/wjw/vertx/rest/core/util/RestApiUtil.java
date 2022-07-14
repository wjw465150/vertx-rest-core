package org.wjw.vertx.rest.core.util;

import org.wjw.vertx.rest.core.model.JsonResult;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * 此类定义了几个统一返回JSON格式字符串的响应.
 */
public abstract class RestApiUtil {

  /**
   * Fire json response.
   *
   * @param ctx the ctx
   * @param jsonResult the json result
   */
  public static void fireJsonResponse(RoutingContext ctx, JsonResult jsonResult) {
    JsonObject jsonObject = JsonObject.mapFrom(jsonResult);
    ctx.response().putHeader("content-type", "application/json; charset=utf-8").setStatusCode(200).end(jsonObject.encode());
  }

  /**
   * Fire error json response.
   *
   * @param ctx the ctx
   * @param msg the msg
   */
  public static void fireErrorJsonResponse(RoutingContext ctx, String msg) {
    JsonResult jsonResult = new JsonResult().setCode(JsonResult.FAIL_CODE).setMsg(ParamUtil.isBlank(msg) ? JsonResult.FAIL_MESSAGE : msg);
    JsonObject jsonObject = JsonObject.mapFrom(jsonResult);
    ctx.response().putHeader("content-type", "application/json; charset=utf-8").setStatusCode(200).end(jsonObject.encode());
  }

  /**
   * Fire text response.
   *
   * @param ctx the ctx
   * @param text the text
   */
  public static void fireTextResponse(RoutingContext ctx, String text) {
    ctx.response().putHeader("content-type", "text/html; charset=utf-8").end(text);
  }

  /**
   * Send error.
   *
   * @param statusCode the status code
   * @param ctx the ctx
   */
  public static void sendError(int statusCode, RoutingContext ctx) {
    ctx.response().setStatusCode(statusCode).end();
  }
}
