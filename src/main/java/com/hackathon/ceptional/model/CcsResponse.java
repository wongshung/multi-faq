package com.hackathon.ceptional.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


/**
 * 中控系统对接子 Bot 接口 返回格式
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/12
 */
@Setter
@Getter
public class CcsResponse<T> {

    private int response_code;

    private String response_message;

    private List<T> results = new ArrayList<>();

    public CcsResponse() {
    }

    /**
     * 成功时候的调用
     *
     * @param results results
     * @param <T>     t
     * @return Result
     */
    public static <T> CcsResponse<T> success(T results) {
        return new CcsResponse<>(results);
    }

    /**
     * 失败时候的调用
     *
     * @param codeMsg codeMsg
     * @param <T>     t
     * @return Result
     */
    public static <T> CcsResponse<T> error(RespMessage codeMsg) {
        return new CcsResponse<T>(codeMsg);
    }

    /**
     * 成功的构造函数
     *
     * @param data data
     */
    private CcsResponse(T data) {
        this.response_code = 200;
        this.response_message = "success";
        this.results.add(data);
    }

    public CcsResponse(int code, String msg) {
        this.response_code = code;
        this.response_message = msg;
    }

    /**
     * 失败的构造函数
     *
     * @param codeMsg codeMsg
     */
    private CcsResponse(RespMessage codeMsg) {
        if (codeMsg != null) {
            this.response_code = codeMsg.getCode();
            this.response_message = codeMsg.getMsg();
        }
    }

    @Override
    public String toString() {
        return "Result{" +
                "responseCode=" + response_code +
                ", responseMessage='" + response_message + '\'' +
                ", results=" + results +
                '}';
    }
}



