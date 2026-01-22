package org.cyberlab.repository;

import org.cyberlab.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    // 根据可见性筛选资产（可用于 red / blue / both）
    List<Asset> findByVisibility(String visibility);

    // 根据名称查找资产
    Optional<Asset> findByName(String name);

    // 根据名称和公司查找资产 (用于容器导入时检查重复)
    Optional<Asset> findByNameAndCompany(String name, String company);

    // 红队视角：只看被标记为靶场的资产
    List<Asset> findByIsTargetTrue();

    // 根据公司查找资产
    List<Asset> findByCompany(String company);

    // 根据所有者查找资产
    List<Asset> findByOwner(String owner);

    // 查找启用的资产
    List<Asset> findByEnabledTrue();

    // 根据IP地址模糊查找
    List<Asset> findByIpContaining(String ipPattern);

    // 根据拓扑项目ID查找资产
    List<Asset> findByTopologyProjectId(String topologyProjectId);

    // 根据资产类型查找资产
    List<Asset> findByAssetType(String assetType);
    
    // 查找所有容器类型的资产
    @Query("SELECT a FROM Asset a WHERE a.assetType = 'container' OR a.dockerImage IS NOT NULL")
    List<Asset> findAllContainerAssets();

    // 查找没有分配拓扑项目的资产
    List<Asset> findByTopologyProjectIdIsNull();

    // 按名称搜索资产（模糊查询）
    @Query("SELECT a FROM Asset a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.company) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Asset> searchByKeyword(@Param("keyword") String keyword);

    // 按名称搜索资产（模糊查询，分页）
    @Query("SELECT a FROM Asset a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.company) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Asset> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 根据IP地址查找资产
    List<Asset> findByIp(String ip);

    // 统计各公司的资产数量
    @Query("SELECT a.company, COUNT(a) FROM Asset a GROUP BY a.company")
    List<Object[]> countByCompany();

    // 统计各所有者的资产数量
    @Query("SELECT a.owner, COUNT(a) FROM Asset a GROUP BY a.owner")
    List<Object[]> countByOwner();

    // 统计启用的资产数量
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.enabled = true")
    long countByEnabledTrue();

    // 统计目标资产数量
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.isTarget = true")
    long countByIsTargetTrue();

    // 根据可见性和启用状态查找资产
    List<Asset> findByVisibilityInAndEnabled(List<String> visibilities, boolean enabled);

    // 根据公司和项目查找资产
    List<Asset> findByCompanyAndProject(String company, String project);

    // 根据项目查找资产
    List<Asset> findByProject(String project);

    // 统计容器类型资产数量
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.assetType = 'container'")
    long countByAssetTypeContainer();

    // 统计各资产类型数量
    @Query("SELECT a.assetType, COUNT(a) FROM Asset a GROUP BY a.assetType")
    List<Object[]> countByAssetType();
    
    // 根据首选主机节点ID查找资产
    List<Asset> findByPreferredHostNodeId(Long preferredHostNodeId);

    // ========================================
    // 性能优化查询说明
    // ========================================
    // 注意: Asset实体使用简单字段(preferredHostNodeId)而非@ManyToOne关联
    // 因此无需使用EntityGraph或JOIN FETCH来避免N+1查询
    // 若需要HostNode信息,应在Service层通过ID批量查询
}