package org.cyberlab.controller;

import org.cyberlab.dto.ReviewApplicationRequest;
import org.cyberlab.dto.SubmitApplicationRequest;
import org.cyberlab.entity.TeamApplication;
import org.cyberlab.entity.User;
import org.cyberlab.repository.UserRepository;
import org.cyberlab.service.TeamApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-applications")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class TeamApplicationController {

    @Autowired
    private TeamApplicationService applicationService;

    @Autowired
    private UserRepository userRepo;

    // ==================== Application Management ====================

    /**
     * 提交加入战队申请（红队/蓝队用户）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('red', 'blue')")
    public ResponseEntity<?> submitApplication(
            @RequestBody SubmitApplicationRequest request,
            Authentication authentication) {
        try {
            // 获取当前用户
            String username = authentication.getName();
            User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
            Long userId = user.getId();

            TeamApplication application = applicationService.submitApplication(
                request.getTeamId(),
                userId,
                request.getMessage()
            );
            return ResponseEntity.ok(application);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("提交申请失败: " + e.getMessage());
        }
    }

    /**
     * 批准申请（战队队长）
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('red', 'blue')")
    public ResponseEntity<?> approveApplication(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            // 获取当前用户
            String username = authentication.getName();
            User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
            Long reviewerId = user.getId();

            applicationService.approveApplication(id, reviewerId);
            return ResponseEntity.ok("申请已批准");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("批准申请失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝申请（战队队长）
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('red', 'blue')")
    public ResponseEntity<?> rejectApplication(
            @PathVariable Long id,
            @RequestBody ReviewApplicationRequest request,
            Authentication authentication) {
        try {
            // 获取当前用户
            String username = authentication.getName();
            User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
            Long reviewerId = user.getId();

            applicationService.rejectApplication(id, reviewerId, request.getReason());
            return ResponseEntity.ok("申请已拒绝");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("拒绝申请失败: " + e.getMessage());
        }
    }

    /**
     * 撤回申请（申请人）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('red', 'blue')")
    public ResponseEntity<?> withdrawApplication(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            // 获取当前用户
            String username = authentication.getName();
            User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
            Long userId = user.getId();

            applicationService.withdrawApplication(id, userId);
            return ResponseEntity.ok("申请已撤回");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("撤回申请失败: " + e.getMessage());
        }
    }

    // ==================== Query Applications ====================

    /**
     * 获取战队的所有申请（队长和管理员）
     */
    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('admin', 'red', 'blue')")
    public ResponseEntity<?> getTeamApplications(@PathVariable Long teamId) {
        try {
            List<TeamApplication> applications = applicationService.getTeamApplications(teamId);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取战队的待处理申请（队长和管理员）
     */
    @GetMapping("/team/{teamId}/pending")
    @PreAuthorize("hasAnyRole('admin', 'red', 'blue')")
    public ResponseEntity<?> getPendingApplicationsByTeam(@PathVariable Long teamId) {
        try {
            List<TeamApplication> applications = applicationService.getPendingApplicationsByTeam(teamId);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户的所有申请
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('red', 'blue')")
    public ResponseEntity<?> getMyApplications(Authentication authentication) {
        try {
            // 获取当前用户
            String username = authentication.getName();
            User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
            Long userId = user.getId();

            List<TeamApplication> applications = applicationService.getUserApplications(userId);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户的待处理申请
     */
    @GetMapping("/my/pending")
    @PreAuthorize("hasAnyRole('red', 'blue')")
    public ResponseEntity<?> getMyPendingApplications(Authentication authentication) {
        try {
            // 获取当前用户
            String username = authentication.getName();
            User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
            Long userId = user.getId();

            List<TeamApplication> applications = applicationService.getPendingApplicationsByUser(userId);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取申请详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'red', 'blue')")
    public ResponseEntity<?> getApplicationById(@PathVariable Long id) {
        TeamApplication application = applicationService.getApplicationById(id);
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(application);
    }

    // ==================== Statistics ====================

    /**
     * 统计战队的待处理申请数量
     */
    @GetMapping("/team/{teamId}/pending/count")
    @PreAuthorize("hasAnyRole('admin', 'red', 'blue')")
    public ResponseEntity<?> countPendingApplications(@PathVariable Long teamId) {
        try {
            long count = applicationService.countPendingApplications(teamId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("查询失败: " + e.getMessage());
        }
    }

    /**
     * 检查当前用户是否有待处理的申请
     */
    @GetMapping("/my/pending/exists")
    @PreAuthorize("hasAnyRole('red', 'blue')")
    public ResponseEntity<?> hasPendingApplication(Authentication authentication) {
        try {
            // 获取当前用户
            String username = authentication.getName();
            User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
            Long userId = user.getId();

            boolean hasPending = applicationService.hasPendingApplication(userId);
            return ResponseEntity.ok(hasPending);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("查询失败: " + e.getMessage());
        }
    }
}
