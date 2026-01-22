package org.cyberlab.controller;

import org.cyberlab.dto.CreateUserRequest;
import org.cyberlab.dto.UpdateUserRequest;
import org.cyberlab.entity.User;
import org.cyberlab.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class AdminController {

    @Autowired
    private UserService userService;

    // 获取所有用户 - 管理员、裁判和授权管理员都可以访问
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'judge', 'license_admin')")
    public ResponseEntity<List<User>> getAllUsers(@RequestParam(required = false) String role) {
        if (role != null && !role.trim().isEmpty()) {
            // 按角色筛选
            try {
                return ResponseEntity.ok(userService.getUsersByRole(role));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            // 返回所有用户
            return ResponseEntity.ok(userService.getAllUsers());
        }
    }
    
    // 新增：创建用户
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            User user = userService.createUser(
                request.getUsername(), 
                request.getPassword(), 
                request.getRole()
            );
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("创建用户失败");
        }
    }
    
    // 新增：更新用户
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        try {
            User user = userService.updateUser(
                id,
                request.getUsername(),
                request.getRole(),
                request.getEnabled()
            );
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("更新用户失败");
        }
    }

    // 删除指定用户
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.ok("用户已删除");
        } else {
            return ResponseEntity.badRequest().body("用户不存在");
        }
    }

    // 切换启用状态
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> toggleUserEnabled(@PathVariable Long id) {
        boolean toggled = userService.toggleUserStatus(id);
        if (toggled) {
            return ResponseEntity.ok("用户状态已更新");
        } else {
            return ResponseEntity.badRequest().body("用户不存在");
        }
    }
}