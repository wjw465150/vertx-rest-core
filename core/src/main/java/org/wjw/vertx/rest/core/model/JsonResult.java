package org.wjw.vertx.rest.core.model;


import java.io.Serializable;

/**
 * JSON格式的响应消息类.
 */
public class JsonResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The success code. */
    public static int SUCCESS_CODE = 1;

    /** The fail code. */
    public static int FAIL_CODE = 0;

    /** The Constant SUCCESS_MESSAGE. */
    public static final String SUCCESS_MESSAGE = "SUCCESS";

    /** The Constant FAIL_MESSAGE. */
    public static final String FAIL_MESSAGE = "FAILED";

    /** The code. */
    private int code = SUCCESS_CODE;//状态

    /** The msg. */
    private String msg = SUCCESS_MESSAGE;//消息

    /** The data. */
    private Object data;

    /**
     * Instantiates a new json result.
     */
    public JsonResult() {
    }

    /**
     * Instantiates a new json result.
     *
     * @param data the data
     */
    public JsonResult(Object data) {
        this.data = data;
    }

    /**
     * Instantiates a new json result.
     *
     * @param code the code
     * @param msg the msg
     * @param data the data
     */
    public JsonResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * Gets the failed result.
     *
     * @return the failed result
     */
    public static JsonResult getFailedResult() {
        return new JsonResult(FAIL_CODE, FAIL_MESSAGE, null);
    }

    /**
     * Gets the code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * Sets the code.
     *
     * @param code the code
     * @return the json result
     */
    public JsonResult setCode(int code) {
        this.code = code;
        return this;
    }

    /**
     * Gets the msg.
     *
     * @return the msg
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Sets the msg.
     *
     * @param msg the msg
     * @return the json result
     */
    public JsonResult setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    /**
     * Gets the data.
     *
     * @return the data
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets the data.
     *
     * @param data the data
     * @return the json result
     */
    public JsonResult setData(Object data) {
        this.data = data;
        return this;
    }
}
