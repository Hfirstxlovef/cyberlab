package org.cyberlab.service;

import org.cyberlab.entity.DrillAsset;
import org.cyberlab.repository.DrillAssetRepository;
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
public class DrillAssetService {

    @Autowired
    private DrillAssetRepository drillAssetRepository;

    public List<DrillAsset> getAllActiveAssets() {
        return drillAssetRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    public Page<DrillAsset> getAllActiveAssets(Pageable pageable) {
        return drillAssetRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);
    }

    public Optional<DrillAsset> getAssetById(Long id) {
        return drillAssetRepository.findById(id);
    }

    public DrillAsset createAsset(DrillAsset asset) {
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        asset.setIsActive(true);
        return drillAssetRepository.save(asset);
    }

    public DrillAsset updateAsset(Long id, DrillAsset updatedAsset) {
        return drillAssetRepository.findById(id)
            .map(asset -> {
                asset.setName(updatedAsset.getName());
                asset.setCategory(updatedAsset.getCategory());
                asset.setDescription(updatedAsset.getDescription());
                asset.setDockerImage(updatedAsset.getDockerImage());
                asset.setDefaultPort(updatedAsset.getDefaultPort());
                asset.setExposedPorts(updatedAsset.getExposedPorts());
                asset.setEnvironmentVars(updatedAsset.getEnvironmentVars());
                asset.setVolumeMounts(updatedAsset.getVolumeMounts());
                asset.setNetworkConfig(updatedAsset.getNetworkConfig());
                asset.setSecurityConfig(updatedAsset.getSecurityConfig());
                asset.setResourceLimits(updatedAsset.getResourceLimits());
                asset.setVulnerabilityType(updatedAsset.getVulnerabilityType());
                asset.setDifficultyLevel(updatedAsset.getDifficultyLevel());
                asset.setAttackVector(updatedAsset.getAttackVector());
                asset.setTeamVisibility(updatedAsset.getTeamVisibility());
                asset.setIsTarget(updatedAsset.getIsTarget());
                asset.setSetupInstructions(updatedAsset.getSetupInstructions());
                asset.setExerciseInstructions(updatedAsset.getExerciseInstructions());
                asset.setSolutionHints(updatedAsset.getSolutionHints());
                asset.setTags(updatedAsset.getTags());
                asset.setUpdatedAt(LocalDateTime.now());
                return drillAssetRepository.save(asset);
            })
            .orElse(null);
    }

    public boolean deleteAsset(Long id) {
        return drillAssetRepository.findById(id)
            .map(asset -> {
                asset.setIsActive(false);
                asset.setUpdatedAt(LocalDateTime.now());
                drillAssetRepository.save(asset);
                return true;
            })
            .orElse(false);
    }

    public List<DrillAsset> getAssetsByCategory(String category) {
        return drillAssetRepository.findByCategoryAndIsActiveTrue(category);
    }

    public Page<DrillAsset> getAssetsByCategory(String category, Pageable pageable) {
        return drillAssetRepository.findByCategoryAndIsActiveTrue(category, pageable);
    }

    public List<DrillAsset> getAssetsByDifficultyLevel(String difficultyLevel) {
        return drillAssetRepository.findByDifficultyLevelAndIsActiveTrue(difficultyLevel);
    }

    public List<DrillAsset> getAssetsByTeamVisibility(String teamVisibility) {
        return drillAssetRepository.findByTeamVisibilityOrBoth(teamVisibility);
    }

    public List<DrillAsset> getAssetsByVulnerabilityType(String vulnerabilityType) {
        return drillAssetRepository.findByVulnerabilityTypeAndIsActiveTrue(vulnerabilityType);
    }

    public List<DrillAsset> getTargetAssets() {
        return drillAssetRepository.findByIsTargetAndIsActiveTrue(true);
    }

    public List<DrillAsset> getNonTargetAssets() {
        return drillAssetRepository.findByIsTargetAndIsActiveTrue(false);
    }

    public Page<DrillAsset> searchAssets(String category, String difficultyLevel, 
                                       String vulnerabilityType, String teamVisibility, 
                                       Boolean isTarget, Pageable pageable) {
        return drillAssetRepository.findByMultipleConditions(
            category, difficultyLevel, vulnerabilityType, teamVisibility, isTarget, pageable);
    }

    public List<DrillAsset> searchAssetsByKeyword(String keyword) {
        return drillAssetRepository.searchByKeyword(keyword);
    }

    public Page<DrillAsset> searchAssetsByKeyword(String keyword, Pageable pageable) {
        return drillAssetRepository.searchByKeyword(keyword, pageable);
    }

    public List<DrillAsset> getAssetsByCreator(Long createdBy) {
        return drillAssetRepository.findByCreatedByAndIsActiveTrue(createdBy);
    }

    public List<String> getAllCategories() {
        return drillAssetRepository.findAllDistinctCategories();
    }

    public List<String> getAllDifficultyLevels() {
        return drillAssetRepository.findAllDistinctDifficultyLevels();
    }

    public List<String> getAllVulnerabilityTypes() {
        return drillAssetRepository.findAllDistinctVulnerabilityTypes();
    }

    public Map<String, Long> getCategoryStatistics() {
        List<Object[]> results = drillAssetRepository.countByCategory();
        return results.stream().collect(
            Collectors.toMap(
                result -> (String) result[0],
                result -> (Long) result[1]
            )
        );
    }

    public Map<String, Long> getDifficultyStatistics() {
        List<Object[]> results = drillAssetRepository.countByDifficultyLevel();
        return results.stream().collect(
            Collectors.toMap(
                result -> (String) result[0],
                result -> (Long) result[1]
            )
        );
    }

    public long getTotalActiveAssetsCount() {
        return drillAssetRepository.countByIsActiveTrue();
    }

    public long getTargetAssetsCount() {
        return drillAssetRepository.findByIsTargetAndIsActiveTrue(true).size();
    }

    public boolean validateAssetConfiguration(DrillAsset asset) {
        if (asset.getName() == null || asset.getName().trim().isEmpty()) {
            return false;
        }
        if (asset.getCategory() == null || asset.getCategory().trim().isEmpty()) {
            return false;
        }
        if (asset.getDockerImage() == null || asset.getDockerImage().trim().isEmpty()) {
            return false;
        }
        if (asset.getDifficultyLevel() == null || asset.getDifficultyLevel().trim().isEmpty()) {
            return false;
        }
        if (asset.getTeamVisibility() == null || asset.getTeamVisibility().trim().isEmpty()) {
            return false;
        }
        return true;
    }
}