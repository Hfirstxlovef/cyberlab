package org.cyberlab.service;

import org.cyberlab.dto.UserBasicDTO;
import org.cyberlab.entity.User;
import org.cyberlab.enums.UserRole;
import org.cyberlab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    // 获取所有用户
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }
    
    // 新增：按角色获取用户
    public List<User> getUsersByRole(String role) {
        // 验证角色是否有效
        try {
            UserRole.fromCode(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的角色: " + role);
        }
        return userRepo.findByRole(role);
    }
    
    // 新增：创建用户
    public User createUser(String username, String password, String role) {
        // 验证角色是否有效
        try {
            UserRole.fromCode(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的角色: " + role);
        }
        
        // 检查用户名是否已存在
        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        
        return userRepo.save(user);
    }
    
    // 新增：更新用户
    public User updateUser(Long id, String username, String role, Boolean enabled) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
            
        if (username != null && !username.trim().isEmpty()) {
            // 检查用户名是否被其他用户使用
            userRepo.findByUsername(username).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(id)) {
                    throw new IllegalArgumentException("用户名已存在: " + username);
                }
            });
            user.setUsername(username);
        }
        
        if (role != null) {
            // 验证角色是否有效
            try {
                UserRole.fromCode(role);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的角色: " + role);
            }
            user.setRole(role);
        }
        
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        
        return userRepo.save(user);
    }

    // 删除指定 ID 用户
    public boolean deleteUser(Long id) {
        if (!userRepo.existsById(id)) {
            return false;
        }
        userRepo.deleteById(id);
        return true;
    }

    // 启用 / 禁用 用户状态（切换）
    public boolean toggleUserStatus(Long id) {
        return userRepo.findById(id).map(user -> {
            user.setEnabled(!user.isEnabled());
            userRepo.save(user);
            return true;
        }).orElse(false);
    }

    // 根据 ID 获取用户
    public User getUserById(Long id) {
        return userRepo.findById(id).orElse(null);
    }

    /**
     * 获取所有用户的基本信息（不包含密码）
     * 用于公共接口，供所有认证用户访问
     */
    public List<UserBasicDTO> getAllUsersBasic() {
        List<User> users = userRepo.findAll();
        return users.stream()
                .map(UserBasicDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * 按角色获取用户基本信息（不包含密码）
     * 用于公共接口，供所有认证用户访问
     */
    public List<UserBasicDTO> getUsersBasicByRole(String role) {
        // 验证角色是否有效
        try {
            UserRole.fromCode(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的角色: " + role);
        }

        List<User> users = userRepo.findByRole(role);
        return users.stream()
                .map(UserBasicDTO::new)
                .collect(Collectors.toList());
    }
}