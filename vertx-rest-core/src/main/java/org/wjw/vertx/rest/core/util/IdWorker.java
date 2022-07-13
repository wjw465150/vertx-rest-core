package org.wjw.vertx.rest.core.util;

import java.util.UUID;

/**
 * 生成雪花分布式ID 和 UUID 的帮助类.
 */
public class IdWorker {

    /** 主机和进程的机器码. */
    private static SnowflakeIdWorker SNOWFLAKE_WORKER = new SnowflakeIdWorker();

    /**
     * Gets the id.
     *
     * @return the id
     */
    public static long getSnowFlakeId() {
        return SNOWFLAKE_WORKER.nextId();
    }

    /**
     * Gets the id str.
     *
     * @return the id str
     */
    public static String getSnowFlakeIdStr() {
        return String.valueOf(SNOWFLAKE_WORKER.nextId());
    }

    /**
     * <p>
     * 有参构造器
     * </p>.
     *
     * @param workerId     工作机器ID
     * @param datacenterId 数据中心ID
     */
    public static void initSequence(long workerId, long datacenterId) {
      SNOWFLAKE_WORKER = new SnowflakeIdWorker(workerId, datacenterId);
    }

    /**
     * <p>
     * 获取去掉"-" UUID
     * </p>.
     *
     * @return the 32uuid
     */
    public static synchronized String get32UUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
