package org.cyberlab.repository;

import org.cyberlab.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    // 新增：按角色查询用户
    List<User> findByRole(String role);
    
    // 新增：按角色和启用状态查询用户
    List<User> findByRoleAndEnabled(String role, boolean enabled);
    
    // 在现有的UserRepository中添加以下方法
    long countByEnabled(boolean enabled);
    
    // 按角色统计用户数量
    long countByRole(String role);
}