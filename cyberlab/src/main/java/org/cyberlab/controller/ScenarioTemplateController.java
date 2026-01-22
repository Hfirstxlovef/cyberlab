package org.cyberlab.controller;

import org.cyberlab.dto.PageResponse;
import org.cyberlab.entity.ScenarioTemplate;
import org.cyberlab.repository.UserRepository;
import org.cyberlab.service.ScenarioTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/scenario-templates")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class ScenarioTemplateController {

    @Autowired
    private ScenarioTemplateService scenarioTemplateService;

    @Autowired
    private UserRepository userRepository;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private Long getCurrentUserId() {
        String username = getCurrentUsername();
        if (username == null) {
            return 1L; // 默认返回admin用户ID
        }
        return userRepository.findByUsername(username)
                .map(user -> user.getId())
                .orElse(1L); // 如果找不到用户，默认返回admin用户ID
    }

    @GetMapping
    public ResponseEntity<List<ScenarioTemplate>> getAllTemplates() {
        return ResponseEntity.ok(scenarioTemplateService.getAllActiveTemplates());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<ScenarioTemplate>> getAllTemplatesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        return ResponseEntity.ok(PageResponse.of(scenarioTemplateService.getAllActiveTemplates(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScenarioTemplate> getTemplateById(@PathVariable Long id) {
        return scenarioTemplateService.getTemplateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ScenarioTemplate> createTemplate(@RequestBody ScenarioTemplate template) {
        Long currentUserId = getCurrentUserId();
        template.setCreatedBy(currentUserId);
        return ResponseEntity.ok(scenarioTemplateService.createTemplate(template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScenarioTemplate> updateTemplate(@PathVariable Long id, @RequestBody ScenarioTemplate template) {
        return ResponseEntity.ok(scenarioTemplateService.updateTemplate(id, template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        if (scenarioTemplateService.deleteTemplate(id)) {
            return ResponseEntity.ok("✅ 场景模板已删除");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/type/{scenarioType}")
    public ResponseEntity<List<ScenarioTemplate>> getTemplatesByType(@PathVariable String scenarioType) {
        return ResponseEntity.ok(scenarioTemplateService.getTemplatesByScenarioType(scenarioType));
    }

    @GetMapping("/type/{scenarioType}/paged")
    public ResponseEntity<PageResponse<ScenarioTemplate>> getTemplatesByTypePaged(
            @PathVariable String scenarioType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.of(scenarioTemplateService.getTemplatesByScenarioType(scenarioType, pageable)));
    }

    @GetMapping("/difficulty/{difficultyLevel}")
    public ResponseEntity<List<ScenarioTemplate>> getTemplatesByDifficulty(@PathVariable String difficultyLevel) {
        return ResponseEntity.ok(scenarioTemplateService.getTemplatesByDifficultyLevel(difficultyLevel));
    }

    @GetMapping("/public")
    public ResponseEntity<List<ScenarioTemplate>> getPublicTemplates() {
        return ResponseEntity.ok(scenarioTemplateService.getPublicTemplates());
    }

    @GetMapping("/public/paged")
    public ResponseEntity<PageResponse<ScenarioTemplate>> getPublicTemplatesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.of(scenarioTemplateService.getPublicTemplates(pageable)));
    }

    @GetMapping("/creator/{createdBy}")
    public ResponseEntity<List<ScenarioTemplate>> getTemplatesByCreator(@PathVariable Long createdBy) {
        return ResponseEntity.ok(scenarioTemplateService.getTemplatesByCreator(createdBy));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ScenarioTemplate>> searchTemplates(@RequestParam String keyword) {
        return ResponseEntity.ok(scenarioTemplateService.searchTemplatesByKeyword(keyword));
    }

    @GetMapping("/search/paged")
    public ResponseEntity<PageResponse<ScenarioTemplate>> searchTemplatesPaged(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.of(scenarioTemplateService.searchTemplatesByKeyword(keyword, pageable)));
    }

    @GetMapping("/filter")
    public ResponseEntity<PageResponse<ScenarioTemplate>> filterTemplates(
            @RequestParam(required = false) String scenarioType,
            @RequestParam(required = false) String difficultyLevel,
            @RequestParam(required = false) Integer maxParticipants,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.of(scenarioTemplateService.searchTemplates(
            scenarioType, difficultyLevel, maxParticipants, isPublic, pageable)));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<ScenarioTemplate>> getPopularTemplates() {
        return ResponseEntity.ok(scenarioTemplateService.getPopularTemplates());
    }

    @GetMapping("/popular/paged")
    public ResponseEntity<PageResponse<ScenarioTemplate>> getPopularTemplatesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.of(scenarioTemplateService.getPopularTemplates(pageable)));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ScenarioTemplate>> getRecentTemplates() {
        return ResponseEntity.ok(scenarioTemplateService.getRecentlyUsedTemplates());
    }

    @GetMapping("/duration")
    public ResponseEntity<List<ScenarioTemplate>> getTemplatesByDuration(
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Integer maxDuration) {
        return ResponseEntity.ok(scenarioTemplateService.getTemplatesByDuration(minDuration, maxDuration));
    }

    @PostMapping("/{id}/use")
    public ResponseEntity<ScenarioTemplate> useTemplate(@PathVariable Long id) {
        ScenarioTemplate template = scenarioTemplateService.useTemplate(id);
        if (template != null) {
            return ResponseEntity.ok(template);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ScenarioTemplate> cloneTemplate(
            @PathVariable Long id,
            @RequestParam(required = false) String newName) {
        String currentUser = getCurrentUsername();
        Long currentUserId = 1L; // 这里应该从用户服务获取当前用户ID
        ScenarioTemplate cloned = scenarioTemplateService.cloneTemplate(id, currentUserId, newName);
        if (cloned != null) {
            return ResponseEntity.ok(cloned);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getAllScenarioTypes() {
        return ResponseEntity.ok(scenarioTemplateService.getAllScenarioTypes());
    }

    @GetMapping("/difficulty-levels")
    public ResponseEntity<List<String>> getAllDifficultyLevels() {
        return ResponseEntity.ok(scenarioTemplateService.getAllDifficultyLevels());
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTemplateStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("scenarioTypeStats", scenarioTemplateService.getScenarioTypeStatistics());
        statistics.put("difficultyStats", scenarioTemplateService.getDifficultyStatistics());
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/top-used")
    public ResponseEntity<List<ScenarioTemplate>> getTopUsedTemplates(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(scenarioTemplateService.getTopUsedTemplates(limit));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateTemplate(@RequestBody ScenarioTemplate template) {
        Map<String, Object> response = new HashMap<>();
        boolean isValid = scenarioTemplateService.validateTemplateConfiguration(template);
        response.put("valid", isValid);
        
        if (!isValid) {
            response.put("errors", List.of(
                template.getName() == null || template.getName().trim().isEmpty() ? "模板名称不能为空" : null,
                template.getScenarioType() == null || template.getScenarioType().trim().isEmpty() ? "场景类型不能为空" : null,
                template.getDifficultyLevel() == null || template.getDifficultyLevel().trim().isEmpty() ? "难度等级不能为空" : null,
                template.getEstimatedDuration() == null || template.getEstimatedDuration() <= 0 ? "预计演练时间必须大于0" : null,
                template.getMaxParticipants() == null || template.getMaxParticipants() <= 0 ? "最大参与人数必须大于0" : null
            ).stream().filter(error -> error != null).toList());
        }
        
        return ResponseEntity.ok(response);
    }
}