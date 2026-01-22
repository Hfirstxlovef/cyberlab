package org.cyberlab.config;

import org.cyberlab.entity.User;
import org.cyberlab.entity.Asset;
import org.cyberlab.entity.ScenarioTemplate;
import org.cyberlab.entity.DrillAsset;
import org.cyberlab.entity.CyberRange;
import org.cyberlab.repository.UserRepository;
import org.cyberlab.repository.AssetRepository;
import org.cyberlab.repository.ScenarioTemplateRepository;
import org.cyberlab.repository.DrillAssetRepository;
import org.cyberlab.repository.CyberRangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private ScenarioTemplateRepository scenarioTemplateRepository;

    @Autowired
    private DrillAssetRepository drillAssetRepository;

    @Autowired
    private CyberRangeRepository cyberRangeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeUsers();
        initializeAssets();
        initializeScenarioTemplates();
        initializeDrillAssets();
        initializeCyberRanges();
    }

    private void initializeUsers() {
        try {
            // 创建管理员用户
            if (!userRepository.findByUsername("admin").isPresent()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("admin");
                admin.setEnabled(true);
                userRepository.save(admin);
                logger.debug("Created admin user: admin");
            }

            // 创建红队用户
            if (!userRepository.findByUsername("redteam").isPresent()) {
                User redUser = new User();
                redUser.setUsername("redteam");
                redUser.setPassword(passwordEncoder.encode("red123"));
                redUser.setRole("red");
                redUser.setEnabled(true);
                userRepository.save(redUser);
                logger.debug("Created red team user: redteam");
            }

            // 创建蓝队用户
            if (!userRepository.findByUsername("blueteam").isPresent()) {
                User blueUser = new User();
                blueUser.setUsername("blueteam");
                blueUser.setPassword(passwordEncoder.encode("blue123"));
                blueUser.setRole("blue");
                blueUser.setEnabled(true);
                userRepository.save(blueUser);
                logger.debug("Created blue team user: blueteam");
            }

            // 创建裁判用户
            if (!userRepository.findByUsername("judge").isPresent()) {
                User judgeUser = new User();
                judgeUser.setUsername("judge");
                judgeUser.setPassword(passwordEncoder.encode("judge123"));
                judgeUser.setRole("judge");
                judgeUser.setEnabled(true);
                userRepository.save(judgeUser);
                logger.debug("Created judge user: judge");
            }

            // 创建caipan管理员用户
            if (!userRepository.findByUsername("caipan").isPresent()) {
                User caipanUser = new User();
                caipanUser.setUsername("caipan");
                caipanUser.setPassword(passwordEncoder.encode("caipan123"));
                caipanUser.setRole("admin");
                caipanUser.setEnabled(true);
                userRepository.save(caipanUser);
                logger.debug("Created caipan admin user: caipan");
            }

        } catch (Exception e) {
            logger.error("用户数据初始化失败: " + e.getMessage());
        }
    }

    private void initializeAssets() {
        try {
            // 检查是否已有测试资产
            if (assetRepository.count() == 0) {
                // 创建测试资产
                Asset asset1 = new Asset();
                asset1.setName("Web服务器");
                asset1.setIp("172.16.190.130");
                asset1.setCompany("红岸实验室");
                asset1.setOwner("admin");
                asset1.setVisibility("both");
                asset1.setTarget(true);
                asset1.setEnabled(true);
                asset1.setNotes("主要Web服务器");
                asset1.setProject("网络安全演练项目");
                asset1.setTopologyProjectId("demo-topology-001");
                assetRepository.save(asset1);

                Asset asset2 = new Asset();
                asset2.setName("数据库服务器");
                asset2.setIp("192.168.1.101");
                asset2.setCompany("红岸实验室");
                asset2.setOwner("admin");
                asset2.setVisibility("blue");
                asset2.setTarget(false);
                asset2.setEnabled(true);
                asset2.setNotes("MySQL数据库服务器");
                asset2.setProject("网络安全演练项目");
                asset2.setTopologyProjectId("demo-topology-001");
                assetRepository.save(asset2);

                Asset asset3 = new Asset();
                asset3.setName("防火墙设备");
                asset3.setIp("192.168.1.1");
                asset3.setCompany("红岸实验室");
                asset3.setOwner("admin");
                asset3.setVisibility("both");
                asset3.setTarget(false);
                asset3.setEnabled(true);
                asset3.setNotes("边界防护设备");
                asset3.setProject("企业安全防护项目");
                asset3.setTopologyProjectId("demo-topology-002");
                assetRepository.save(asset3);

                logger.debug("Created test asset data");
            }
        } catch (Exception e) {
            logger.error("资产数据初始化失败: " + e.getMessage());
        }
    }

    private void initializeScenarioTemplates() {
        try {
            // 检查是否已有场景模板
            if (scenarioTemplateRepository.count() == 0) {
                // 创建红队攻击场景模板
                ScenarioTemplate redTemplate = new ScenarioTemplate();
                redTemplate.setName("Web应用渗透测试");
                redTemplate.setDescription("针对Web应用的综合渗透测试场景，包括SQL注入、XSS、文件上传等常见漏洞利用");
                redTemplate.setScenarioType("red_team");
                redTemplate.setDifficultyLevel("intermediate");
                redTemplate.setAssetConfig("{\n" +
                    "  \"assets\": [\n" +
                    "    {\"type\": \"web_server\", \"name\": \"目标Web服务器\", \"vulnerabilities\": [\"sql_injection\", \"xss\", \"file_upload\"]}\n" +
                    "  ],\n" +
                    "  \"objectives\": [\n" +
                    "    \"获取管理员权限\",\n" +
                    "    \"提取敏感数据\",\n" +
                    "    \"建立持久化后门\"\n" +
                    "  ],\n" +
                    "  \"tools\": [\"sqlmap\", \"burpsuite\", \"metasploit\"],\n" +
                    "  \"timeLimit\": 120\n" +
                    "}");
                redTemplate.setIsActive(true);
                redTemplate.setCreatedBy(1L); // admin用户的ID
                scenarioTemplateRepository.save(redTemplate);

                // 创建蓝队防御场景模板
                ScenarioTemplate blueTemplate = new ScenarioTemplate();
                blueTemplate.setName("APT攻击检测与响应");
                blueTemplate.setDescription("模拟高级持续性威胁(APT)攻击，训练蓝队的检测、分析和响应能力");
                blueTemplate.setScenarioType("blue_team");
                blueTemplate.setDifficultyLevel("advanced");
                blueTemplate.setAssetConfig("{\n" +
                    "  \"assets\": [\n" +
                    "    {\"type\": \"domain_controller\", \"name\": \"域控制器\"},\n" +
                    "    {\"type\": \"file_server\", \"name\": \"文件服务器\"},\n" +
                    "    {\"type\": \"workstation\", \"name\": \"办公终端\"}\n" +
                    "  ],\n" +
                    "  \"attack_scenarios\": [\n" +
                    "    \"钓鱼邮件投递\",\n" +
                    "    \"权限提升\",\n" +
                    "    \"横向移动\",\n" +
                    "    \"数据窃取\"\n" +
                    "  ],\n" +
                    "  \"defense_tools\": [\"SIEM\", \"EDR\", \"IDS/IPS\", \"威胁情报平台\"],\n" +
                    "  \"success_criteria\": [\"检测到初始入侵\", \"阻止权限提升\", \"隔离受感染主机\"]\n" +
                    "}");
                blueTemplate.setIsActive(true);
                blueTemplate.setCreatedBy(1L); // admin用户的ID
                scenarioTemplateRepository.save(blueTemplate);

                // 创建混合演练场景模板
                ScenarioTemplate mixedTemplate = new ScenarioTemplate();
                mixedTemplate.setName("企业网络综合攻防演练");
                mixedTemplate.setDescription("大型企业网络环境下的红蓝对抗演练，涵盖多个攻击向量和防御策略");
                mixedTemplate.setScenarioType("mixed");
                mixedTemplate.setDifficultyLevel("advanced");
                mixedTemplate.setAssetConfig("{\n" +
                    "  \"network_topology\": {\n" +
                    "    \"dmz\": [\"web_server\", \"mail_server\", \"dns_server\"],\n" +
                    "    \"internal\": [\"domain_controller\", \"database_server\", \"file_server\"],\n" +
                    "    \"workstations\": [\"hr_pc\", \"finance_pc\", \"admin_pc\"]\n" +
                    "  },\n" +
                    "  \"red_team_objectives\": [\n" +
                    "    \"获取域管理权限\",\n" +
                    "    \"窃取财务数据\",\n" +
                    "    \"破坏业务连续性\"\n" +
                    "  ],\n" +
                    "  \"blue_team_objectives\": [\n" +
                    "    \"检测并阻止攻击\",\n" +
                    "    \"保护核心资产\",\n" +
                    "    \"快速事件响应\"\n" +
                    "  ],\n" +
                    "  \"duration\": 480\n" +
                    "}");
                mixedTemplate.setIsActive(true);
                mixedTemplate.setCreatedBy(1L); // admin用户的ID
                scenarioTemplateRepository.save(mixedTemplate);

                logger.debug("Created scenario template data");
            }
        } catch (Exception e) {
            logger.error("场景模板数据初始化失败: " + e.getMessage());
        }
    }

    private void initializeDrillAssets() {
        try {
            // 检查是否已有演练资产
            if (drillAssetRepository.count() == 0) {
                // 获取已创建的资产
                Asset webServer = assetRepository.findByName("Web服务器").orElse(null);
                Asset dbServer = assetRepository.findByName("数据库服务器").orElse(null);
                Asset firewall = assetRepository.findByName("防火墙设备").orElse(null);

                if (webServer != null) {
                    DrillAsset drillAsset1 = new DrillAsset();
                    drillAsset1.setName(webServer.getName());
                    drillAsset1.setCategory("web_server");
                    drillAsset1.setDescription("Web服务器 - IP: " + webServer.getIp());
                    drillAsset1.setDockerImage("nginx:latest");
                    drillAsset1.setTeamVisibility("both");
                    drillAsset1.setVulnerabilityType("sql_injection");
                    drillAsset1.setDifficultyLevel("intermediate");
                    drillAsset1.setAttackVector("network");
                    drillAsset1.setSetupInstructions("WAF,输入验证,文件类型检查");
                    drillAsset1.setExerciseInstructions("Apache日志,ModSecurity");
                    drillAsset1.setCreatedBy(1L);
                    drillAsset1.setIsTarget(true);
                    drillAsset1.setIsActive(true);
                    drillAssetRepository.save(drillAsset1);
                }

                if (dbServer != null) {
                    DrillAsset drillAsset2 = new DrillAsset();
                    drillAsset2.setName(dbServer.getName());
                    drillAsset2.setCategory("database");
                    drillAsset2.setDescription("数据库服务器 - IP: " + dbServer.getIp());
                    drillAsset2.setDockerImage("mysql:8.0");
                    drillAsset2.setTeamVisibility("both");
                    drillAsset2.setVulnerabilityType("sql_injection");
                    drillAsset2.setDifficultyLevel("advanced");
                    drillAsset2.setAttackVector("network");
                    drillAsset2.setSetupInstructions("强密码策略,最小权限原则,参数化查询");
                    drillAsset2.setExerciseInstructions("MySQL审计日志,数据库活动监控");
                    drillAsset2.setCreatedBy(1L);
                    drillAsset2.setIsTarget(false);
                    drillAsset2.setIsActive(true);
                    drillAssetRepository.save(drillAsset2);
                }

                if (firewall != null) {
                    DrillAsset drillAsset3 = new DrillAsset();
                    drillAsset3.setName(firewall.getName());
                    drillAsset3.setCategory("network_device");
                    drillAsset3.setDescription("防火墙设备 - IP: " + firewall.getIp());
                    drillAsset3.setDockerImage("pfsense/pfsense:latest");
                    drillAsset3.setTeamVisibility("blue");
                    drillAsset3.setVulnerabilityType("privilege_escalation");
                    drillAsset3.setDifficultyLevel("advanced");
                    drillAsset3.setAttackVector("network");
                    drillAsset3.setSetupInstructions("安全配置基线,规则定期审查,强认证");
                    drillAsset3.setExerciseInstructions("防火墙日志,流量分析,SIEM");
                    drillAsset3.setCreatedBy(1L);
                    drillAsset3.setIsTarget(false);
                    drillAsset3.setIsActive(true);
                    drillAssetRepository.save(drillAsset3);
                }

                // 创建一些虚拟演练资产
                DrillAsset virtualAsset1 = new DrillAsset();
                virtualAsset1.setName("模拟邮件服务器");
                virtualAsset1.setCategory("server");
                virtualAsset1.setDescription("模拟邮件服务器 - IP: 192.168.1.102");
                virtualAsset1.setDockerImage("mailhog/mailhog:latest");
                virtualAsset1.setTeamVisibility("both");
                virtualAsset1.setVulnerabilityType("command_injection");
                virtualAsset1.setDifficultyLevel("beginner");
                virtualAsset1.setAttackVector("network");
                virtualAsset1.setSetupInstructions("邮件过滤,SPF/DKIM/DMARC,多因子认证");
                virtualAsset1.setExerciseInstructions("邮件安全网关,日志监控");
                virtualAsset1.setCreatedBy(1L);
                virtualAsset1.setIsTarget(true);
                virtualAsset1.setIsActive(true);
                drillAssetRepository.save(virtualAsset1);

                DrillAsset virtualAsset2 = new DrillAsset();
                virtualAsset2.setName("模拟域控制器");
                virtualAsset2.setCategory("server");
                virtualAsset2.setDescription("模拟域控制器 - IP: 192.168.1.10");
                virtualAsset2.setDockerImage("samba:latest");
                virtualAsset2.setTeamVisibility("blue");
                virtualAsset2.setVulnerabilityType("privilege_escalation");
                virtualAsset2.setDifficultyLevel("advanced");
                virtualAsset2.setAttackVector("local");
                virtualAsset2.setSetupInstructions("特权账户管理,Kerberos加固,审计策略");
                virtualAsset2.setExerciseInstructions("Windows Event Log,Sysmon,PowerShell日志");
                virtualAsset2.setCreatedBy(1L);
                virtualAsset2.setIsTarget(false);
                virtualAsset2.setIsActive(true);
                drillAssetRepository.save(virtualAsset2);

                logger.debug("Created drill asset data");
            }
        } catch (Exception e) {
            logger.error("演练资产数据初始化失败: " + e.getMessage());
        }
    }

    private void initializeCyberRanges() {
        try {
            // 检查是否已有演练项目
            if (cyberRangeRepository.count() == 0) {
                logger.info("Initializing CyberRange test data...");

                // 创建演练项目1：Web应用渗透演练
                CyberRange range1 = new CyberRange();
                range1.setName("Web应用渗透演练");
                range1.setDescription("针对Web应用的安全渗透测试演练，包括SQL注入、XSS、文件上传等漏洞利用");
                range1.setStatus("running");
                range1.setDrillType("attack_defense");
                range1.setDifficultyLevel("intermediate");
                range1.setStartTime(LocalDateTime.now().minusDays(2));
                range1.setEndTime(LocalDateTime.now().plusDays(5));
                range1.setMaxParticipants(20);
                range1.setCreatorId(1L);
                range1.setTopologyProjectId("demo-topology-001");
                range1.setTopologyConfig("{\"nodes\":[{\"type\":\"web_server\",\"ip\":\"172.16.190.130\"}]}");
                range1.setVulnerabilityConfig("{\"vulnerabilities\":[\"sql_injection\",\"xss\",\"file_upload\"]}");
                range1.setCreatedAt(LocalDateTime.now().minusDays(2));
                range1.setUpdatedAt(LocalDateTime.now());
                cyberRangeRepository.save(range1);

                // 创建演练项目2：APT攻击检测与响应
                CyberRange range2 = new CyberRange();
                range2.setName("APT攻击检测与响应");
                range2.setDescription("模拟高级持续性威胁(APT)攻击场景，训练蓝队的检测、分析和应急响应能力");
                range2.setStatus("running");
                range2.setDrillType("blue_team");
                range2.setDifficultyLevel("advanced");
                range2.setStartTime(LocalDateTime.now().minusDays(1));
                range2.setEndTime(LocalDateTime.now().plusDays(6));
                range2.setMaxParticipants(15);
                range2.setCreatorId(1L);
                range2.setTopologyProjectId("demo-topology-002");
                range2.setTopologyConfig("{\"nodes\":[{\"type\":\"domain_controller\"},{\"type\":\"file_server\"}]}");
                range2.setVulnerabilityConfig("{\"attack_scenarios\":[\"phishing\",\"privilege_escalation\",\"lateral_movement\"]}");
                range2.setCreatedAt(LocalDateTime.now().minusDays(1));
                range2.setUpdatedAt(LocalDateTime.now());
                cyberRangeRepository.save(range2);

                // 创建演练项目3：企业网络红蓝对抗
                CyberRange range3 = new CyberRange();
                range3.setName("企业网络红蓝对抗");
                range3.setDescription("大型企业网络环境下的红蓝队综合对抗演练，覆盖多个攻击向量和防御策略");
                range3.setStatus("running");
                range3.setDrillType("mixed");
                range3.setDifficultyLevel("advanced");
                range3.setStartTime(LocalDateTime.now());
                range3.setEndTime(LocalDateTime.now().plusDays(7));
                range3.setMaxParticipants(30);
                range3.setCreatorId(1L);
                range3.setTopologyProjectId("demo-topology-003");
                range3.setTopologyConfig("{\"dmz\":[\"web_server\",\"mail_server\"],\"internal\":[\"dc\",\"db\"]}");
                range3.setVulnerabilityConfig("{\"red_objectives\":[\"domain_admin\",\"data_theft\"],\"blue_objectives\":[\"detect_attack\",\"incident_response\"]}");
                range3.setCreatedAt(LocalDateTime.now());
                range3.setUpdatedAt(LocalDateTime.now());
                cyberRangeRepository.save(range3);

                // 创建演练项目4：工业控制系统安全演练
                CyberRange range4 = new CyberRange();
                range4.setName("工业控制系统安全演练");
                range4.setDescription("针对工控系统(ICS/SCADA)的安全攻防演练，模拟关键基础设施防护场景");
                range4.setStatus("running");
                range4.setDrillType("attack_defense");
                range4.setDifficultyLevel("advanced");
                range4.setStartTime(LocalDateTime.now().minusHours(6));
                range4.setEndTime(LocalDateTime.now().plusDays(4));
                range4.setMaxParticipants(12);
                range4.setCreatorId(1L);
                range4.setTopologyProjectId("demo-topology-004");
                range4.setTopologyConfig("{\"nodes\":[{\"type\":\"scada_server\"},{\"type\":\"plc\"},{\"type\":\"hmi\"}]}");
                range4.setVulnerabilityConfig("{\"vulnerabilities\":[\"modbus_attack\",\"firmware_manipulation\",\"dos\"]}");
                range4.setCreatedAt(LocalDateTime.now().minusHours(6));
                range4.setUpdatedAt(LocalDateTime.now());
                cyberRangeRepository.save(range4);

                // 创建演练项目5：云环境安全渗透（暂停状态）
                CyberRange range5 = new CyberRange();
                range5.setName("云环境安全渗透");
                range5.setDescription("云计算环境下的安全渗透测试，包括容器逃逸、云API滥用等场景");
                range5.setStatus("paused");
                range5.setDrillType("red_team");
                range5.setDifficultyLevel("intermediate");
                range5.setStartTime(LocalDateTime.now().minusDays(3));
                range5.setEndTime(LocalDateTime.now().plusDays(3));
                range5.setMaxParticipants(18);
                range5.setCreatorId(1L);
                range5.setTopologyProjectId("demo-topology-005");
                range5.setTopologyConfig("{\"nodes\":[{\"type\":\"k8s_cluster\"},{\"type\":\"docker_host\"}]}");
                range5.setVulnerabilityConfig("{\"vulnerabilities\":[\"container_escape\",\"api_abuse\",\"misconfig\"]}");
                range5.setCreatedAt(LocalDateTime.now().minusDays(3));
                range5.setUpdatedAt(LocalDateTime.now());
                cyberRangeRepository.save(range5);

                logger.info("Created {} CyberRange test records (4 running, 1 paused)", 5);
            } else {
                logger.info("CyberRange data already exists, count: {}", cyberRangeRepository.count());
            }
        } catch (Exception e) {
            logger.error("CyberRange数据初始化失败: " + e.getMessage(), e);
        }
    }
}