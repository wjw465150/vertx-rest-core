package org.wjw.vertx.rest.core.util;

import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * 参数 工具类
 */
public final class ParamUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(ParamUtil.class);

  /**
   * 请求参数转换
   */
  public static JsonObject getRequestParams(RoutingContext ctx) {
    JsonObject params = new JsonObject();
    try {
      params = getRequestParams(ctx.request().params());
      //ip
      //            params.put("serverIp", ctx.request().localAddress().host());
      //            params.put("clientIp", ctx.request().remoteAddress().host());

      JsonObject param = ctx.getBodyAsJson();
      if (param != null) {
        LOGGER.debug("参数来源body,为JSON....");
        Map<String, Object> paramMap = param.getMap();
        Iterator<String>    iterator = paramMap.keySet().iterator();
        while (iterator.hasNext()) {
          String key = iterator.next();
          if (paramMap.get(key) instanceof String) {
            params.put(key, (String) paramMap.get(key));
          } else if ((paramMap.get(key) instanceof JsonArray) || (paramMap.get(key) instanceof List)) {
            JsonArray arry = new JsonArray();
            if (paramMap.get(key) instanceof List) {
              arry = new JsonArray((List) paramMap.get(key));
            } else {
              arry = (JsonArray) paramMap.get(key);
            }
            params.put(key, new JsonArray(arry.encode()));
          } else if ((paramMap.get(key) instanceof JsonObject) || (paramMap.get(key) instanceof Map)) {
            JsonObject sub1 = new JsonObject();
            if (paramMap.get(key) instanceof JsonObject) {
              sub1 = (JsonObject) paramMap.get(key);
            } else {
              sub1 = new JsonObject((Map) paramMap.get(key));
            }
            params.put(key, new JsonObject(sub1.encode()));
          } else {
            params.put(key, paramMap.get(key));
          }
        }
      }
    } catch (Exception e) {
      LOGGER.debug("请求body体中无参数!");
    }
    return params;
  }

  /**
   * 根据http get请求，获取URL参数
   */
  public static JsonObject getQueryMap(String query) {
    String[]   params = query.split("&");
    JsonObject map    = new JsonObject();
    for (String param : params) {
      String name  = param.split("=")[0];
      String value = "";
      try {
        value = URLDecoder.decode(param.split("=")[1], "UTF-8");
      } catch (Exception e) {
      }
      map.put(name, value);
    }
    return getParamPage(map);
  }

  /**
   * 请求参数转换
   */
  private static JsonObject getRequestParams(MultiMap paramMap) {
    JsonObject param = new JsonObject();
    if (paramMap != null) {
      Iterator iter = paramMap.entries().iterator();
      while (iter.hasNext()) {
        Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iter.next();
        if (entry.getValue() instanceof String) {
          if (param.containsKey(entry.getKey())) {//多值
            if (!(param.getValue(entry.getKey()) instanceof JsonArray)) {
              List<String> arry = (List<String>) paramMap.getAll((String) entry.getKey());
              for (int i = 0; i < arry.size(); i++) {
                arry.set(i, arry.get(i));
              }
              param.put(entry.getKey(), arry);
              //直接转
              continue;
            }
          } else {
            param.put(entry.getKey(), (String) entry.getValue());
          }
        } else {
          param.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return getParamPage(param);
  }

  /**
   * 默认处理分页
   */
  private static JsonObject getParamPage(JsonObject params) {
    if (params != null) {
      if (!params.containsKey("size")) {
        params.put("size", 10);
      } else {
        if (params.getValue("size") instanceof String) {
          if (ParamUtil.isBlank(params.getString("size"))) {
            params.put("size", 10);
          } else {
            params.put("size", Integer.valueOf(params.getString("size")));
          }
        } else if (params.getValue("size") instanceof Number) {
          params.put("size", Integer.valueOf(params.getValue("size").toString()));
        } else {
          params.put("size", 10);
        }
      }
      if (!params.containsKey("current")) {
        params.put("current", 1);
      } else {
        if (params.getValue("current") instanceof String) {
          if (ParamUtil.isBlank(params.getString("current"))) {
            params.put("current", 1);
          } else {
            params.put("current", Integer.valueOf(params.getString("current")));
          }
        } else if (params.getValue("current") instanceof Number) {
          params.put("current", Integer.valueOf(params.getValue("current").toString()));
        } else {
          params.put("current", 1);
        }
      }
    }
    return params;
  }

  public static boolean isNotEmpty(final CharSequence cs) {
    return !isEmpty(cs);
  }

  public static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.length() == 0;
  }

  public static boolean isNotBlank(final CharSequence cs) {
    return !isBlank(cs);
  }

  public static boolean isBlank(final CharSequence cs) {
    final int strLen = length(cs);
    if (strLen == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static int length(final CharSequence cs) {
    return cs == null ? 0 : cs.length();
  }

}
