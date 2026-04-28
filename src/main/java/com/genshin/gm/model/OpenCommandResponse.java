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
        // 兼容不同版本的 Grasscutter OpenCommand 插件
        // 官方文档: retcode=200 表示成功
        // 某些版本: retcode=0 表示成功
        return retcode == 0 || retcode == 200;
    }
}
