package org.cyberlab.service;

import org.cyberlab.entity.ScenarioTemplate;
import org.cyberlab.repository.ScenarioTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ScenarioTemplateService {

    @Autowired
    private ScenarioTemplateRepository scenarioTemplateRepository;

    public List<ScenarioTemplate> getAllActiveTemplates() {
        return scenarioTemplateRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    public Page<ScenarioTemplate> getAllActiveTemplates(Pageable pageable) {
        return scenarioTemplateRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);
    }

    public Optional<ScenarioTemplate> getTemplateById(Long id) {
        return scenarioTemplateRepository.findById(id);
    }

    public ScenarioTemplate createTemplate(ScenarioTemplate template) {
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setIsActive(true);
        template.setUsageCount(0);
        return scenarioTemplateRepository.save(template);
    }

    public ScenarioTemplate updateTemplate(Long id, ScenarioTemplate updatedTemplate) {
        return scenarioTemplateRepository.findById(id)
            .map(template -> {
                template.setName(updatedTemplate.getName());
                template.setDescription(updatedTemplate.getDescription());
                template.setScenarioType(updatedTemplate.getScenarioType());
                template.setDifficultyLevel(updatedTemplate.getDifficultyLevel());
                template.setEstimatedDuration(updatedTemplate.getEstimatedDuration());
                template.setMaxParticipants(updatedTemplate.getMaxParticipants());
                template.setLearningObjectives(updatedTemplate.getLearningObjectives());
                template.setPrerequisites(updatedTemplate.getPrerequisites());
                template.setAssetConfig(updatedTemplate.getAssetConfig());
                template.setNetworkTopology(updatedTemplate.getNetworkTopology());
                template.setDeploymentOrder(updatedTemplate.getDeploymentOrder());
                template.setExerciseScript(updatedTemplate.getExerciseScript());
                template.setEvaluationCriteria(updatedTemplate.getEvaluationCriteria());
                template.setSuccessMetrics(updatedTemplate.getSuccessMetrics());
                template.setInstructorNotes(updatedTemplate.getInstructorNotes());
                template.setStudentGuidelines(updatedTemplate.getStudentGuidelines());
                template.setTags(updatedTemplate.getTags());
                template.setIsPublic(updatedTemplate.getIsPublic());
                template.setUpdatedAt(LocalDateTime.now());
                return scenarioTemplateRepository.save(template);
            })
            .orElse(null);
    }

    public boolean deleteTemplate(Long id) {
        return scenarioTemplateRepository.findById(id)
            .map(template -> {
                template.setIsActive(false);
                template.setUpdatedAt(LocalDateTime.now());
                scenarioTemplateRepository.save(template);
                return true;
            })
            .orElse(false);
    }

    public List<ScenarioTemplate> getTemplatesByScenarioType(String scenarioType) {
        return scenarioTemplateRepository.findByScenarioTypeAndIsActiveTrue(scenarioType);
    }

    public Page<ScenarioTemplate> getTemplatesByScenarioType(String scenarioType, Pageable pageable) {
        return scenarioTemplateRepository.findByScenarioTypeAndIsActiveTrue(scenarioType, pageable);
    }

    public List<ScenarioTemplate> getTemplatesByDifficultyLevel(String difficultyLevel) {
        return scenarioTemplateRepository.findByDifficultyLevelAndIsActiveTrue(difficultyLevel);
    }

    public List<ScenarioTemplate> getPublicTemplates() {
        return scenarioTemplateRepository.findByIsPublicTrueAndIsActiveTrue();
    }

    public Page<ScenarioTemplate> getPublicTemplates(Pageable pageable) {
        return scenarioTemplateRepository.findByIsPublicTrueAndIsActiveTrue(pageable);
    }

    public List<ScenarioTemplate> getTemplatesByCreator(Long createdBy) {
        return scenarioTemplateRepository.findByCreatedByAndIsActiveTrue(createdBy);
    }

    public Page<ScenarioTemplate> searchTemplates(String scenarioType, String difficultyLevel,
                                                Integer maxParticipants, Boolean isPublic,
                                                Pageable pageable) {
        return scenarioTemplateRepository.findByMultipleConditions(
            scenarioType, difficultyLevel, maxParticipants, isPublic, pageable);
    }

    public List<ScenarioTemplate> searchTemplatesByKeyword(String keyword) {
        return scenarioTemplateRepository.searchByKeyword(keyword);
    }

    public Page<ScenarioTemplate> searchTemplatesByKeyword(String keyword, Pageable pageable) {
        return scenarioTemplateRepository.searchByKeyword(keyword, pageable);
    }

    public List<ScenarioTemplate> getPopularTemplates() {
        return scenarioTemplateRepository.findByIsActiveTrueOrderByUsageCountDesc();
    }

    public Page<ScenarioTemplate> getPopularTemplates(Pageable pageable) {
        return scenarioTemplateRepository.findByIsActiveTrueOrderByUsageCountDesc(pageable);
    }

    public List<ScenarioTemplate> getRecentlyUsedTemplates() {
        return scenarioTemplateRepository.findByIsActiveTrueOrderByLastUsedAtDesc();
    }

    public List<ScenarioTemplate> getTemplatesByDuration(Integer minDuration, Integer maxDuration) {
        return scenarioTemplateRepository.findByEstimatedDurationBetween(minDuration, maxDuration);
    }

    public ScenarioTemplate useTemplate(Long id) {
        return scenarioTemplateRepository.findById(id)
            .map(template -> {
                template.incrementUsageCount();
                return scenarioTemplateRepository.save(template);
            })
            .orElse(null);
    }

    public ScenarioTemplate cloneTemplate(Long id, Long newCreatedBy, String newName) {
        return scenarioTemplateRepository.findById(id)
            .map(originalTemplate -> {
                ScenarioTemplate clonedTemplate = new ScenarioTemplate();
                clonedTemplate.setName(newName != null ? newName : "复制 - " + originalTemplate.getName());
                clonedTemplate.setDescription(originalTemplate.getDescription());
                clonedTemplate.setScenarioType(originalTemplate.getScenarioType());
                clonedTemplate.setDifficultyLevel(originalTemplate.getDifficultyLevel());
                clonedTemplate.setEstimatedDuration(originalTemplate.getEstimatedDuration());
                clonedTemplate.setMaxParticipants(originalTemplate.getMaxParticipants());
                clonedTemplate.setLearningObjectives(originalTemplate.getLearningObjectives());
                clonedTemplate.setPrerequisites(originalTemplate.getPrerequisites());
                clonedTemplate.setAssetConfig(originalTemplate.getAssetConfig());
                clonedTemplate.setNetworkTopology(originalTemplate.getNetworkTopology());
                clonedTemplate.setDeploymentOrder(originalTemplate.getDeploymentOrder());
                clonedTemplate.setExerciseScript(originalTemplate.getExerciseScript());
                clonedTemplate.setEvaluationCriteria(originalTemplate.getEvaluationCriteria());
                clonedTemplate.setSuccessMetrics(originalTemplate.getSuccessMetrics());
                clonedTemplate.setInstructorNotes(originalTemplate.getInstructorNotes());
                clonedTemplate.setStudentGuidelines(originalTemplate.getStudentGuidelines());
                clonedTemplate.setTags(originalTemplate.getTags());
                clonedTemplate.setIsPublic(false);
                clonedTemplate.setCreatedBy(newCreatedBy);
                return scenarioTemplateRepository.save(clonedTemplate);
            })
            .orElse(null);
    }

    public List<String> getAllScenarioTypes() {
        return scenarioTemplateRepository.findAllDistinctScenarioTypes();
    }

    public List<String> getAllDifficultyLevels() {
        return scenarioTemplateRepository.findAllDistinctDifficultyLevels();
    }

    public Map<String, Long> getScenarioTypeStatistics() {
        List<Object[]> results = scenarioTemplateRepository.countByScenarioType();
        return results.stream().collect(
            Collectors.toMap(
                result -> (String) result[0],
                result -> (Long) result[1]
            )
        );
    }

    public Map<String, Long> getDifficultyStatistics() {
        List<Object[]> results = scenarioTemplateRepository.countByDifficultyLevel();
        return results.stream().collect(
            Collectors.toMap(
                result -> (String) result[0],
                result -> (Long) result[1]
            )
        );
    }

    public List<ScenarioTemplate> getTopUsedTemplates(int limit) {
        return scenarioTemplateRepository.findTopUsedTemplates(
            org.springframework.data.domain.PageRequest.of(0, limit));
    }

    public boolean validateTemplateConfiguration(ScenarioTemplate template) {
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            return false;
        }
        if (template.getScenarioType() == null || template.getScenarioType().trim().isEmpty()) {
            return false;
        }
        if (template.getDifficultyLevel() == null || template.getDifficultyLevel().trim().isEmpty()) {
            return false;
        }
        if (template.getEstimatedDuration() == null || template.getEstimatedDuration() <= 0) {
            return false;
        }
        if (template.getMaxParticipants() == null || template.getMaxParticipants() <= 0) {
            return false;
        }
        return true;
    }
}