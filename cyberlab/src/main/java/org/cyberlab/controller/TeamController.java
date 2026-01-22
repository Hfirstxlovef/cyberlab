package org.cyberlab.controller;

import org.cyberlab.dto.AddMemberRequest;
import org.cyberlab.dto.CreateTeamRequest;
import org.cyberlab.dto.UpdateTeamRequest;
import org.cyberlab.entity.Team;
import org.cyberlab.entity.TeamMember;
import org.cyberlab.entity.User;
import org.cyberlab.repository.UserRepository;
import org.cyberlab.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserRepository userRepo;

    // ==================== Team CRUD ====================

    /**
     * 获取所有战队（可按类型筛选）
     * 管理员可查看所有，红蓝队只能查看对应类型
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'judge', 'red', 'blue')")
    public ResponseEntity<List<Team>> getAllTeams(@RequestParam(required = false) String teamType) {
        if (teamType != null && !teamType.trim().isEmpty()) {
            try {
                return ResponseEntity.ok(teamService.getTeamsByType(teamType));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.ok(teamService.getAllTeams());
        }
    }

    /**
     * 获取活跃战队（按类型）
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('admin', 'judge', 'red', 'blue')")
    public ResponseEntity<List<Team>> getActiveTeams(@RequestParam String teamType) {
        try {
            return ResponseEntity.ok(teamService.getActiveTeamsByType(teamType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 根据ID获取战队详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'judge', 'red', 'blue')")
    public ResponseEntity<?> getTeamById(@PathVariable Long id) {
        Team team = teamService.getTeamById(id);
        if (team == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(team);
    }

    /**
     * 创建战队（仅管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> createTeam(@RequestBody CreateTeamRequest request) {
        try {
            Team team = teamService.createTeam(
                request.getName(),
                request.getLeaderId(),
                request.getDescription(),
                request.getTeamType()
            );
            return ResponseEntity.ok(team);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("创建战队失败: " + e.getMessage());
        }
    }

    /**
     * 更新战队信息（仅管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> updateTeam(@PathVariable Long id, @RequestBody UpdateTeamRequest request) {
        try {
            Team team = teamService.updateTeam(
                id,
                request.getName(),
                request.getLeaderId(),
                request.getDescription(),
                request.getStatus()
            );
            return ResponseEntity.ok(team);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("更新战队失败: " + e.getMessage());
        }
    }

    /**
     * 删除战队（仅管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> deleteTeam(@PathVariable Long id) {
        try {
            boolean deleted = teamService.deleteTeam(id);
            if (deleted) {
                return ResponseEntity.ok("战队已删除");
            } else {
                return ResponseEntity.badRequest().body("战队不存在");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("删除战队失败: " + e.getMessage());
        }
    }

    /**
     * 切换战队状态（仅管理员）
     */
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> toggleTeamStatus(@PathVariable Long id) {
        try {
            boolean toggled = teamService.toggleTeamStatus(id);
            if (toggled) {
                return ResponseEntity.ok("战队状态已更新");
            } else {
                return ResponseEntity.badRequest().body("战队不存在");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("更新状态失败: " + e.getMessage());
        }
    }

    // ==================== Team Member Management ====================

    /**
     * 获取战队成员列表
     */
    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('admin', 'judge', 'red', 'blue')")
    public ResponseEntity<List<TeamMember>> getTeamMembers(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(teamService.getTeamMembers(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 添加战队成员（仅管理员）
     */
    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> addMember(@PathVariable Long id, @RequestBody AddMemberRequest request) {
        try {
            TeamMember member = teamService.addMemberToTeam(id, request.getUserId());
            return ResponseEntity.ok(member);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("添加成员失败: " + e.getMessage());
        }
    }

    /**
     * 移除战队成员（管理员或队长）
     */
    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('admin', 'red', 'blue')")
    public ResponseEntity<?> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {
        try {
            // 获取当前用户username
            String username = authentication.getName();

            // 通过username查询User对象
            User currentUser = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));

            Long currentUserId = currentUser.getId();

            // 判断是否为管理员
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_admin"));

            boolean removed = teamService.removeMemberFromTeam(id, userId, currentUserId, isAdmin);
            if (removed) {
                return ResponseEntity.ok("成员已移除");
            } else {
                return ResponseEntity.badRequest().body("成员不存在");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("移除成员失败: " + e.getMessage());
        }
    }

    /**
     * 获取战队成员数量
     */
    @GetMapping("/{id}/members/count")
    @PreAuthorize("hasAnyRole('admin', 'judge', 'red', 'blue')")
    public ResponseEntity<Long> getTeamMemberCount(@PathVariable Long id) {
        try {
            long count = teamService.getTeamMemberCount(id);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据用户ID获取所在战队
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('admin', 'judge', 'red', 'blue')")
    public ResponseEntity<?> getTeamByUserId(@PathVariable Long userId) {
        try {
            Team team = teamService.getTeamByUserId(userId);
            // 用户未加入战队是正常业务状态，返回200+null而不是404
            // 前端会根据null判断用户是否加入战队
            return ResponseEntity.ok(team);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("查询失败: " + e.getMessage());
        }
    }

    // ==================== Statistics ====================

    /**
     * 统计战队数量
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('admin', 'judge')")
    public ResponseEntity<Long> countTeams(@RequestParam(required = false) String teamType) {
        try {
            if (teamType != null && !teamType.trim().isEmpty()) {
                return ResponseEntity.ok(teamService.countTeamsByType(teamType));
            } else {
                return ResponseEntity.ok(teamService.countActiveTeams());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
