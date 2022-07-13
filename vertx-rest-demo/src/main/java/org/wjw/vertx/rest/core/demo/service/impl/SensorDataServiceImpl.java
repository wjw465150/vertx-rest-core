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
