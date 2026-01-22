package org.cyberlab.repository;

import org.cyberlab.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 授权仓库接口
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Repository for CyberLab Platform
 *
 * 遵循八耻八荣原则：
 * - 清晰的方法命名
 * - 完整的查询方法
 * - 优化的查询性能
 */
@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {

    /**
     * 查找当前激活的授权
     *
     * @return 当前激活的授权（如果存在）
     */
    Optional<License> findByIsCurrentTrue();

    /**
     * 根据状态查询授权列表，按创建时间倒序排列
     *
     * @param status 授权状态
     * @return 授权列表
     */
    List<License> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 查询所有授权，按创建时间倒序排列
     *
     * @return 授权列表
     */
    List<License> findAllByOrderByCreatedAtDesc();

    /**
     * 根据序列号查询授权
     *
     * @param serialNumber 序列号
     * @return 授权（如果存在）
     */
    Optional<License> findBySerialNumber(String serialNumber);

    /**
     * 根据授权码查询授权
     *
     * @param licenseCode 授权码
     * @return 授权（如果存在）
     */
    Optional<License> findByLicenseCode(String licenseCode);

    /**
     * 根据序列号和授权码同时查询授权
     * 用于授权激活验证
     *
     * @param serialNumber 序列号
     * @param licenseCode 授权码
     * @return 授权（如果存在）
     */
    Optional<License> findBySerialNumberAndLicenseCode(String serialNumber, String licenseCode);

    /**
     * 检查序列号是否已存在
     *
     * @param serialNumber 序列号
     * @return true 如果存在，否则 false
     */
    boolean existsBySerialNumber(String serialNumber);

    /**
     * 检查授权码是否已存在
     *
     * @param licenseCode 授权码
     * @return true 如果存在，否则 false
     */
    boolean existsByLicenseCode(String licenseCode);

    /**
     * 查询即将过期的授权（指定天数内）
     *
     * @param today       今天的日期
     * @param futureDate  未来日期（today + N天）
     * @return 即将过期的授权列表
     */
    @Query("SELECT l FROM License l WHERE l.expiryDate > :today AND l.expiryDate <= :futureDate AND l.status = 'ACTIVE'")
    List<License> findExpiringLicenses(@Param("today") LocalDate today, @Param("futureDate") LocalDate futureDate);

    /**
     * 查询已过期但状态仍为 ACTIVE 的授权
     *
     * @param today 今天的日期
     * @return 已过期的授权列表
     */
    @Query("SELECT l FROM License l WHERE l.expiryDate < :today AND l.status = 'ACTIVE'")
    List<License> findExpiredActiveLicenses(@Param("today") LocalDate today);

    /**
     * 将所有授权的 is_current 设置为 false
     * 用于设置新的当前授权之前
     */
    @Modifying
    @Query("UPDATE License l SET l.isCurrent = false WHERE l.isCurrent = true")
    void clearAllCurrentFlags();

    /**
     * 统计各状态的授权数量
     *
     * @param status 授权状态
     * @return 数量
     */
    long countByStatus(String status);

    /**
     * 统计总授权数量
     *
     * @return 总数
     */
    @Query("SELECT COUNT(l) FROM License l")
    long countTotal();

    /**
     * 根据关键词搜索授权（序列号、授权码、授权给）
     *
     * @param keyword 搜索关键词
     * @return 匹配的授权列表
     */
    @Query("SELECT l FROM License l WHERE " +
            "l.serialNumber LIKE %:keyword% OR " +
            "l.licenseCode LIKE %:keyword% OR " +
            "l.issuedTo LIKE %:keyword% " +
            "ORDER BY l.createdAt DESC")
    List<License> searchLicenses(@Param("keyword") String keyword);
}
