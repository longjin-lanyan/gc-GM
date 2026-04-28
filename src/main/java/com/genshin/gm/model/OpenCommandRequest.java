package com.genshin.gm.model;

/**
 * OpenCommand API 请求模型
 */
public class OpenCommandRequest {
    private String token = "";
    private String action = "";
    private String server = "";
    private Object data = null;

    public OpenCommandRequest() {
    }

    public OpenCommandRequest(String action) {
        this.action = action;
    }

    public OpenCommandRequest(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "OpenCommandRequest{" +
                "token='" + (token != null && !token.isEmpty() ? "***" : "") + '\'' +
                ", action='" + action + '\'' +
                ", server='" + server + '\'' +
                ", data=" + data +
                '}';
    }
}
