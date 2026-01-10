package com.genshin.gm.config;

/**
 * 应用配置类
 */
public class AppConfig {
    private FrontendConfig frontend;

    public FrontendConfig getFrontend() {
        return frontend;
    }

    public void setFrontend(FrontendConfig frontend) {
        this.frontend = frontend;
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
}
