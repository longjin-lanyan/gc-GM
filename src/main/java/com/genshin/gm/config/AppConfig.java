package com.genshin.gm.config;

/**
 * 应用配置类
 */
public class AppConfig {
    private FrontendConfig frontend;
    private GrasscutterConfig grasscutter;
    private MongoDBConfig mongodb;

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

    public MongoDBConfig getMongodb() {
        return mongodb;
    }

    public void setMongodb(MongoDBConfig mongodb) {
        this.mongodb = mongodb;
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

        public String getFullUrl() {
            return serverUrl + apiPath;
        }
    }

    public static class MongoDBConfig {
        private String host = "localhost";
        private int port = 27017;
        private String database = "genshin_gm";
        private String username = "";
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

        public String getConnectionString() {
            StringBuilder sb = new StringBuilder("mongodb://");
            if (username != null && !username.isEmpty()) {
                sb.append(username);
                if (password != null && !password.isEmpty()) {
                    sb.append(":").append(password);
                }
                sb.append("@");
            }
            sb.append(host).append(":").append(port).append("/").append(database);
            return sb.toString();
        }
    }
}
