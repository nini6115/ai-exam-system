package com.aiexam.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
public class AjaxResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：200成功，其他失败 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 数据 */
    private T data;

    public static <T> AjaxResult<T> success() {
        return success(null);
    }

    public static <T> AjaxResult<T> success(T data) {
        AjaxResult<T> result = new AjaxResult<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> AjaxResult<T> error(String msg) {
        return error(500, msg);
    }

    public static <T> AjaxResult<T> error(int code, String msg) {
        AjaxResult<T> result = new AjaxResult<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
