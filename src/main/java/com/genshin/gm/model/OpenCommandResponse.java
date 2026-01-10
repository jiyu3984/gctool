package com.genshin.gm.model;

/**
 * OpenCommand API 响应模型
 */
public class OpenCommandResponse {
    private int retcode = 200;
    private String message = "Success";
    private Object data;

    public OpenCommandResponse() {
    }

    public int getRetcode() {
        return retcode;
    }

    public void setRetcode(int retcode) {
        this.retcode = retcode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return retcode == 200;
    }
}
