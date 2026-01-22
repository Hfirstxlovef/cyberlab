package org.cyberlab.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * 网络连接测试工具类
 * 用于测试Docker API连接和网络连通性
 */
public class NetworkTestUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkTestUtil.class);
    
    /**
     * 测试TCP端口连通性
     */
    public static boolean testTcpConnection(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (SocketTimeoutException e) {
            logger.warn("TCP connection timeout: {}:{} ({}ms)", host, port, timeoutMs);
            return false;
        } catch (IOException e) {
            logger.warn("TCP connection failed: {}:{} - {}", host, port, e.getMessage());
            return false;
        }
    }
    
    /**
     * 测试Docker API连通性
     */
    public static boolean testDockerApi(String host, int port) {
        try {
            // 首先测试TCP连接
            if (!testTcpConnection(host, port, 5000)) {
                return false;
            }
            
            // 尝试访问Docker API版本端点
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("curl", "-s", "--connect-timeout", "5", 
                      String.format("http://%s:%d/version", host, port));
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            
            if (finished && process.exitValue() == 0) {
                String response = output.toString();
                if (response.contains("\"ApiVersion\"") || response.contains("\"Version\"")) {
                    return true;
                } else {
                    logger.warn("Docker API response abnormal: {}:{} - {}", host, port, response);
                    return false;
                }
            } else {
                logger.warn("Docker API request failed: {}:{}", host, port);
                return false;
            }

        } catch (Exception e) {
            logger.error("Docker API test exception: {}:{} - {}", host, port, e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用ping测试网络连通性
     */
    public static boolean pingHost(String host, int timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            
            // 根据操作系统选择ping命令
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                pb.command("ping", "-n", "1", "-w", String.valueOf(timeoutSeconds * 1000), host);
            } else {
                pb.command("ping", "-c", "1", "-W", String.valueOf(timeoutSeconds), host);
            }
            
            Process process = pb.start();
            boolean finished = process.waitFor(timeoutSeconds + 2, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                return true;
            } else {
                logger.warn("Ping failed: {} (exit code: {})", host,
                          finished ? process.exitValue() : "timeout");
                return false;
            }

        } catch (Exception e) {
            logger.error("Ping test exception: {} - {}", host, e.getMessage());
            return false;
        }
    }
    
    /**
     * 综合网络诊断
     */
    public static NetworkDiagnosticResult diagnoseConnection(String host, int port) {
        NetworkDiagnosticResult result = new NetworkDiagnosticResult();
        result.setHost(host);
        result.setPort(port);
        result.setTimestamp(System.currentTimeMillis());

        // 1. Ping测试
        result.setPingSuccess(pingHost(host, 5));

        // 2. TCP连接测试
        result.setTcpConnectionSuccess(testTcpConnection(host, port, 5000));

        // 3. Docker API测试
        if (result.isTcpConnectionSuccess()) {
            result.setDockerApiSuccess(testDockerApi(host, port));
        } else {
            result.setDockerApiSuccess(false);
        }

        // 4. 综合评估
        if (result.isDockerApiSuccess()) {
            result.setOverallStatus("SUCCESS");
            result.setRecommendation("连接正常，可以使用Docker API");
        } else if (result.isTcpConnectionSuccess()) {
            result.setOverallStatus("PARTIAL");
            result.setRecommendation("TCP连接正常但Docker API不可用，请检查Docker daemon配置");
        } else if (result.isPingSuccess()) {
            result.setOverallStatus("NETWORK_ISSUE");
            result.setRecommendation("网络连通但端口不可达，请检查防火墙和Docker端口配置");
        } else {
            result.setOverallStatus("FAILED");
            result.setRecommendation("网络不通，请检查IP地址和网络配置");
        }

        return result;
    }
    
    /**
     * 测试多个Docker节点的连通性
     */
    public static void diagnoseMultipleNodes(String[][] nodeConfigs) {
        for (String[] config : nodeConfigs) {
            if (config.length >= 2) {
                String host = config[0];
                int port = Integer.parseInt(config[1]);
                diagnoseConnection(host, port);
            }
        }
    }
    
    /**
     * 网络诊断结果类
     */
    public static class NetworkDiagnosticResult {
        private String host;
        private int port;
        private long timestamp;
        private boolean pingSuccess;
        private boolean tcpConnectionSuccess;
        private boolean dockerApiSuccess;
        private String overallStatus;
        private String recommendation;
        
        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public boolean isPingSuccess() { return pingSuccess; }
        public void setPingSuccess(boolean pingSuccess) { this.pingSuccess = pingSuccess; }
        
        public boolean isTcpConnectionSuccess() { return tcpConnectionSuccess; }
        public void setTcpConnectionSuccess(boolean tcpConnectionSuccess) { this.tcpConnectionSuccess = tcpConnectionSuccess; }
        
        public boolean isDockerApiSuccess() { return dockerApiSuccess; }
        public void setDockerApiSuccess(boolean dockerApiSuccess) { this.dockerApiSuccess = dockerApiSuccess; }
        
        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
        
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }
}