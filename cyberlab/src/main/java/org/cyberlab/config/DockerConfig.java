package org.cyberlab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cyberlab.docker")
public class DockerConfig {
    
    /**
     * Docker命令超时时间（秒）
     */
    private int commandTimeout = 30;
    
    /**
     * 是否启用Docker功能
     */
    private boolean enabled = true;
    
    /**
     * Docker socket路径（Linux/Mac）
     */
    private String socketPath = "/var/run/docker.sock";
    
    /**
     * Docker主机地址（Windows或远程Docker）
     */
    private String host = "localhost";
    
    /**
     * Docker API端口
     */
    private int port = 2375;
    
    /**
     * 是否使用TLS
     */
    private boolean tlsEnabled = false;
    
    /**
     * 是否启用远程Docker连接
     */
    private boolean remoteEnabled = false;
    
    /**
     * 允许操作的容器名称前缀（安全限制）
     */
    private String[] allowedContainerPrefixes = {"cyberlab-", "drill-", "vuln-"};

    // Getters and Setters
    public int getCommandTimeout() {
        return commandTimeout;
    }

    public void setCommandTimeout(int commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSocketPath() {
        return socketPath;
    }

    public void setSocketPath(String socketPath) {
        this.socketPath = socketPath;
    }

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

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }
    
    public boolean isRemoteEnabled() {
        return remoteEnabled;
    }

    public void setRemoteEnabled(boolean remoteEnabled) {
        this.remoteEnabled = remoteEnabled;
    }

    public String[] getAllowedContainerPrefixes() {
        return allowedContainerPrefixes;
    }

    public void setAllowedContainerPrefixes(String[] allowedContainerPrefixes) {
        this.allowedContainerPrefixes = allowedContainerPrefixes;
    }
}