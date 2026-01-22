package org.cyberlab.controller;

import org.cyberlab.dto.UserBasicDTO;
import org.cyberlab.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户信息查询控制器
 * 提供公共接口供所有认证用户访问基本用户信息（不包含敏感数据）
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取所有用户的基本信息（不包含密码）
     * 所有认证用户都可以访问此接口，用于显示用户名等基本信息
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserBasicDTO>> getUsers(@RequestParam(required = false) String role) {
        try {
            if (role != null && !role.trim().isEmpty()) {
                // 按角色筛选
                return ResponseEntity.ok(userService.getUsersBasicByRole(role));
            } else {
                // 返回所有用户的基本信息
                return ResponseEntity.ok(userService.getAllUsersBasic());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
