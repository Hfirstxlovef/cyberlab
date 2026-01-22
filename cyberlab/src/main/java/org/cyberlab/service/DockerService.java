package org.cyberlab.service;

import org.cyberlab.config.DockerConfig;
import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.entity.ImageInfo;
import org.cyberlab.entity.HostNode;
import org.cyberlab.util.NetworkTestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DockerService {
    
    @Autowired
    private DockerConfig dockerConfig;
    
    @Autowired
    @Lazy
    private HostNodeService hostNodeService;

    /**
     * 构建Docker命令前缀（原有方法，用于本地或配置的远程连接）
     */
    private List<String> buildDockerCommand() {
        List<String> command = new ArrayList<>();
        
        if (dockerConfig.isRemoteEnabled()) {
            // 远程Docker连接
            command.add("docker");
            command.add("-H");
            command.add("tcp://" + dockerConfig.getHost() + ":" + dockerConfig.getPort());
        } else {
            // 本地Docker命令
            command.add("docker");
        }
        
        return command;
    }
    
    /**
     * 构建针对特定主机节点的Docker命令前缀
     */
    private List<String> buildDockerCommandForNode(HostNode hostNode) {
        List<String> command = new ArrayList<>();
        
        if (hostNode.isLocal()) {
            // 本地节点
            command.add("docker");
        } else {
            // 远程节点
            command.add("docker");
            command.add("-H");
            
            String dockerUrl;
            if (hostNode.getDockerTlsEnabled()) {
                dockerUrl = "tcp://" + hostNode.getHostIp() + ":" + hostNode.getDockerPort();
                // 如果启用了TLS，添加证书路径
                if (hostNode.getDockerCertPath() != null && !hostNode.getDockerCertPath().isEmpty()) {
                    command.add("--tlscert");
                    command.add(hostNode.getDockerCertPath());
                }
            } else {
                dockerUrl = "tcp://" + hostNode.getHostIp() + ":" + hostNode.getDockerPort();
            }
            
            command.add(dockerUrl);
        }
        
        return command;
    }

    /**
     * 执行Docker命令的通用方法（原有方法）
     */
    private ProcessBuilder createDockerProcess(String... dockerArgs) {
        List<String> command = buildDockerCommand();
        for (String arg : dockerArgs) {
            command.add(arg);
        }
        return new ProcessBuilder(command);
    }
    
    /**
     * 针对特定主机节点执行Docker命令
     */
    private ProcessBuilder createDockerProcessForNode(HostNode hostNode, String... dockerArgs) {
        List<String> command = buildDockerCommandForNode(hostNode);
        for (String arg : dockerArgs) {
            command.add(arg);
        }
        return new ProcessBuilder(command);
    }
    
    /**
     * 带重试机制的Docker命令执行
     * @param hostNode 主机节点
     * @param maxRetries 最大重试次数
     * @param dockerArgs Docker命令参数
     * @return 执行结果
     */
    private String executeDockerCommandWithRetry(HostNode hostNode, int maxRetries, String... dockerArgs) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 先检查节点连接状态
                if (!isNodeHealthy(hostNode)) {
                    if (attempt < maxRetries) {
                        Thread.sleep(1000 * attempt); // 递增等待时间
                        continue;
                    } else {
                        return "节点 " + hostNode.getHostIp() + " 连接失败，已重试 " + maxRetries + " 次";
                    }
                }
                
                ProcessBuilder processBuilder = createDockerProcessForNode(hostNode, dockerArgs);
                processBuilder.redirectErrorStream(true);
                
                Process process = processBuilder.start();
                
                // 设置超时时间
                boolean finished = process.waitFor(dockerConfig.getCommandTimeout(), TimeUnit.SECONDS);
                
                if (!finished) {
                    process.destroyForcibly();
                    if (attempt < maxRetries) {
                        Thread.sleep(1000 * attempt);
                        continue;
                    } else {
                        return "Docker命令执行超时";
                    }
                }
                
                int exitCode = process.exitValue();
                String output = readProcessOutput(process);
                
                if (exitCode == 0) {
                    return output;
                } else {
                    String error = "Docker命令执行失败 (退出码: " + exitCode + "): " + output;
                    if (attempt < maxRetries) {
                        Thread.sleep(1000 * attempt);
                        continue;
                    } else {
                        return error;
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Docker命令执行被中断";
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return "重试等待被中断";
                    }
                    continue;
                }
            }
        }
        
        return "Docker命令执行失败，已重试 " + maxRetries + " 次。最后错误: " + 
               (lastException != null ? lastException.getMessage() : "未知错误");
    }
    
    /**
     * 检查节点健康状态
     */
    private boolean isNodeHealthy(HostNode hostNode) {
        try {
            NetworkTestUtil.NetworkDiagnosticResult result = NetworkTestUtil.diagnoseConnection(
                hostNode.getHostIp(), 
                hostNode.getDockerPort()
            );
            return result.isDockerApiSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 读取进程输出并确保资源清理
     */
    private String readProcessOutput(Process process) {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            // 确保进程完全结束
            if (process.isAlive()) {
                process.waitFor(5, TimeUnit.SECONDS);
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
            
        } catch (IOException e) {
            return "读取输出失败: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return "读取输出被中断";
        } finally {
            // 清理进程资源
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
        return output.toString().trim();
    }

    /**
     * 重启Docker容器
     * @param containerId 容器ID
     * @return 操作结果
     */
    public String restartContainer(String containerId) {
        try {
            // 安全验证
            String securityResult = validateContainerSecurity(containerId);
            if (securityResult != null) {
                return securityResult;
            }

            // 执行Docker重启命令
            ProcessBuilder processBuilder = createDockerProcess("restart", containerId);
            Process process = processBuilder.start();
            
            // 等待命令执行完成，最多等待30秒
            boolean finished = process.waitFor(dockerConfig.getCommandTimeout(), TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return "容器重启超时";
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return "容器 " + containerId + " 重启成功";
            } else {
                String error = readProcessError(process);
                return "容器重启失败: " + error;
            }
            
        } catch (IOException e) {
            return "执行Docker命令失败: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "容器重启被中断";
        } catch (Exception e) {
            return "容器重启异常: " + e.getMessage();
        }
    }

    /**
     * 停止Docker容器
     * @param containerId 容器ID
     * @return 操作结果
     */
    public String stopContainer(String containerId) {
        try {
            // 安全验证
            String securityResult = validateContainerSecurity(containerId);
            if (securityResult != null) {
                return securityResult;
            }

            // 执行Docker停止命令
            ProcessBuilder processBuilder = createDockerProcess("stop", containerId);
            Process process = processBuilder.start();
            
            // 等待命令执行完成，最多等待30秒
            boolean finished = process.waitFor(dockerConfig.getCommandTimeout(), TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return "容器停止超时";
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return "容器 " + containerId + " 已停止";
            } else {
                String error = readProcessError(process);
                return "容器停止失败: " + error;
            }
            
        } catch (IOException e) {
            return "执行Docker命令失败: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "容器停止被中断";
        } catch (Exception e) {
            return "容器停止异常: " + e.getMessage();
        }
    }

    /**
     * 启动Docker容器
     * @param containerId 容器ID
     * @return 操作结果
     */
    public String startContainer(String containerId) {
        try {
            // 安全验证
            String securityResult = validateContainerSecurity(containerId);
            if (securityResult != null) {
                return securityResult;
            }

            ProcessBuilder processBuilder = createDockerProcess("start", containerId);
            Process process = processBuilder.start();
            
            boolean finished = process.waitFor(dockerConfig.getCommandTimeout(), TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return "容器启动超时";
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return "容器 " + containerId + " 启动成功";
            } else {
                String error = readProcessError(process);
                return "容器启动失败: " + error;
            }
            
        } catch (Exception e) {
            return "容器启动异常: " + e.getMessage();
        }
    }

    /**
     * 获取所有容器信息
     * @return 容器信息列表
     */
    public List<ContainerInfo> getAllContainers() {
        List<ContainerInfo> containers = new ArrayList<>();
        
        try {
            // 执行docker ps -a命令获取所有容器
            ProcessBuilder processBuilder = createDockerProcess("ps", "-a", "--format", 
                "{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}|{{.Ports}}");
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ContainerInfo container = parseContainerInfo(line);
                    if (container != null) {
                        // 获取容器资源使用情况
                        enrichContainerWithStats(container);
                        containers.add(container);
                    }
                }
            }
            
            // 如果Docker命令失败或超时，返回空列表
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ArrayList<>();
            }
            if (process.exitValue() != 0) {
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            return new ArrayList<>();
        }
        
        return containers;
    }

    /**
     * 获取所有容器的基本信息（不包含CPU/内存统计，性能优化版本）
     * 用于仅需要容器状态的场景（如dashboard统计），避免多次docker stats调用
     * @return 容器列表
     */
    public List<ContainerInfo> getAllContainersBasic() {
        List<ContainerInfo> containers = new ArrayList<>();

        try {
            // 执行docker ps -a命令获取所有容器
            ProcessBuilder processBuilder = createDockerProcess("ps", "-a", "--format",
                "{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}|{{.Ports}}");
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ContainerInfo container = parseContainerInfo(line);
                    if (container != null) {
                        // 不调用enrichContainerWithStats，仅设置默认值
                        container.setCpuUsage("N/A");
                        container.setMemoryUsage("N/A");
                        containers.add(container);
                    }
                }
            }

            // 如果Docker命令失败或超时，返回空列表
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ArrayList<>();
            }
            if (process.exitValue() != 0) {
                return new ArrayList<>();
            }

        } catch (Exception e) {
            return new ArrayList<>();
        }

        return containers;
    }

    /**
     * 获取单个容器的详细信息
     * @param containerId 容器ID或名称
     * @return 容器信息
     */
    public ContainerInfo getContainerInfo(String containerId) {
        try {
            if (!isValidContainerId(containerId)) {
                return null;
            }

            ProcessBuilder processBuilder = createDockerProcess("inspect", containerId, "--format",
                "{{.Id}}|{{.Name}}|{{.Config.Image}}|{{.State.Status}}|{{.NetworkSettings.Ports}}");
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && process.waitFor() == 0) {
                    ContainerInfo container = parseContainerInfo(line);
                    if (container != null) {
                        enrichContainerWithStats(container);
                        return container;
                    }
                }
            }
        } catch (Exception e) {
            // 获取容器信息异常
        }
        return null;
    }

    /**
     * 获取容器日志
     * @param containerId 容器ID或名称
     * @param tail 获取最后N行日志，默认100行
     * @return 容器日志内容
     */
    public String getContainerLogs(String containerId, int tail) {
        try {
            if (!isValidContainerId(containerId)) {
                return "无效的容器ID格式";
            }

            ProcessBuilder processBuilder = createDockerProcess("logs", "--tail", String.valueOf(tail), containerId);
            Process process = processBuilder.start();
            
            StringBuilder logs = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line).append("\n");
                }
            }
            
            // 也读取错误流的日志
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    logs.append("[STDERR] ").append(line).append("\n");
                }
            }
            
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "获取日志超时";
            }
            
            if (process.exitValue() == 0) {
                return logs.toString();
            } else {
                return "获取日志失败: 容器不存在或无权限访问";
            }
            
        } catch (Exception e) {
            return "获取日志异常: " + e.getMessage();
        }
    }

    /**
     * 获取容器日志（默认100行）
     */
    public String getContainerLogs(String containerId) {
        return getContainerLogs(containerId, 100);
    }

    /**
     * 删除容器
     * @param containerId 容器ID或名称
     * @param force 是否强制删除（即使容器正在运行）
     * @return 操作结果
     */
    public String removeContainer(String containerId, boolean force) {
        try {
            // 安全验证
            String securityResult = validateContainerSecurity(containerId);
            if (securityResult != null) {
                return securityResult;
            }

            List<String> command = buildDockerCommand();
            command.add("rm");
            if (force) {
                command.add("-f");
            }
            command.add(containerId);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return "容器删除超时";
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return "容器 " + containerId + " 删除成功";
            } else {
                String error = readProcessError(process);
                return "容器删除失败: " + error;
            }
            
        } catch (Exception e) {
            return "容器删除异常: " + e.getMessage();
        }
    }

    /**
     * 删除容器（默认非强制删除）
     */
    public String removeContainer(String containerId) {
        return removeContainer(containerId, false);
    }

    /**
     * 运行新容器
     * @param imageName 镜像名称
     * @param containerName 容器名称
     * @param portMapping 端口映射 (格式: "host:container")
     * @param envVars 环境变量
     * @return 操作结果
     */
    public String runContainer(String imageName, String containerName, String portMapping, List<String> envVars) {
        // 调用新版本方法,转换返回值以保持向后兼容
        ContainerCreationResult result = runContainerV2(imageName, containerName, portMapping, envVars);
        return result.getFullMessage();
    }

    /**
     * 运行新容器 V2 - 带实际验证的版本
     * @param imageName 镜像名称
     * @param containerName 容器名称
     * @param portMapping 端口映射 (格式: "host:container")
     * @param envVars 环境变量列表
     * @return 容器创建结果对象
     */
    public ContainerCreationResult runContainerV2(String imageName, String containerName, String portMapping, List<String> envVars) {
        String errorMessage = "";

        try {
            // 验证容器名称安全性
            if (containerName != null && !containerName.isEmpty()) {
                if (!isAllowedContainerName(containerName)) {
                    return ContainerCreationResult.failure(containerName,
                        "不允许创建此容器名称。仅允许前缀为 " +
                        String.join(", ", dockerConfig.getAllowedContainerPrefixes()) + " 的容器");
                }
            }

            List<String> command = buildDockerCommand();
            command.add("run");
            command.add("-d"); // 后台运行

            if (containerName != null && !containerName.isEmpty()) {
                command.add("--name");
                command.add(containerName);
            }

            if (portMapping != null && !portMapping.isEmpty()) {
                command.add("-p");
                command.add(portMapping);
            }

            if (envVars != null) {
                for (String env : envVars) {
                    command.add("-e");
                    command.add(env);
                }
            }

            command.add(imageName);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ContainerCreationResult.failure(containerName, "容器启动超时");
            }

            // 读取可能的错误信息
            errorMessage = readProcessError(process);

            // 🆕 不管exitCode如何,都进行实际验证
            // 等待1秒让容器完全启动
            Thread.sleep(1000);

            // 查询容器是否真的存在并运行
            List<ContainerInfo> containers = getContainersByName(containerName);

            if (!containers.isEmpty()) {
                // 找到匹配的容器
                ContainerInfo container = containers.get(0);
                String status = container.getStatus();

                // 检查容器状态是否为运行中
                if (status != null && (status.toLowerCase().contains("up") || status.toLowerCase().contains("running"))) {
                    // ✓ 容器确实存在且运行
                    return ContainerCreationResult.success(container.getContainerId(), containerName);
                }
            }

            // ✗ 容器不存在或未运行
            String finalError = errorMessage.isEmpty() ? "容器创建后未能成功运行" : errorMessage;
            return ContainerCreationResult.failure(containerName, finalError);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ContainerCreationResult.failure(containerName, "容器验证被中断: " + e.getMessage());
        } catch (Exception e) {
            return ContainerCreationResult.failure(containerName, "容器创建异常: " + e.getMessage());
        }
    }

    /**
     * 检查Docker是否可用
     * @return true如果Docker可用，否则false
     */
    public boolean isDockerAvailable() {
        try {
            ProcessBuilder processBuilder = createDockerProcess("--version");
            Process process = processBuilder.start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证容器ID格式
     */
    private boolean isValidContainerId(String containerId) {
        if (containerId == null || containerId.trim().isEmpty()) {
            return false;
        }
        // 容器ID通常是12位或64位十六进制字符串，或者是容器名称
        return containerId.matches("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$") && containerId.length() >= 1;
    }

    /**
     * 验证容器名称是否符合安全策略
     */
    private boolean isAllowedContainerName(String containerName) {
        if (containerName == null || containerName.trim().isEmpty()) {
            return false;
        }
        
        String[] allowedPrefixes = dockerConfig.getAllowedContainerPrefixes();
        if (allowedPrefixes == null || allowedPrefixes.length == 0) {
            // 如果未配置前缀限制，则允许所有容器
            return true;
        }
        
        for (String prefix : allowedPrefixes) {
            if (containerName.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 验证容器操作安全性
     */
    private String validateContainerSecurity(String containerId) {
        if (!isValidContainerId(containerId)) {
            return "无效的容器ID格式";
        }
        
        // 如果containerId看起来像容器名称而不是十六进制ID
        if (!containerId.matches("^[a-fA-F0-9]{12,64}$")) {
            if (!isAllowedContainerName(containerId)) {
                return "不允许操作此容器: " + containerId + "。仅允许操作前缀为 " + 
                       String.join(", ", dockerConfig.getAllowedContainerPrefixes()) + " 的容器";
            }
        }
        
        return null; // 验证通过
    }

    /**
     * 读取进程错误输出并确保资源清理
     */
    /**
     * 根据容器名称查询容器列表
     * @param containerName 容器名称（支持部分匹配）
     * @return 匹配的容器信息列表
     */
    public List<ContainerInfo> getContainersByName(String containerName) {
        List<ContainerInfo> containers = new ArrayList<>();

        try {
            // 执行docker ps -a --filter name=容器名 命令
            ProcessBuilder processBuilder = createDockerProcess("ps", "-a",
                "--filter", "name=" + containerName,
                "--format", "{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}|{{.Ports}}");
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ContainerInfo container = parseContainerInfo(line);
                    if (container != null) {
                        containers.add(container);
                    }
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ArrayList<>();
            }

        } catch (Exception e) {
            return new ArrayList<>();
        }

        return containers;
    }

    private String readProcessError(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
            return error.toString().trim();
        } catch (IOException e) {
            return "无法读取错误信息";
        } finally {
            // 确保进程资源清理
            if (process.isAlive()) {
                try {
                    process.waitFor(2, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }
    }

    /**
     * 使用容器统计信息丰富容器信息
     * @param container 容器信息对象
     */
    private void enrichContainerWithStats(ContainerInfo container) {
        try {
            // 只对运行中的容器获取统计信息
            if (container.getStatus().toLowerCase().contains("running")) {
                ProcessBuilder processBuilder = createDockerProcess("stats", "--no-stream", "--format",
                    "{{.CPUPerc}}|{{.MemUsage}}", container.getContainerId());
                Process process = processBuilder.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                        String[] stats = line.split("\\|");
                        if (stats.length >= 2) {
                            container.setCpuUsage(stats[0].trim());
                            container.setMemoryUsage(stats[1].trim());
                        }
                    }
                }
            } else {
                container.setCpuUsage("0%");
                container.setMemoryUsage("0B / 0B");
            }
        } catch (Exception e) {
            // 如果获取统计信息失败，设置默认值
            container.setCpuUsage("N/A");
            container.setMemoryUsage("N/A");
        }
    }

    /**
     * 解析容器信息
     */
    private ContainerInfo parseContainerInfo(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length >= 4) {
                ContainerInfo container = new ContainerInfo();
                
                // 设置容器ID，确保不为空
                String containerId = parts[0].trim();
                if (containerId == null || containerId.isEmpty()) {
                    // Debug statement removed
                    return null;
                }
                container.setContainerId(containerId);
                
                // 设置容器名称，处理可能的空值
                String containerName = parts[1].trim();
                container.setName(containerName != null && !containerName.isEmpty() ? containerName : "unnamed-" + containerId);
                
                // 设置镜像名称
                String image = parts[2].trim();
                container.setImage(image != null && !image.isEmpty() ? image : "unknown");
                
                // 设置状态
                String status = parts[3].trim();
                container.setStatus(status != null && !status.isEmpty() ? status : "unknown");
                
                // 初始化资源使用情况，后续通过enrichContainerWithStats更新
                container.setCpuUsage("N/A");
                container.setMemoryUsage("N/A");
                
                return container;
            }
        } catch (Exception e) {
            // Debug statement removed
        }
        return null;
    }

    /**
     * 获取模拟容器数据（当Docker不可用时使用）
     */
    private List<ContainerInfo> getSimulatedContainers() {
        List<ContainerInfo> list = new ArrayList<>();

        ContainerInfo c1 = new ContainerInfo();
        c1.setContainerId("1a2b3c4d");
        c1.setName("mysql-db");
        c1.setImage("mysql:8.0");
        c1.setStatus("Running");
        c1.setCpuUsage("3.2%");
        c1.setMemoryUsage("150MB");
        list.add(c1);

        ContainerInfo c2 = new ContainerInfo();
        c2.setContainerId("5e6f7g8h");
        c2.setName("vuln-web");
        c2.setImage("vulnerable/web:latest");
        c2.setStatus("Exited");
        c2.setCpuUsage("0%");
        c2.setMemoryUsage("0MB");
        list.add(c2);

        return list;
    }
    
    // ===== 多节点支持的新方法 =====
    
    /**
     * 在指定主机节点上重启容器
     */
    public String restartContainerOnNode(String containerId, Long hostNodeId) {
        try {
            // 安全验证
            String securityResult = validateContainerSecurity(containerId);
            if (securityResult != null) {
                return securityResult;
            }
            
            HostNode hostNode = hostNodeService.getNodeById(hostNodeId)
                .orElseThrow(() -> new RuntimeException("主机节点不存在: " + hostNodeId));
                
            if (!hostNode.isActive()) {
                return "主机节点未激活: " + hostNode.getDisplayName();
            }

            ProcessBuilder processBuilder = createDockerProcessForNode(hostNode, "restart", containerId);
            Process process = processBuilder.start();
            
            boolean finished = process.waitFor(dockerConfig.getCommandTimeout(), TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return "容器重启超时";
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return "容器 " + containerId + " 在节点 " + hostNode.getDisplayName() + " 上重启成功";
            } else {
                String error = readProcessError(process);
                return "容器重启失败: " + error;
            }
            
        } catch (Exception e) {
            return "容器重启异常: " + e.getMessage();
        }
    }
    
    /**
     * 在指定主机节点上停止容器
     */
    public String stopContainerOnNode(String containerId, Long hostNodeId) {
        try {
            // 安全验证
            String securityResult = validateContainerSecurity(containerId);
            if (securityResult != null) {
                return securityResult;
            }
            
            HostNode hostNode = hostNodeService.getNodeById(hostNodeId)
                .orElseThrow(() -> new RuntimeException("主机节点不存在: " + hostNodeId));
                
            if (!hostNode.isActive()) {
                return "主机节点未激活: " + hostNode.getDisplayName();
            }

            ProcessBuilder processBuilder = createDockerProcessForNode(hostNode, "stop", containerId);
            Process process = processBuilder.start();
            
            boolean finished = process.waitFor(dockerConfig.getCommandTimeout(), TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return "容器停止超时";
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return "容器 " + containerId + " 在节点 " + hostNode.getDisplayName() + " 上已停止";
            } else {
                String error = readProcessError(process);
                return "容器停止失败: " + error;
            }
            
        } catch (Exception e) {
            return "容器停止异常: " + e.getMessage();
        }
    }
    
    /**
     * 在指定主机节点上启动容器
     */
    public String startContainerOnNode(String containerId, Long hostNodeId) {
        try {
            // 安全验证
            String securityResult = validateContainerSecurity(containerId);
            if (securityResult != null) {
                return securityResult;
            }
            
            HostNode hostNode = hostNodeService.getNodeById(hostNodeId)
                .orElseThrow(() -> new RuntimeException("主机节点不存在: " + hostNodeId));
                
            if (!hostNode.isActive()) {
                return "主机节点未激活: " + hostNode.getDisplayName();
            }

            ProcessBuilder processBuilder = createDockerProcessForNode(hostNode, "start", containerId);
            Process process = processBuilder.start();
            
            boolean finished = process.waitFor(dockerConfig.getCommandTimeout(), TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return "容器启动超时";
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return "容器 " + containerId + " 在节点 " + hostNode.getDisplayName() + " 上启动成功";
            } else {
                String error = readProcessError(process);
                return "容器启动失败: " + error;
            }
            
        } catch (Exception e) {
            return "容器启动异常: " + e.getMessage();
        }
    }
    
    /**
     * 在指定主机节点上创建并运行容器
     */
    public String createContainerOnNode(String imageName, String containerName, String portMapping, 
                                      List<String> envVars, Long hostNodeId) {
        try {
            HostNode hostNode = hostNodeService.getNodeById(hostNodeId)
                .orElseThrow(() -> new RuntimeException("主机节点不存在: " + hostNodeId));
                
            if (!hostNode.isActive()) {
                return "主机节点未激活: " + hostNode.getDisplayName();
            }
            
            if (imageName == null || imageName.trim().isEmpty()) {
                return "镜像名称不能为空";
            }
            
            // 验证容器名称安全性
            if (containerName != null && !containerName.isEmpty()) {
                if (!isAllowedContainerName(containerName)) {
                    return "不允许创建此容器名称: " + containerName + "。仅允许前缀为 " + 
                           String.join(", ", dockerConfig.getAllowedContainerPrefixes()) + " 的容器";
                }
            }

            List<String> command = buildDockerCommandForNode(hostNode);
            command.add("run");
            command.add("-d"); // 后台运行
            
            if (containerName != null && !containerName.isEmpty()) {
                command.add("--name");
                command.add(containerName);
            }
            
            if (portMapping != null && !portMapping.isEmpty()) {
                command.add("-p");
                command.add(portMapping);
            }
            
            if (envVars != null) {
                for (String env : envVars) {
                    command.add("-e");
                    command.add(env);
                }
            }
            
            command.add(imageName);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return "容器创建超时";
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return "容器在节点 " + hostNode.getDisplayName() + " 上创建成功，容器ID: " + output.toString().trim();
            } else {
                String error = readProcessError(process);
                return "容器创建失败: " + error;
            }
            
        } catch (Exception e) {
            return "容器创建异常: " + e.getMessage();
        }
    }
    
    /**
     * 获取指定主机节点上的容器列表
     */
    public List<ContainerInfo> getContainersOnNode(Long hostNodeId) {
        try {
            HostNode hostNode = hostNodeService.getNodeById(hostNodeId)
                .orElseThrow(() -> new RuntimeException("主机节点不存在: " + hostNodeId));
                
            if (!hostNode.isActive()) {
                // Debug statement removed
                return new ArrayList<>(); // 返回空列表
            }
            
            // 使用带重试的Docker命令执行
            String result = executeDockerCommandWithRetry(hostNode, 3, "ps", "-a", 
                "--format", "{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}");
                
            // 检查执行结果
            if (result.contains("连接失败") || result.contains("执行失败") || result.contains("超时")) {
                // Debug statement removed
                return new ArrayList<>();
            }
            
            List<ContainerInfo> containers = new ArrayList<>();
            
            // 解析容器信息
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                ContainerInfo container = parseContainerInfo(line);
                if (container != null) {
                    // 添加节点信息
                    container.setHostNodeId(hostNodeId);
                    container.setHostNodeName(hostNode.getName());
                    container.setHostNodeIp(hostNode.getHostIp());
                    containers.add(container);
                }
            }
            
            // 为每个容器获取详细统计信息
            for (ContainerInfo container : containers) {
                enrichContainerWithStatsOnNode(container, hostNode);
            }
            
            return containers;
            
        } catch (Exception e) {
            // Debug statement removed
            return new ArrayList<>();
        }
    }
    
    /**
     * 测试指定节点的Docker连接
     */
    private boolean testDockerConnectionOnNode(HostNode hostNode) {
        try {
            // 使用NetworkTestUtil进行连接诊断
            NetworkTestUtil.NetworkDiagnosticResult result = 
                NetworkTestUtil.diagnoseConnection(hostNode.getHostIp(), hostNode.getDockerPort());
            
            if (result.isDockerApiSuccess()) {
                return true;
            } else {
                // Debug statement removed
                // Debug statement removed
                // Debug statement removed
                // Debug statement removed
                // Debug statement removed
                return false;
            }
        } catch (Exception e) {
            // Debug statement removed
            return false;
        }
    }
    
    /**
     * 检查指定主机节点上的Docker是否可用
     */
    public boolean isDockerAvailableOnNode(Long hostNodeId) {
        try {
            HostNode hostNode = hostNodeService.getNodeById(hostNodeId)
                .orElse(null);
                
            if (hostNode == null || !hostNode.isActive()) {
                return false;
            }
            
            ProcessBuilder processBuilder = createDockerProcessForNode(hostNode, "--version");
            Process process = processBuilder.start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取所有节点上的容器列表（聚合视图）
     */
    public List<ContainerInfo> getAllContainersFromAllNodes() {
        List<ContainerInfo> allContainers = new ArrayList<>();
        
        List<HostNode> activeNodes = hostNodeService.getActiveNodes();
        
        for (HostNode node : activeNodes) {
            try {
                List<ContainerInfo> nodeContainers = getContainersOnNode(node.getId());
                allContainers.addAll(nodeContainers);
            } catch (Exception e) {
                // Debug statement removed
            }
        }
        
        return allContainers;
    }
    
    /**
     * 为指定节点上的容器获取统计信息
     */
    private void enrichContainerWithStatsOnNode(ContainerInfo container, HostNode hostNode) {
        try {
            if (container.getStatus().toLowerCase().contains("running")) {
                ProcessBuilder processBuilder = createDockerProcessForNode(hostNode, "stats", "--no-stream",
                    "--format", "{{.CPUPerc}}|{{.MemUsage}}", container.getContainerId());
                Process process = processBuilder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                        String[] stats = line.split("\\|");
                        if (stats.length >= 2) {
                            container.setCpuUsage(stats[0].trim());
                            container.setMemoryUsage(stats[1].trim());
                        }
                    }
                }
            } else {
                container.setCpuUsage("0%");
                container.setMemoryUsage("0B / 0B");
            }
        } catch (Exception e) {
            container.setCpuUsage("N/A");
            container.setMemoryUsage("N/A");
        }
    }

    /**
     * 直接使用Docker API URL获取容器列表（新方法 - 支持资产IP直连）
     * @param dockerUrl Docker API URL，格式：http://IP:PORT
     * @return 容器列表
     */
    public List<ContainerInfo> getContainersFromUrl(String dockerUrl) {
        List<ContainerInfo> containers = new ArrayList<>();

        try {
            if (dockerUrl == null || dockerUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("Docker URL不能为空");
            }

            // 构建Docker命令，直接使用URL
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("-H");
            command.add(dockerUrl);
            command.add("ps");
            command.add("-a");
            command.add("--format");
            command.add("{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}");

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 等待命令执行完成
            boolean finished = process.waitFor(dockerConfig.getCommandTimeout(), TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Docker命令执行超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String error = readProcessOutput(process);
                throw new RuntimeException("Docker命令执行失败: " + error);
            }

            // 读取并解析容器信息
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    ContainerInfo container = parseContainerInfo(line);
                    if (container != null && container.getContainerId() != null && !container.getContainerId().isEmpty()) {
                        // 为容器添加URL信息
                        container.setDockerUrl(dockerUrl);
                        containers.add(container);
                    }
                }
            }

            // 为运行中的容器获取资源使用统计
            for (ContainerInfo container : containers) {
                enrichContainerWithStatsFromUrl(container, dockerUrl);
            }

        } catch (Exception e) {
            throw new RuntimeException("从Docker URL获取容器失败: " + e.getMessage(), e);
        }

        return containers;
    }

    /**
     * 直接使用Docker API URL获取镜像列表（支持资产IP直连）
     * @param dockerUrl Docker API URL，格式：http://IP:PORT
     * @return 镜像列表
     */
    public List<ImageInfo> getImagesFromUrl(String dockerUrl) {
        List<ImageInfo> images = new ArrayList<>();

        try {
            if (dockerUrl == null || dockerUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("Docker URL不能为空");
            }

            // 构建Docker命令，直接使用URL
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("-H");
            command.add(dockerUrl);
            command.add("images");
            command.add("--format");
            command.add("{{.Repository}}|{{.Tag}}|{{.ID}}|{{.CreatedSince}}|{{.Size}}");

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 等待命令执行完成
            boolean finished = process.waitFor(dockerConfig.getCommandTimeout(), TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Docker命令执行超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String error = readProcessOutput(process);
                throw new RuntimeException("Docker命令执行失败: " + error);
            }

            // 读取并解析镜像信息
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split("\\|");
                    if (parts.length >= 5) {
                        ImageInfo image = new ImageInfo();
                        image.setRepository(parts[0].trim());
                        image.setTag(parts[1].trim());
                        image.setImageId(parts[2].trim());
                        image.setCreated(parts[3].trim());
                        image.setSize(parts[4].trim());

                        // 过滤<none>镜像
                        if (image.isValid()) {
                            images.add(image);
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("从Docker URL获取镜像失败: " + e.getMessage(), e);
        }

        return images;
    }

    /**
     * 从URL获取容器统计信息
     */
    private void enrichContainerWithStatsFromUrl(ContainerInfo container, String dockerUrl) {
        try {
            if (container.getStatus().toLowerCase().contains("running")) {
                List<String> command = new ArrayList<>();
                command.add("docker");
                command.add("-H");
                command.add(dockerUrl);
                command.add("stats");
                command.add("--no-stream");
                command.add("--format");
                command.add("{{.CPUPerc}}|{{.MemUsage}}");
                command.add(container.getContainerId());

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                Process process = processBuilder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                        String[] stats = line.split("\\|");
                        if (stats.length >= 2) {
                            container.setCpuUsage(stats[0].trim());
                            container.setMemoryUsage(stats[1].trim());
                        }
                    }
                }
            } else {
                container.setCpuUsage("0%");
                container.setMemoryUsage("0B / 0B");
            }
        } catch (Exception e) {
            container.setCpuUsage("N/A");
            container.setMemoryUsage("N/A");
        }
    }

    /**
     * 获取指定节点上的Docker镜像列表
     */
    public List<ImageInfo> getImagesOnNode(Long hostNodeId) {
        try {
            HostNode hostNode = hostNodeService.getNodeById(hostNodeId)
                .orElseThrow(() -> new RuntimeException("主机节点不存在: " + hostNodeId));

            if (!hostNode.isActive()) {
                return new ArrayList<>();
            }

            // 执行 docker images 命令，格式化输出
            String result = executeDockerCommandWithRetry(hostNode, 3, "images",
                "--format", "{{.Repository}}|{{.Tag}}|{{.ID}}|{{.CreatedSince}}|{{.Size}}");

            if (result.contains("连接失败") || result.contains("执行失败") || result.contains("超时")) {
                return new ArrayList<>();
            }

            List<ImageInfo> images = new ArrayList<>();
            String[] lines = result.split("\n");

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    ImageInfo image = new ImageInfo();
                    image.setRepository(parts[0].trim());
                    image.setTag(parts[1].trim());
                    image.setImageId(parts[2].trim());
                    image.setCreated(parts[3].trim());
                    image.setSize(parts[4].trim());

                    // 添加节点信息
                    image.setHostNodeId(hostNodeId);
                    image.setHostNodeName(hostNode.getName());
                    image.setHostNodeIp(hostNode.getHostIp());

                    // 只添加有效镜像(过滤<none>)
                    if (image.isValid()) {
                        images.add(image);
                    }
                }
            }

            return images;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 容器创建结果类
     * 用于封装容器创建操作的结果信息
     */
    public static class ContainerCreationResult {
        private boolean success;
        private String containerId;
        private String containerName;
        private String message;
        private String errorDetails;

        private ContainerCreationResult() {
        }

        /**
         * 创建成功结果
         */
        public static ContainerCreationResult success(String containerId, String containerName) {
            ContainerCreationResult result = new ContainerCreationResult();
            result.success = true;
            result.containerId = containerId;
            result.containerName = containerName;
            result.message = "容器创建成功";
            return result;
        }

        /**
         * 创建失败结果
         */
        public static ContainerCreationResult failure(String containerName, String errorMessage) {
            ContainerCreationResult result = new ContainerCreationResult();
            result.success = false;
            result.containerName = containerName;
            result.message = "容器创建失败";
            result.errorDetails = errorMessage;
            return result;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getContainerId() {
            return containerId;
        }

        public String getContainerName() {
            return containerName;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorDetails() {
            return errorDetails;
        }

        public String getFullMessage() {
            if (success) {
                return message + ", 容器ID: " + containerId;
            } else {
                return message + (errorDetails != null ? ": " + errorDetails : "");
            }
        }
    }
}