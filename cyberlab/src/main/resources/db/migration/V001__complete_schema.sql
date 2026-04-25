-- V001__complete_schema.sql
-- 完整数据库结构（基于 cyberlab.sql 权威版本，2026-04-21）
-- IF NOT EXISTS 保证幂等，新用户全量建表，存量用户重启不报错

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- achievements
CREATE TABLE IF NOT EXISTS `achievements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attack_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `proof_files` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `range_id` bigint NOT NULL,
  `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `review_time` datetime(6) DEFAULT NULL,
  `reviewer_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `screenshots` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `submit_time` datetime(6) NOT NULL,
  `target_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `team_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `attack_report_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `recording_time_range` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_recording_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `achievement_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `base_score` int DEFAULT NULL,
  `final_score` int DEFAULT NULL,
  `score_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `team_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_achievements_range_id` (`range_id`),
  KEY `idx_achievements_team_name` (`team_name`),
  KEY `idx_achievements_team_type` (`team_type`),
  KEY `idx_achievements_status` (`status`),
  KEY `idx_achievements_composite` (`range_id`,`team_type`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- asset
CREATE TABLE IF NOT EXISTS `asset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_target` bit(1) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `visibility` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `topology_project_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deployment_strategy` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `preferred_host_node_id` bigint DEFAULT NULL,
  `preferred_host_node_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `asset_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `container_command` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `container_ports` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `docker_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `environments` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `health_check_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resource_limit_cpu` int DEFAULT NULL,
  `resource_limit_memory` bigint DEFAULT NULL,
  `volumes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `affinity_rules` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `anti_affinity_rules` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deployment_priority` int DEFAULT NULL,
  `enable_failover` bit(1) DEFAULT NULL,
  `failover_strategy` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fallback_host_node_id` bigint DEFAULT NULL,
  `asset_platform` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `docker_api_enabled` bit(1) DEFAULT NULL,
  `docker_port` int DEFAULT NULL,
  `k8s_api_server` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `k8s_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_probe_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `probe_error_message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `probe_status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `k8s_port` int DEFAULT '6443' COMMENT 'Kubernetes API端口，默认6443',
  PRIMARY KEY (`id`),
  KEY `idx_asset_topology_project_id` (`topology_project_id`),
  KEY `idx_asset_enabled` (`enabled`),
  KEY `idx_asset_asset_type` (`asset_type`),
  KEY `idx_asset_preferred_host_node_id` (`preferred_host_node_id`),
  KEY `idx_asset_composite` (`topology_project_id`,`enabled`,`asset_type`),
  KEY `idx_asset_k8s_port` (`k8s_port`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- cluster_node
CREATE TABLE IF NOT EXISTS `cluster_node` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cpu_cores` int DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `current_containers` int DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `disk_gb` int DEFAULT NULL,
  `docker_version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `health_check_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `labels` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `last_health_check` datetime(6) DEFAULT NULL,
  `max_containers` int DEFAULT NULL,
  `memory_gb` int DEFAULT NULL,
  `node_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `node_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `node_port` int DEFAULT NULL,
  `node_status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `node_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `os_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tls_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `update_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7f6pvpopf2hmdbh5i5c354m4h` (`node_name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- container_discovery_records
CREATE TABLE IF NOT EXISTS `container_discovery_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` varchar(255) NOT NULL COMMENT '项目ID（格式：企业｜项目）',
  `asset_id` bigint DEFAULT NULL COMMENT '关联的资产ID',
  `asset_name` varchar(255) DEFAULT NULL COMMENT '资产名称',
  `asset_ip` varchar(100) DEFAULT NULL COMMENT '资产IP地址',
  `container_id` varchar(255) NOT NULL COMMENT 'Docker容器ID（完整ID）',
  `container_name` varchar(255) DEFAULT NULL COMMENT '容器名称',
  `image` varchar(500) DEFAULT NULL COMMENT '容器镜像名称',
  `status` varchar(50) DEFAULT NULL COMMENT '容器状态：running/exited/paused/dead',
  `ports` text COMMENT '端口映射（JSON格式）',
  `labels` text COMMENT '容器标签（JSON格式）',
  `created_at` timestamp NULL DEFAULT NULL COMMENT '容器创建时间',
  `discovered_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次发现时间',
  `last_seen_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后探测时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_container` (`project_id`,`container_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_container_id` (`container_id`),
  KEY `idx_last_seen` (`last_seen_at` DESC),
  KEY `idx_project_container` (`project_id`,`container_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='容器探测记录表-增量同步';

-- container_states
CREATE TABLE IF NOT EXISTS `container_states` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_id` bigint DEFAULT NULL,
  `container_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `container_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `current_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `desired_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `health_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `host_node_id` bigint DEFAULT NULL,
  `image_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_sync_at` datetime(6) DEFAULT NULL,
  `max_sync_attempts` int DEFAULT NULL,
  `sync_attempts` int DEFAULT NULL,
  `sync_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `sync_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_container_states_asset_id` (`asset_id`),
  KEY `idx_container_states_host_node_id` (`host_node_id`),
  KEY `idx_container_states_sync_status` (`sync_status`),
  KEY `idx_container_states_current_status` (`current_status`),
  KEY `idx_container_states_composite` (`sync_status`,`current_status`,`host_node_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- cyber_range
CREATE TABLE IF NOT EXISTS `cyber_range` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `topology_config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `vulnerability_config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `creator_id` bigint DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `difficulty_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `drill_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `max_participants` int DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `topology_project_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- domain_config
CREATE TABLE IF NOT EXISTS `domain_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `domain` varchar(255) NOT NULL COMMENT '域名 (支持通配符 *.example.com)',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `description` varchar(500) DEFAULT NULL COMMENT '域名描述',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人 (管理员用户名)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `domain` (`domain`),
  KEY `idx_domain` (`domain`),
  KEY `idx_active` (`is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='授权域名配置表';

-- drill_asset_image_mapping
CREATE TABLE IF NOT EXISTS `drill_asset_image_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `image_full_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_tag` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `range_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1dqr45rv1q8rtb4laix471p3x` (`range_id`,`asset_id`,`image_id`),
  KEY `idx_range_id` (`range_id`),
  KEY `idx_asset_id` (`asset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- drill_assets
CREATE TABLE IF NOT EXISTS `drill_assets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attack_vector` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `category` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `default_port` int DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `difficulty_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `docker_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `environment_vars` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `exercise_instructions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `exposed_ports` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `is_active` bit(1) NOT NULL,
  `is_target` bit(1) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `network_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `resource_limits` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `security_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `setup_instructions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `solution_hints` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `team_visibility` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `volume_mounts` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `vulnerability_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- drill_containers
CREATE TABLE IF NOT EXISTS `drill_containers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `range_id` bigint NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `container_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `port` int DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deploy_log` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `log_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `deploy_time` datetime DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `stop_time` datetime DEFAULT NULL,
  `asset_id` bigint DEFAULT NULL,
  `auto_start` bit(1) NOT NULL,
  `environment_vars` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `exposed_ports` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `health_check_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `network_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `resource_limits` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `restart_policy` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scenario_template_id` bigint DEFAULT NULL,
  `security_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `volume_mounts` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `asset_company` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `asset_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `asset_project` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `cluster_node_id` bigint DEFAULT NULL,
  `cluster_node_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `container_display_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `deployment_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `host_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  `container_full_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `host_node_id` bigint DEFAULT NULL,
  `host_node_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `host_node_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_range_name` (`range_id`,`name`),
  KEY `idx_range_id` (`range_id`),
  KEY `idx_container_id` (`container_id`),
  KEY `idx_status` (`status`),
  KEY `idx_drill_containers_range_id` (`range_id`),
  KEY `idx_drill_containers_status` (`status`),
  KEY `idx_drill_containers_host_node_id` (`host_node_id`),
  KEY `idx_drill_containers_composite` (`range_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- exception_records
CREATE TABLE IF NOT EXISTS `exception_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exception_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `http_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_occurred_at` datetime(6) DEFAULT NULL,
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `occurred_at` datetime(6) DEFAULT NULL,
  `occurrence_count` int DEFAULT NULL,
  `request_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resolution` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `resolved_at` datetime(6) DEFAULT NULL,
  `resolved_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stack_trace` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- fingerprint_blacklist
CREATE TABLE IF NOT EXISTS `fingerprint_blacklist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fingerprint` varchar(64) NOT NULL COMMENT '浏览器指纹 (SHA-256)',
  `block_reason` text COMMENT '封禁原因',
  `anomaly_count` int DEFAULT '1' COMMENT '累计异常次数',
  `first_seen_ip` varchar(45) DEFAULT NULL COMMENT '首次出现的IP',
  `last_seen_ip` varchar(45) DEFAULT NULL COMMENT '最后出现的IP',
  `block_start_time` datetime NOT NULL COMMENT '封禁开始时间',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否当前生效',
  `unblock_by` varchar(50) DEFAULT NULL COMMENT '解封管理员',
  `unblock_time` datetime DEFAULT NULL COMMENT '解封时间',
  `unblock_reason` text COMMENT '解封原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `fingerprint` (`fingerprint`),
  KEY `idx_fingerprint` (`fingerprint`),
  KEY `idx_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='浏览器指纹黑名单表';

-- host_nodes
CREATE TABLE IF NOT EXISTS `host_nodes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cluster_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cpu_cores` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `disk_total` bigint DEFAULT NULL,
  `display_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `docker_cert_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `docker_port` int DEFAULT NULL,
  `docker_tls_enabled` bit(1) DEFAULT NULL,
  `environment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `host_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `labels` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `last_health_check` datetime(6) DEFAULT NULL,
  `max_containers` int DEFAULT NULL,
  `memory_total` bigint DEFAULT NULL,
  `metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `monitoring_enabled` bit(1) DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `node_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` int DEFAULT NULL,
  `ssh_key_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ssh_port` int DEFAULT NULL,
  `ssh_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgugh0b5yoa6d7689ndrfwrdn3` (`name`),
  KEY `idx_host_nodes_status` (`status`),
  KEY `idx_host_nodes_last_health_check` (`last_health_check`),
  KEY `idx_host_nodes_composite` (`status`,`last_health_check`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ip_blacklist
CREATE TABLE IF NOT EXISTS `ip_blacklist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ip_address` varchar(45) NOT NULL COMMENT 'IP地址',
  `block_reason` text COMMENT '封禁原因',
  `anomaly_count` int DEFAULT '1' COMMENT '累计异常次数',
  `block_type` varchar(50) NOT NULL COMMENT '封禁类型: TEMPORARY_24H, PERMANENT',
  `block_start_time` datetime NOT NULL COMMENT '封禁开始时间',
  `block_end_time` datetime DEFAULT NULL COMMENT '临时封禁的解封时间',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否当前生效',
  `unblock_by` varchar(50) DEFAULT NULL COMMENT '解封管理员',
  `unblock_time` datetime DEFAULT NULL COMMENT '解封时间',
  `unblock_reason` text COMMENT '解封原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ip_address` (`ip_address`),
  KEY `idx_ip` (`ip_address`),
  KEY `idx_active` (`is_active`),
  KEY `idx_block_type` (`block_type`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IP黑名单表';

-- js_anomaly_record
CREATE TABLE IF NOT EXISTS `js_anomaly_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(100) NOT NULL COMMENT '会话ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID (可为空)',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `ip_address` varchar(45) NOT NULL COMMENT 'IP地址',
  `browser_fingerprint` varchar(64) DEFAULT NULL COMMENT '浏览器指纹',
  `user_agent` text COMMENT 'User-Agent信息',
  `last_heartbeat_time` datetime DEFAULT NULL COMMENT '最后一次心跳时间',
  `detected_time` datetime NOT NULL COMMENT '异常检测时间',
  `anomaly_type` varchar(50) NOT NULL COMMENT '异常类型: HEARTBEAT_TIMEOUT, JS_DISABLED',
  `failure_count` int DEFAULT '1' COMMENT '10分钟内失败次数',
  `geo_country` varchar(50) DEFAULT NULL COMMENT '国家',
  `geo_province` varchar(50) DEFAULT NULL COMMENT '省份/州',
  `geo_city` varchar(50) DEFAULT NULL COMMENT '城市',
  `is_resolved` tinyint(1) DEFAULT '0' COMMENT '是否已解决',
  `resolved_by` varchar(50) DEFAULT NULL COMMENT '解决人 (管理员用户名)',
  `resolved_at` datetime DEFAULT NULL COMMENT '解决时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_ip` (`ip_address`),
  KEY `idx_detected_time` (`detected_time`),
  KEY `idx_resolved` (`is_resolved`),
  KEY `idx_anomaly_type` (`anomaly_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='JS异常行为记录表';

-- js_heartbeat
CREATE TABLE IF NOT EXISTS `js_heartbeat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(100) NOT NULL COMMENT '会话ID (前端生成)',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID (可为空-未登录用户)',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `ip_address` varchar(45) NOT NULL COMMENT 'IP地址 (支持IPv6)',
  `user_agent` text COMMENT 'User-Agent信息',
  `browser_fingerprint` varchar(64) DEFAULT NULL COMMENT '浏览器指纹 (SHA-256)',
  `url` varchar(500) DEFAULT NULL COMMENT '当前访问URL',
  `timestamp` bigint NOT NULL COMMENT '前端心跳时间戳 (毫秒)',
  `server_time` datetime NOT NULL COMMENT '服务器接收时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_ip` (`ip_address`),
  KEY `idx_created` (`created_at`),
  KEY `idx_server_time` (`server_time`)
) ENGINE=InnoDB AUTO_INCREMENT=428 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='JavaScript心跳记录表';

-- licenses
CREATE TABLE IF NOT EXISTS `licenses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `edition` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `expiry_date` date NOT NULL,
  `is_current` bit(1) NOT NULL,
  `issued_date` date NOT NULL,
  `issued_to` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `license_code` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `serial_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKfb066si8t1pex8g0yh13lqtvh` (`license_code`),
  UNIQUE KEY `UKseeju6lve8nb327fkdpluhl7c` (`serial_number`),
  KEY `idx_serial_number` (`serial_number`),
  KEY `idx_license_code` (`license_code`),
  KEY `idx_status` (`status`),
  KEY `idx_expiry_date` (`expiry_date`),
  KEY `idx_is_current` (`is_current`),
  KEY `idx_created_at` (`created_at` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- scenario_templates
CREATE TABLE IF NOT EXISTS `scenario_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `deployment_order` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `difficulty_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `estimated_duration` int DEFAULT NULL,
  `evaluation_criteria` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `exercise_script` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `instructor_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `is_active` bit(1) NOT NULL,
  `is_public` bit(1) NOT NULL,
  `last_used_at` datetime(6) DEFAULT NULL,
  `learning_objectives` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `max_participants` int DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `network_topology` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `prerequisites` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `scenario_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_guidelines` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `success_metrics` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `updated_at` datetime(6) DEFAULT NULL,
  `usage_count` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_scenario_templates_scenario_type` (`scenario_type`),
  KEY `idx_scenario_templates_is_active` (`is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- screen_recordings
CREATE TABLE IF NOT EXISTS `screen_recordings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chunk_count` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `project_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_achievement_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_time` datetime(6) NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_chunks` int DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK79shqu7paj9ou52hoce81wy9l` (`session_id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- system_backup
CREATE TABLE IF NOT EXISTS `system_backup` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `completed_time` datetime(6) DEFAULT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_time` datetime(6) NOT NULL,
  `database_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `encrypted` bit(1) NOT NULL,
  `encryption_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint NOT NULL,
  `md5checksum` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `table_count` int DEFAULT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- system_exceptions
CREATE TABLE IF NOT EXISTS `system_exceptions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `api_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `api_status` int DEFAULT NULL,
  `component_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `exception_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `occurred_at` datetime(6) DEFAULT NULL,
  `occurrence_count` int DEFAULT NULL,
  `request_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resolution` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `resolved_at` datetime(6) DEFAULT NULL,
  `resolved_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stack_trace` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `viewport_height` int DEFAULT NULL,
  `viewport_width` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  KEY `idx_occurred_at` (`occurred_at`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- system_log
CREATE TABLE IF NOT EXISTS `system_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operation` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `log_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `log_category` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `response_status` int DEFAULT NULL COMMENT 'HTTP响应状态码',
  `execution_time` bigint DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `exception_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '异常类型（完整类名）',
  `error_stack` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误堆栈信息',
  `business_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `before_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '操作前数据（JSON格式）',
  `after_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '操作后数据（JSON格式）',
  `tags` json DEFAULT NULL COMMENT '标签（JSON数组）如：["security", "critical", "录屏"]',
  `browser_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作类型：CREATE/READ/UPDATE/DELETE/EXECUTE/BATCH/EXPORT/IMPORT/AUDIT/APPROVE/REJECT/CONFIGURE/LOGIN/LOGOUT',
  `business_module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务模块：CONTAINER/USER/ACHIEVEMENT/DRILL/SCENARIO/PERMISSION/ROLE/TEAM/HOST_NODE/ASSET/SYSTEM_CONFIG/LOG/AUTH/MONITOR/BIG_SCREEN/FILE/RECORDING/TOPOLOGY/BACKUP/NOTIFICATION/OTHER',
  `object_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operation_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作状态：SUCCESS/FAILED/PARTIAL/PENDING/TIMEOUT/CANCELLED/SKIPPED',
  PRIMARY KEY (`id`),
  KEY `idx_log_level` (`log_level`),
  KEY `idx_log_category` (`log_category`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_timestamp` (`timestamp`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_timestamp_level` (`timestamp`,`log_level`),
  KEY `idx_timestamp_category` (`timestamp`,`log_category`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_business_module` (`business_module`),
  KEY `idx_object_type` (`object_type`),
  KEY `idx_operation_status` (`operation_status`),
  KEY `idx_module_type` (`business_module`,`operation_type`),
  KEY `idx_module_status` (`business_module`,`operation_status`),
  KEY `idx_type_status` (`operation_type`,`operation_status`),
  KEY `idx_module_type_status` (`business_module`,`operation_type`,`operation_status`),
  KEY `idx_module_timestamp` (`business_module`,`timestamp`),
  KEY `idx_status_timestamp` (`operation_status`,`timestamp`),
  KEY `idx_system_log_username` (`username`),
  KEY `idx_system_log_log_level` (`log_level`),
  KEY `idx_system_log_log_category` (`log_category`),
  KEY `idx_system_log_composite` (`username`,`timestamp` DESC,`log_level`),
  KEY `idx_system_log_timestamp_desc` (`timestamp` DESC),
  KEY `idx_system_log_level` (`log_level`),
  KEY `idx_system_log_category` (`log_category`),
  KEY `idx_system_log_operation_type` (`operation_type`),
  KEY `idx_system_log_business_module` (`business_module`),
  KEY `idx_system_log_operation_status` (`operation_status`),
  KEY `idx_system_log_timestamp_username` (`timestamp` DESC,`username`),
  KEY `idx_system_log_level_timestamp` (`log_level`,`timestamp` DESC),
  KEY `idx_system_log_trace_id` (`trace_id`),
  KEY `idx_system_log_session_id` (`session_id`),
  KEY `idx_system_log_business_id` (`business_id`)
) ENGINE=InnoDB AUTO_INCREMENT=33925 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- system_settings
CREATE TABLE IF NOT EXISTS `system_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `setting_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `setting_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `setting_value` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `updated_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnm18l4pyovtvd8y3b3x0l2y64` (`setting_key`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- team_applications
CREATE TABLE IF NOT EXISTS `team_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `team_id` bigint NOT NULL COMMENT '申请加入的战队ID',
  `user_id` bigint NOT NULL COMMENT '申请用户ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '申请状态: pending, approved, rejected',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申请留言',
  `reject_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '拒绝理由',
  `reviewed_by` bigint DEFAULT NULL COMMENT '审批人用户ID',
  `reviewed_at` timestamp NULL DEFAULT NULL COMMENT '审批时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_applications_pending` (`team_id`,`user_id`,`status`),
  KEY `fk_applications_reviewer` (`reviewed_by`),
  KEY `idx_applications_team` (`team_id`),
  KEY `idx_applications_user` (`user_id`),
  KEY `idx_applications_status` (`status`),
  KEY `idx_applications_created` (`created_at`),
  CONSTRAINT `fk_applications_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_applications_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_applications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='战队申请表';

-- team_members
CREATE TABLE IF NOT EXISTS `team_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `team_id` bigint NOT NULL COMMENT '战队ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `member_role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'member' COMMENT '成员角色: leader, member',
  `joined_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_members_user` (`user_id`),
  KEY `idx_team_members_team` (`team_id`),
  KEY `idx_team_members_user` (`user_id`),
  CONSTRAINT `fk_team_members_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_team_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='战队成员表';

-- team_scores
CREATE TABLE IF NOT EXISTS `team_scores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approved_count` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `last_update_time` datetime(6) DEFAULT NULL,
  `range_id` bigint NOT NULL,
  `ranking` int DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `team_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `team_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_score` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2cseydj4baxfjf2rs2muy763e` (`team_name`,`range_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- teams
CREATE TABLE IF NOT EXISTS `teams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '战队名称',
  `leader_id` bigint NOT NULL COMMENT '队长用户ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '战队描述',
  `team_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '战队类型: red, blue',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '战队状态: active, disbanded',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_teams_name` (`name`),
  KEY `idx_teams_type` (`team_type`),
  KEY `idx_teams_status` (`status`),
  KEY `idx_teams_leader` (`leader_id`),
  CONSTRAINT `fk_teams_leader` FOREIGN KEY (`leader_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='战队管理表';

-- topology_data
CREATE TABLE IF NOT EXISTS `topology_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `custom_elements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `links` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `nodes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `project_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7dnklgpf5c6c5t13xqy9eaq7y` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- user_ban_record
CREATE TABLE IF NOT EXISTS `user_ban_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `ban_level` int NOT NULL COMMENT '封禁等级: 1=警告, 2=强制跳转, 3=冻结24h, 4=永久封禁',
  `ban_type` varchar(50) NOT NULL COMMENT '封禁类型: WARNING, REDIRECT, FREEZE_24H, PERMANENT',
  `ban_reason` text COMMENT '封禁原因',
  `anomaly_record_id` bigint DEFAULT NULL COMMENT '关联的异常记录ID',
  `ban_start_time` datetime NOT NULL COMMENT '封禁开始时间',
  `ban_end_time` datetime DEFAULT NULL COMMENT '临时封禁的解封时间 (永久封禁为NULL)',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否当前生效',
  `unban_by` varchar(50) DEFAULT NULL COMMENT '解封操作的管理员',
  `unban_time` datetime DEFAULT NULL COMMENT '解封时间',
  `unban_reason` text COMMENT '解封原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_ban_level` (`ban_level`),
  KEY `idx_active` (`is_active`),
  KEY `idx_ban_type` (`ban_type`),
  KEY `idx_ban_start_time` (`ban_start_time`),
  KEY `anomaly_record_id` (`anomaly_record_id`),
  CONSTRAINT `user_ban_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_ban_record_ibfk_2` FOREIGN KEY (`anomaly_record_id`) REFERENCES `js_anomaly_record` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户封禁记录表';

-- users
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` tinyint(1) DEFAULT '1',
  `team_id` bigint DEFAULT NULL COMMENT '所属战队ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_users_team` (`team_id`),
  KEY `idx_users_role` (`role`),
  KEY `idx_users_team_id` (`team_id`),
  KEY `idx_users_enabled` (`enabled`),
  CONSTRAINT `fk_users_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 系统设置初始化数据
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, updated_at)
VALUES
    ('system_logo', '/images/default-logo.png', 'image', '系统Logo图片', NOW()),
    ('login_title', '欢迎使用CyberLab网络空间安全攻防演练平台', 'text', '登录页面标题', NOW()),
    ('sidebar_title', 'CyberLab平台', 'text', '侧边栏标题', NOW()),
    ('serial_number', 'CYBERLAB-2024-001', 'code', '产品序列号', NOW()),
    ('license_code', '未设置授权码', 'code', '产品授权码', NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

SET FOREIGN_KEY_CHECKS = 1;
