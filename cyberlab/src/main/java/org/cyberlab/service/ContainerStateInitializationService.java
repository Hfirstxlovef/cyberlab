package org.cyberlab.service;

import org.cyberlab.entity.Asset;
import org.cyberlab.entity.ContainerState;
import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.entity.HostNode;
import org.cyberlab.repository.AssetRepository;
import org.cyberlab.repository.ContainerStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 容器状态初始化和同步服务
 */
@Service
public class ContainerStateInitializationService {

    private static final Logger logger = LoggerFactory.getLogger(ContainerStateInitializationService.class);

    @Autowired
    private ContainerStateRepository containerStateRepository;
    
    @Autowired
    private AssetRepository assetRepository;
    
    @Autowired
    private DockerService dockerService;
    
    @Autowired
    private HostNodeService hostNodeService;

    /**
     * 初始化所有项目的容器状态数据
     */
    @Transactional
    public void initializeAllContainerStates() {
        // Debug statement removed
        
        try {
            // 获取所有有效的资产
            List<Asset> allAssets = assetRepository.findByEnabledTrue();
            // Debug statement removed
            
            int initialized = 0;
            int skipped = 0;
            int failed = 0;
            
            for (Asset asset : allAssets) {
                try {
                    boolean result = initializeAssetContainerState(asset);
                    if (result) {
                        initialized++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    failed++;
                    // Debug statement removed
                }
            }
            
            // Debug statement removed
            // Debug statement removed
            // Debug statement removed
            // Debug statement removed

        } catch (Exception e) {
            logger.error("初始化所有容器状态失败", e);
        }
    }
    
    /**
     * 初始化特定项目的容器状态
     */
    @Transactional
    public void initializeProjectContainerStates(String projectId) {
        // Debug statement removed
        
        try {
            List<Asset> projectAssets = assetRepository.findByTopologyProjectId(projectId);
            // Debug statement removed
            
            int initialized = 0;
            for (Asset asset : projectAssets) {
                if (initializeAssetContainerState(asset)) {
                    initialized++;
                }
            }
            
            // Debug statement removed
            
        } catch (Exception e) {
            // Debug statement removed
        }
    }
    
    /**
     * 初始化单个资产的容器状态
     */
    private boolean initializeAssetContainerState(Asset asset) {
        try {
            // 检查是否已存在容器状态记录
            List<ContainerState> existingStates = containerStateRepository.findByAssetId(asset.getId());
            if (!existingStates.isEmpty()) {
                // Debug statement removed
                return false;
            }
            
            // 检查资产是否是容器类型
            if (!isContainerAsset(asset)) {
                // Debug statement removed
                return false;
            }
            
            // 检查资产是否关联了主机节点
            if (asset.getPreferredHostNodeId() == null) {
                // Debug statement removed
                createPendingContainerState(asset);
                return true;
            }
            
            // 获取主机节点信息
            Optional<HostNode> hostNodeOpt = hostNodeService.getNodeById(asset.getPreferredHostNodeId());
            if (!hostNodeOpt.isPresent()) {
                // Debug statement removed
                createPendingContainerState(asset);
                return true;
            }
            
            HostNode hostNode = hostNodeOpt.get();
            
            // 尝试从Docker API获取实际容器状态
            try {
                List<ContainerInfo> containers = dockerService.getContainersOnNode(hostNode.getId());
                
                // 查找与资产匹配的容器
                Optional<ContainerInfo> matchingContainer = containers.stream()
                    .filter(c -> isContainerMatchingAsset(c, asset))
                    .findFirst();
                
                if (matchingContainer.isPresent()) {
                    createContainerStateFromExisting(asset, matchingContainer.get(), hostNode);
                    // Debug statement removed
                } else {
                    createDesiredContainerState(asset, hostNode);
                    // Debug statement removed
                }
                
                return true;
                
            } catch (Exception e) {
                // Debug statement removed
                createDesiredContainerState(asset, hostNode);
                return true;
            }
            
        } catch (Exception e) {
            // Debug statement removed
            return false;
        }
    }
    
    /**
     * 检查是否是容器类型资产
     */
    private boolean isContainerAsset(Asset asset) {
        return asset.getDockerImage() != null && !asset.getDockerImage().trim().isEmpty()
            || "container".equals(asset.getAssetType());
    }
    
    /**
     * 检查容器是否匹配资产
     */
    private boolean isContainerMatchingAsset(ContainerInfo container, Asset asset) {
        // 按名称匹配
        if (container.getName() != null && container.getName().contains(asset.getName())) {
            return true;
        }
        
        // 按镜像匹配
        if (asset.getDockerImage() != null && container.getImage() != null) {
            return container.getImage().contains(asset.getDockerImage()) ||
                   asset.getDockerImage().contains(container.getImage());
        }
        
        return false;
    }
    
    /**
     * 创建待分配状态的容器记录
     */
    private void createPendingContainerState(Asset asset) {
        ContainerState state = new ContainerState();
        state.setAssetId(asset.getId());
        state.setContainerName(generateContainerName(asset));
        state.setImageName(asset.getDockerImage());
        state.setDesiredStatus("running");
        state.setCurrentStatus("not_exists");
        state.setHealthStatus("unknown");
        state.setSyncStatus("PENDING");
        state.setSyncAttempts(0);
        state.setMaxSyncAttempts(3);
        state.setCreatedAt(LocalDateTime.now());
        state.setCreatedBy("system");
        
        containerStateRepository.save(state);
    }
    
    /**
     * 从现有容器创建状态记录
     */
    private void createContainerStateFromExisting(Asset asset, ContainerInfo container, HostNode hostNode) {
        ContainerState state = new ContainerState();
        state.setAssetId(asset.getId());
        state.setContainerId(container.getContainerId());
        state.setContainerName(container.getName());
        state.setImageName(container.getImage());
        state.setHostNodeId(hostNode.getId());
        state.setDesiredStatus("running");
        state.setCurrentStatus(mapDockerStatusToInternal(container.getStatus()));
        state.setHealthStatus(determineHealthStatus(container.getStatus()));
        state.setSyncStatus("SYNCED");
        state.setSyncAttempts(0);
        state.setMaxSyncAttempts(3);
        state.setLastSyncAt(LocalDateTime.now());
        state.setCreatedAt(LocalDateTime.now());
        state.setCreatedBy("system");
        
        containerStateRepository.save(state);
    }
    
    /**
     * 创建期望状态的容器记录
     */
    private void createDesiredContainerState(Asset asset, HostNode hostNode) {
        ContainerState state = new ContainerState();
        state.setAssetId(asset.getId());
        state.setContainerName(generateContainerName(asset));
        state.setImageName(asset.getDockerImage());
        state.setHostNodeId(hostNode.getId());
        state.setDesiredStatus("running");
        state.setCurrentStatus("not_exists");
        state.setHealthStatus("unknown");
        state.setSyncStatus("OUT_OF_SYNC");
        state.setSyncAttempts(0);
        state.setMaxSyncAttempts(3);
        state.setCreatedAt(LocalDateTime.now());
        state.setCreatedBy("system");
        
        containerStateRepository.save(state);
    }
    
    /**
     * 生成容器名称
     */
    private String generateContainerName(Asset asset) {
        String baseName = asset.getName().replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        return "cyberlab_" + baseName + "_" + System.currentTimeMillis();
    }
    
    /**
     * 映射Docker状态到内部状态
     */
    private String mapDockerStatusToInternal(String dockerStatus) {
        if (dockerStatus == null) return "unknown";
        
        String status = dockerStatus.toLowerCase();
        if (status.contains("running")) return "running";
        if (status.contains("stopped") || status.contains("exited")) return "stopped";
        if (status.contains("paused")) return "paused";
        if (status.contains("restarting")) return "restarting";
        
        return "unknown";
    }
    
    /**
     * 确定健康状态
     */
    private String determineHealthStatus(String dockerStatus) {
        if (dockerStatus == null) return "unknown";
        
        String status = dockerStatus.toLowerCase();
        if (status.contains("running")) return "healthy";
        if (status.contains("stopped") || status.contains("exited")) return "stopped";
        
        return "unknown";
    }
}