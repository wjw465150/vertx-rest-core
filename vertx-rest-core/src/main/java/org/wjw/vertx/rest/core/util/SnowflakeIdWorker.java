package org.wjw.vertx.rest.core.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分布式雪花ID类 
 */
public class SnowflakeIdWorker {
  private static final Logger LOG = LoggerFactory.getLogger(SnowflakeIdWorker.class);

  // ==============================Fields===========================================
  /**
   * 开始时间截 (2021-01-01)
   */
  private final long twepoch = 1609459200000L;

  /**
   * 机器id所占的位数
   */
  private final long workerIdBits = 5L;

  /**
   * 数据中心id所占的位数
   */
  private final long datacenterIdBits = 5L;

  /**
   * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
   */
  private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

  /**
   * 支持的最大数据中心id，结果是31
   */
  private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

  /**
   * 序列在id中占的位数
   */
  private final long sequenceBits = 12L;

  /**
   * 机器id向左移12位
   */
  private final long workerIdShift = sequenceBits;

  /**
   * 数据中心id向左移17位(12+5)
   */
  private final long datacenterIdShift = sequenceBits + workerIdBits;

  /**
   * 时间截向左移22位(5+5+12)
   */
  private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

  /**
   * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
   */
  private final long sequenceMask = -1L ^ (-1L << sequenceBits);

  /**
   * 工作机器ID(0~31)
   */
  private long workerId;

  /**
   * 数据中心ID(0~31)
   */
  private long datacenterId;

  /**
   * 毫秒内序列(0~4095)
   */
  private long sequence = 0L;

  /**
   * 上次生成ID的时间截
   */
  private long lastTimestamp = -1L;

  //==============================Constructors=====================================
  public SnowflakeIdWorker() {
    this.datacenterId = getDatacenterId(maxDatacenterId);
    this.workerId = getMaxWorkerId(datacenterId, maxWorkerId);
  }

  public SnowflakeIdWorker(long workerId, long datacenterId) {
    if (workerId > maxWorkerId || workerId < 0) {
      throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
    }
    if (datacenterId > maxDatacenterId || datacenterId < 0) {
      throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
    }

    this.workerId = workerId;
    this.datacenterId = datacenterId;
  }

  public String nextIdString() {
    long id = nextId();

    return String.valueOf(id);
  }

  /**
   * 获得下一个ID (该方法是线程安全的)
   *
   * @return SnowflakeId
   */
  public synchronized long nextId() {
    long timestamp = timeGen();

    //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
    if (timestamp < lastTimestamp) {
      throw new RuntimeException(
          String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
    }

    //如果是同一时间生成的，则进行毫秒内序列
    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & sequenceMask;
      //毫秒内序列溢出
      if (sequence == 0) {
        //阻塞到下一个毫秒,获得新的时间戳
        timestamp = tilNextMillis(lastTimestamp);
      }
    }
    //时间戳改变，毫秒内序列重置
    else {
      sequence = 0L;
    }

    //上次生成ID的时间截
    lastTimestamp = timestamp;

    //移位并通过或运算拼到一起组成64位的ID
    return ((timestamp - twepoch) << timestampLeftShift) //
        | (datacenterId << datacenterIdShift) //
        | (workerId << workerIdShift) //
        | sequence;
  }

  /**
   * 阻塞到下一个毫秒，直到获得新的时间戳
   *
   * @param lastTimestamp 上次生成ID的时间截
   * @return 当前时间戳
   */
  protected long tilNextMillis(long lastTimestamp) {
    long timestamp = timeGen();
    while (timestamp <= lastTimestamp) {
      timestamp = timeGen();
    }
    return timestamp;
  }

  /**
   * 返回以毫秒为单位的当前时间
   *
   * @return 当前时间(毫秒)
   */
  protected long timeGen() {
    return System.currentTimeMillis();
  }

  /**
   * <p>
   * 获取 maxWorkerId
   * </p>
   */
  protected static long getMaxWorkerId(long datacenterId, long maxWorkerId) {
    StringBuilder mpid = new StringBuilder();
    mpid.append(datacenterId);
    String name = ManagementFactory.getRuntimeMXBean().getName();
    if (ParamUtil.isNotEmpty(name)) {
      /*
       * GET jvmPid
       */
      mpid.append(name.split("@")[0]);
    }
    /*
     * MAC + PID 的 hashcode 获取16个低位
     */
    return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
  }

  /**
   * <p>
   * 数据中心id部分
   * </p>
   */
  protected static long getDatacenterId(long maxDatacenterId) {
    long id = 0L;
    try {
      InetAddress      ip      = InetAddress.getLocalHost();
      NetworkInterface network = NetworkInterface.getByInetAddress(ip);
      if (network == null) {
        id = 1L;
      } else {
        byte[] mac = network.getHardwareAddress();
        if (null != mac) {
          id = ((0x000000FF & (long) mac[mac.length - 1]) | (0x0000FF00 & (((long) mac[mac.length - 2]) << 8))) >> 6;
          id = id % (maxDatacenterId + 1);
        }
      }
    } catch (Exception e) {
      LOG.warn(" getDatacenterId: " + e.getMessage());
    }
    return id;
  }
}
