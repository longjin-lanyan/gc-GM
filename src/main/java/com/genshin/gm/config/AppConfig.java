package com.genshin.gm.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 应用配置类
 */
public class AppConfig {
    private FrontendConfig frontend;
    private GrasscutterConfig grasscutter;
    private MySQLConfig mysql;

    public FrontendConfig getFrontend() {
        return frontend;
    }

    public void setFrontend(FrontendConfig frontend) {
        this.frontend = frontend;
    }

    public GrasscutterConfig getGrasscutter() {
        return grasscutter;
    }

    public void setGrasscutter(GrasscutterConfig grasscutter) {
        this.grasscutter = grasscutter;
    }

    public MySQLConfig getMysql() {
        return mysql;
    }

    public void setMysql(MySQLConfig mysql) {
        this.mysql = mysql;
    }

    public static class FrontendConfig {
        private String host = "localhost";
        private int port = 8080;
        private boolean autoOpen = true;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isAutoOpen() {
            return autoOpen;
        }

        public void setAutoOpen(boolean autoOpen) {
            this.autoOpen = autoOpen;
        }

        @JsonIgnore
        public String getUrl() {
            return "http://" + host + ":" + port;
        }
    }

    public static class GrasscutterConfig {
        private String serverUrl = "http://127.0.0.1:443";
        private String apiPath = "/opencommand/api";
        private String consoleToken = "";
        private String adminToken = "";
        private int timeout = 10000;

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public String getApiPath() {
            return apiPath;
        }

        public void setApiPath(String apiPath) {
            this.apiPath = apiPath;
        }

        public String getConsoleToken() {
            return consoleToken;
        }

        public void setConsoleToken(String consoleToken) {
            this.consoleToken = consoleToken;
        }

        public String getAdminToken() {
            return adminToken;
        }

        public void setAdminToken(String adminToken) {
            this.adminToken = adminToken;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        @JsonIgnore
        public String getFullUrl() {
            return serverUrl + apiPath;
        }
    }

    public static class MySQLConfig {
        private String host = "localhost";
        private int port = 3306;
        private String database = "genshin_gm";
        private String username = "root";
        private String password = "";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @JsonIgnore
        public String getJdbcUrl() {
            return "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4";
        }
    }
}
