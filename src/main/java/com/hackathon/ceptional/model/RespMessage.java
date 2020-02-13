package com.hackathon.ceptional.model;

import lombok.Getter;
import lombok.Setter;

/**
 * common definition for response message
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/12
 */
@Setter
@Getter
public class RespMessage {
    private int code;
    private String msg;

    /**
     * detailed messages, %s to set variable message
     */
    public static RespMessage SUCCESS = new RespMessage(200, "SUCCESS");
    public static RespMessage SERVER_ERROR = new RespMessage(100, "系统异常：%s");
    public static RespMessage BIND_ERROR = new RespMessage(101, "(绑定异常)参数校验异常：%s");
    public static RespMessage SESSION_ERROR = new RespMessage(102, "没有SESSION！");
    public static RespMessage REQUEST_ERROR = new RespMessage(103,"非法请求！");
    public static RespMessage REQUEST_OVER_LIMIT = new RespMessage(104,"请求次数过多！");

    private RespMessage( ) {
    }
    private RespMessage( int code,String msg ) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 不定参的构造函数
     * @param args - mutable parameters
     * @return RespMessage
     */
    public RespMessage fillArgs(Object... args) {
        int code = this.code;
        String message = String.format(this.msg, args);
        return new RespMessage(code, message);
    }
    @Override
    public String toString() {
        return "RespMessage [code=" + code + ", msg=" + msg + "]";
    }
}
