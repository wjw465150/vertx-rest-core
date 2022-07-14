package org.wjw.vertx.rest.demo.service;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface SensorDataService {
  void valueFor(String sensorId, Handler<AsyncResult<JsonObject>> handler);

  void average(Handler<AsyncResult<JsonObject>> handler);
} 