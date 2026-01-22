package org.cyberlab.service;

import org.cyberlab.entity.Asset;
import org.cyberlab.entity.HostNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 容器权限验证服务
 * 基于现有的企业/项目/可见性机制验证容器操作权限
 */
@Service
public class ContainerPermissionService {

    @Autowired
    private AssetService assetService;
    
    @Autowired
    private HostNodeService hostNodeService;
    
    @Autowired
    private ContainerAuditService containerAuditService;

    /**
     * 验证用户是否可以访问指定资产的容器
     */
    public boolean canAccessAssetContainers(String username, Long assetId) {
        try {
            Asset asset = assetService.getAssetById(assetId).orElse(null);
            if (asset == null) {
                logPermissionCheck(username, "ACCESS_ASSET_CONTAINERS", "Asset:" + assetId, 
                                 false, "资产不存在");
                return false;
            }
            
            boolean allowed = checkAssetAccess(username, asset);
            logPermissionCheck(username, "ACCESS_ASSET_CONTAINERS", 
                             String.format("Asset:%d(%s)", assetId, asset.getName()), 
                             allowed, allowed ? "权限验证通过" : "无访问权限");
            
            return allowed;
        } catch (Exception e) {
            logPermissionCheck(username, "ACCESS_ASSET_CONTAINERS", "Asset:" + assetId, 
                             false, "权限验证异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证用户是否可以操作指定主机节点的容器
     */
    public boolean canAccessHostContainers(String username, Long hostNodeId) {
        try {
            HostNode hostNode = hostNodeService.getNodeById(hostNodeId).orElse(null);
            if (hostNode == null) {
                logPermissionCheck(username, "ACCESS_HOST_CONTAINERS", "Host:" + hostNodeId, 
                                 false, "主机节点不存在");
                return false;
            }
            
            // 检查用户是否可以访问该主机节点上的任何资产
            List<Asset> allAssets = assetService.getAllAssets();
            boolean hasAccessToAnyAsset = allAssets.stream()
                .filter(asset -> hostNodeId.equals(asset.getPreferredHostNodeId()))
                .anyMatch(asset -> checkAssetAccess(username, asset));
            
            logPermissionCheck(username, "ACCESS_HOST_CONTAINERS", 
                             String.format("Host:%d(%s)", hostNodeId, hostNode.getDisplayName()), 
                             hasAccessToAnyAsset, 
                             hasAccessToAnyAsset ? "权限验证通过" : "无访问权限");
            
            return hasAccessToAnyAsset;
        } catch (Exception e) {
            logPermissionCheck(username, "ACCESS_HOST_CONTAINERS", "Host:" + hostNodeId, 
                             false, "权限验证异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证用户是否可以执行批量容器操作
     */
    public boolean canPerformBatchContainerOperations(String username, List<Long> assetIds) {
        try {
            if (assetIds == null || assetIds.isEmpty()) {
                logPermissionCheck(username, "BATCH_CONTAINER_OPERATIONS", "Empty_List", 
                                 false, "资产列表为空");
                return false;
            }
            
            // 检查用户是否可以访问所有指定的资产
            boolean allAllowed = assetIds.stream()
                .allMatch(assetId -> canAccessAssetContainers(username, assetId));
            
            logPermissionCheck(username, "BATCH_CONTAINER_OPERATIONS", 
                             String.format("Assets:%s", assetIds.toString()), 
                             allAllowed, 
                             allAllowed ? "所有资产权限验证通过" : "部分资产无访问权限");
            
            return allAllowed;
        } catch (Exception e) {
            logPermissionCheck(username, "BATCH_CONTAINER_OPERATIONS", 
                             assetIds != null ? assetIds.toString() : "null", 
                             false, "权限验证异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证用户是否可以查看容器审计日志
     */
    public boolean canViewContainerAuditLogs(String username, String targetUsername) {
        try {
            // 管理员可以查看所有日志
            if (isAdmin(username)) {
                logPermissionCheck(username, "VIEW_AUDIT_LOGS", 
                                 targetUsername != null ? "User:" + targetUsername : "All", 
                                 true, "管理员权限");
                return true;
            }
            
            // 普通用户只能查看自己的日志
            boolean allowed = targetUsername == null || username.equals(targetUsername);
            logPermissionCheck(username, "VIEW_AUDIT_LOGS", 
                             targetUsername != null ? "User:" + targetUsername : "All", 
                             allowed, 
                             allowed ? "查看自己的日志" : "无权查看他人日志");
            
            return allowed;
        } catch (Exception e) {
            logPermissionCheck(username, "VIEW_AUDIT_LOGS", 
                             targetUsername != null ? "User:" + targetUsername : "All", 
                             false, "权限验证异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证用户是否可以管理容器同步任务
     */
    public boolean canManageContainerSync(String username) {
        try {
            // 只有管理员可以管理同步任务
            boolean allowed = isAdmin(username);
            logPermissionCheck(username, "MANAGE_CONTAINER_SYNC", "SyncTask", 
                             allowed, 
                             allowed ? "管理员权限" : "需要管理员权限");
            
            return allowed;
        } catch (Exception e) {
            logPermissionCheck(username, "MANAGE_CONTAINER_SYNC", "SyncTask", 
                             false, "权限验证异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取用户可访问的资产列表
     */
    public List<Asset> getAccessibleAssets(String username) {
        try {
            List<Asset> allAssets = assetService.getAllAssets();
            
            return allAssets.stream()
                .filter(asset -> checkAssetAccess(username, asset))
                .collect(Collectors.toList());
        } catch (Exception e) {
            // 获取用户可访问资产失败
            return List.of();
        }
    }

    /**
     * 获取用户可访问的主机节点列表
     */
    public List<HostNode> getAccessibleHostNodes(String username) {
        try {
            List<HostNode> allHosts = hostNodeService.getActiveNodes();
            List<Asset> accessibleAssets = getAccessibleAssets(username);
            
            // 获取可访问资产关联的主机节点ID
            List<Long> accessibleHostIds = accessibleAssets.stream()
                .map(Asset::getPreferredHostNodeId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
            
            return allHosts.stream()
                .filter(host -> accessibleHostIds.contains(host.getId()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            // 获取用户可访问主机节点失败
            return List.of();
        }
    }

    /**
     * 检查资产访问权限
     */
    private boolean checkAssetAccess(String username, Asset asset) {
        if (asset == null || username == null) {
            return false;
        }
        
        // 管理员可以访问所有资产
        if (isAdmin(username)) {
            return true;
        }
        
        // 资产所有者可以访问
        if (username.equals(asset.getOwner())) {
            return true;
        }
        
        // 检查企业权限（这里需要根据实际的用户-企业关联逻辑来实现）
        // 暂时简化处理：如果资产可见性为both或blue，允许访问
        String visibility = asset.getVisibility();
        if ("both".equals(visibility) || "blue".equals(visibility)) {
            return true;
        }
        
        return false;
    }

    /**
     * 检查是否为管理员
     */
    private boolean isAdmin(String username) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                return false;
            }
            
            // 检查是否有管理员角色
            return auth.getAuthorities().stream()
                .anyMatch(authority -> 
                    authority.getAuthority().equals("ROLE_ADMIN") ||
                    authority.getAuthority().equals("ADMIN") ||
                    "admin".equalsIgnoreCase(username)
                );
        } catch (Exception e) {
            return "admin".equalsIgnoreCase(username); // 降级处理
        }
    }

    /**
     * 记录权限检查日志
     */
    private void logPermissionCheck(String username, String operation, String resource, 
                                  boolean allowed, String reason) {
        try {
            containerAuditService.logPermissionCheck(username, operation, resource, allowed, reason);
        } catch (Exception e) {
            // 记录权限检查日志失败
        }
    }

    /**
     * 获取用户权限摘要
     */
    public Map<String, Object> getUserPermissionSummary(String username) {
        try {
            List<Asset> accessibleAssets = getAccessibleAssets(username);
            List<HostNode> accessibleHosts = getAccessibleHostNodes(username);
            
            return Map.of(
                "username", username,
                "isAdmin", isAdmin(username),
                "accessibleAssetsCount", accessibleAssets.size(),
                "accessibleHostsCount", accessibleHosts.size(),
                "permissions", Map.of(
                    "canViewAuditLogs", canViewContainerAuditLogs(username, username),
                    "canManageSync", canManageContainerSync(username),
                    "canBatchOperations", !accessibleAssets.isEmpty()
                )
            );
        } catch (Exception e) {
            return Map.of(
                "username", username,
                "error", "获取权限摘要失败: " + e.getMessage()
            );
        }
    }
}