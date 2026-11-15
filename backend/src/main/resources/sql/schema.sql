-- ============================================================
-- 通用自动化测试平台 V2.1 数据库建表脚本（修订版）
-- MySQL 8.0 / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci
-- 数据库：autotest_platform
-- 修订内容（对照 V2.0）：
--   1) 新增 sys_role_permission
--   2) 统一角色模型：project_member 去 role_code；sys_user_role 承载项目授权
--   3) 可空唯一索引用生成列修复（project_scope / env_scope）
--   4) 套件版本化（test_suite_version + current_version）
--   5) 用例当前版本 + 资产关联
--   6) 执行器授权/能力/配额/凭据（runner_project + runner_agent 扩展）
--   7) 多态引用补 target_version；步骤补 sort/iteration/attempt
--   8) 平台治理表：system_parameter / platform_notification / common_* / variable_dict
--   9) 时间 DATETIME(3)，约定服务端 UTC，展示按项目时区
-- 生产部署前请通过 Flyway/Liquibase 管理迁移。
-- ============================================================

CREATE DATABASE IF NOT EXISTS autotest_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE autotest_platform;

-- ------------------------------------------------------------
-- 一、用户 / 角色 / 权限
-- ------------------------------------------------------------
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  email VARCHAR(160),
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(48) NOT NULL UNIQUE,
  role_name VARCHAR(80) NOT NULL,
  scope_type VARCHAR(16) NOT NULL DEFAULT 'PLATFORM' COMMENT 'PLATFORM/PROJECT',
  description VARCHAR(255),
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;

CREATE TABLE sys_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  permission_code VARCHAR(100) NOT NULL UNIQUE,
  permission_name VARCHAR(100) NOT NULL,
  resource_type VARCHAR(48) NOT NULL
) ENGINE=InnoDB;

-- 【新增】角色-权限关联（修复 B1-1）
CREATE TABLE sys_role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  UNIQUE KEY uk_role_perm(role_id, permission_id),
  KEY idx_perm(permission_id),
  FOREIGN KEY(role_id) REFERENCES sys_role(id),
  FOREIGN KEY(permission_id) REFERENCES sys_permission(id)
) ENGINE=InnoDB;

-- 用户-角色-项目授权；project_id 可空=平台级角色；生成列固化 NULL 修复唯一索引（修复 B1-3）
CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  project_id BIGINT NULL,
  project_scope BIGINT GENERATED ALWAYS AS (IFNULL(project_id,0)) STORED,
  UNIQUE KEY uk_user_role_scope(user_id, role_id, project_scope),
  KEY idx_role(role_id),
  FOREIGN KEY(user_id) REFERENCES sys_user(id),
  FOREIGN KEY(role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 二、项目
-- ------------------------------------------------------------
CREATE TABLE project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_code VARCHAR(64) NOT NULL UNIQUE,
  project_name VARCHAR(128) NOT NULL,
  project_type VARCHAR(24) NOT NULL DEFAULT 'MIXED',
  description VARCHAR(500),
  owner_user_id BIGINT,
  timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用 2归档',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_owner(owner_user_id)
) ENGINE=InnoDB;

-- 项目成员（仅成员关系；角色授权走 sys_user_role，删除 role_code 修复 B1-2）
CREATE TABLE project_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_project_member(project_id, user_id),
  KEY idx_member_user(user_id),
  FOREIGN KEY(project_id) REFERENCES project(id),
  FOREIGN KEY(user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB;

CREATE TABLE project_module (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  parent_id BIGINT NULL,
  module_code VARCHAR(64) NOT NULL,
  module_name VARCHAR(128) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_module(project_id, module_code),
  FOREIGN KEY(project_id) REFERENCES project(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 三、环境与变量
-- ------------------------------------------------------------
CREATE TABLE test_environment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  env_code VARCHAR(48) NOT NULL,
  env_name VARCHAR(100) NOT NULL,
  env_type VARCHAR(24) NOT NULL DEFAULT 'TEST',
  env_level VARCHAR(16) NOT NULL DEFAULT 'TEST' COMMENT 'TEST/DEV/PRED/PROD（生产执行保护）',
  base_url VARCHAR(500) NOT NULL,
  description VARCHAR(500),
  is_default TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_env(project_id, env_code),
  FOREIGN KEY(project_id) REFERENCES project(id)
) ENGINE=InnoDB;

-- 环境变量；env_scope 生成列修复 NULL 唯一索引（修复 B1-3）
CREATE TABLE environment_variable (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  environment_id BIGINT NULL,
  env_scope BIGINT GENERATED ALWAYS AS (IFNULL(environment_id,0)) STORED,
  var_key VARCHAR(128) NOT NULL,
  value_ciphertext TEXT,
  value_type VARCHAR(24) NOT NULL DEFAULT 'STRING',
  is_secret TINYINT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_var(project_id, env_scope, var_key),
  FOREIGN KEY(project_id) REFERENCES project(id),
  FOREIGN KEY(environment_id) REFERENCES test_environment(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 四、API 资产
-- ------------------------------------------------------------
CREATE TABLE api_service (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  service_name VARCHAR(128) NOT NULL,
  description VARCHAR(500),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_api_service_project(project_id),
  FOREIGN KEY(project_id) REFERENCES project(id)
) ENGINE=InnoDB;

CREATE TABLE api_definition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  module_id BIGINT NULL,
  api_name VARCHAR(128) NOT NULL,
  http_method VARCHAR(12) NOT NULL,
  path VARCHAR(500) NOT NULL,
  auth_type VARCHAR(32) NOT NULL DEFAULT 'INHERIT',
  timeout_ms INT NOT NULL DEFAULT 10000,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_api_lookup(project_id, http_method, path(191)),
  FOREIGN KEY(project_id) REFERENCES project(id),
  FOREIGN KEY(service_id) REFERENCES api_service(id),
  FOREIGN KEY(module_id) REFERENCES project_module(id)
) ENGINE=InnoDB;

CREATE TABLE api_definition_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  api_definition_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  request_schema JSON,
  response_schema JSON,
  headers_json JSON,
  query_params_json JSON,
  path_params_json JSON,
  body_template JSON,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_api_version(api_definition_id, version_no),
  FOREIGN KEY(api_definition_id) REFERENCES api_definition(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 五、UI 资产
-- ------------------------------------------------------------
CREATE TABLE ui_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  app_name VARCHAR(128) NOT NULL,
  base_url VARCHAR(500) NOT NULL,
  browser_type VARCHAR(24) NOT NULL DEFAULT 'CHROMIUM',
  viewport_json JSON,
  default_timeout_ms INT NOT NULL DEFAULT 10000,
  status TINYINT NOT NULL DEFAULT 1,
  FOREIGN KEY(project_id) REFERENCES project(id)
) ENGINE=InnoDB;

CREATE TABLE ui_page_object (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  application_id BIGINT NOT NULL,
  page_name VARCHAR(128) NOT NULL,
  url_pattern VARCHAR(500),
  description VARCHAR(500),
  FOREIGN KEY(project_id) REFERENCES project(id),
  FOREIGN KEY(application_id) REFERENCES ui_application(id)
) ENGINE=InnoDB;

CREATE TABLE ui_locator (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  page_object_id BIGINT NOT NULL,
  locator_name VARCHAR(128) NOT NULL,
  strategy VARCHAR(32) NOT NULL COMMENT 'DATA_TESTID/ROLE/LABEL/NAME/ID/CSS/XPATH',
  locator_value VARCHAR(1000) NOT NULL,
  description VARCHAR(255),
  UNIQUE KEY uk_locator(page_object_id, locator_name),
  FOREIGN KEY(page_object_id) REFERENCES ui_page_object(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 六、用例 / 数据集
-- ------------------------------------------------------------
CREATE TABLE test_case (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  module_id BIGINT NULL,
  case_code VARCHAR(64) NOT NULL,
  case_name VARCHAR(160) NOT NULL,
  case_type VARCHAR(16) NOT NULL COMMENT 'API/UI',
  priority VARCHAR(8) NOT NULL DEFAULT 'P1',
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  tags_json JSON,
  -- 修复 B1-5：当前版本 + 资产关联（影响分析）
  current_version INT NOT NULL DEFAULT 1,
  api_definition_id BIGINT NULL COMMENT 'API 用例关联接口',
  ui_page_object_id BIGINT NULL COMMENT 'UI 用例关联页面对象',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_case_code(project_id, case_code),
  KEY idx_case_project(project_id, status),
  FOREIGN KEY(project_id) REFERENCES project(id),
  FOREIGN KEY(module_id) REFERENCES project_module(id),
  FOREIGN KEY(api_definition_id) REFERENCES api_definition(id),
  FOREIGN KEY(ui_page_object_id) REFERENCES ui_page_object(id)
) ENGINE=InnoDB;

CREATE TABLE test_case_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  test_case_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  definition_json JSON NOT NULL,
  published TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_case_version(test_case_id, version_no),
  FOREIGN KEY(test_case_id) REFERENCES test_case(id)
) ENGINE=InnoDB;

CREATE TABLE test_dataset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  dataset_name VARCHAR(128) NOT NULL,
  data_type VARCHAR(16) NOT NULL DEFAULT 'JSON',
  schema_json JSON,
  status TINYINT NOT NULL DEFAULT 1,
  FOREIGN KEY(project_id) REFERENCES project(id)
) ENGINE=InnoDB;

CREATE TABLE test_dataset_row (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dataset_id BIGINT NOT NULL,
  row_no INT NOT NULL,
  data_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_dataset_row(dataset_id, row_no),
  FOREIGN KEY(dataset_id) REFERENCES test_dataset(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 七、场景
-- ------------------------------------------------------------
CREATE TABLE test_scene (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  module_id BIGINT NULL,
  scene_code VARCHAR(64) NOT NULL,
  scene_name VARCHAR(160) NOT NULL,
  description VARCHAR(500),
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  current_version INT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_scene_code(project_id, scene_code),
  FOREIGN KEY(project_id) REFERENCES project(id),
  FOREIGN KEY(module_id) REFERENCES project_module(id)
) ENGINE=InnoDB;

CREATE TABLE test_scene_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  graph_json JSON NOT NULL,
  published TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_scene_version(scene_id, version_no),
  FOREIGN KEY(scene_id) REFERENCES test_scene(id)
) ENGINE=InnoDB;

CREATE TABLE scene_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_version_id BIGINT NOT NULL,
  node_key VARCHAR(64) NOT NULL,
  node_type VARCHAR(32) NOT NULL,
  node_name VARCHAR(128) NOT NULL,
  position_x INT NOT NULL DEFAULT 0,
  position_y INT NOT NULL DEFAULT 0,
  config_json JSON,
  UNIQUE KEY uk_scene_node(scene_version_id, node_key),
  FOREIGN KEY(scene_version_id) REFERENCES test_scene_version(id)
) ENGINE=InnoDB;

CREATE TABLE scene_edge (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_version_id BIGINT NOT NULL,
  source_node_key VARCHAR(64) NOT NULL,
  target_node_key VARCHAR(64) NOT NULL,
  edge_type VARCHAR(24) NOT NULL DEFAULT 'NORMAL',
  condition_json JSON,
  KEY idx_edge_version(scene_version_id, source_node_key, target_node_key),
  FOREIGN KEY(scene_version_id) REFERENCES test_scene_version(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 八、套件（版本化，修复 B1-4）
-- ------------------------------------------------------------
CREATE TABLE test_suite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  suite_name VARCHAR(160) NOT NULL,
  execution_policy VARCHAR(24) NOT NULL DEFAULT 'SEQUENTIAL',
  status TINYINT NOT NULL DEFAULT 1,
  current_version INT NOT NULL DEFAULT 1,
  FOREIGN KEY(project_id) REFERENCES project(id)
) ENGINE=InnoDB;

-- 【新增】套件版本
CREATE TABLE test_suite_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  suite_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  definition_json JSON NOT NULL,
  published TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_suite_version(suite_id, version_no),
  FOREIGN KEY(suite_id) REFERENCES test_suite(id)
) ENGINE=InnoDB;

CREATE TABLE test_suite_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  suite_id BIGINT NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  target_id BIGINT NOT NULL,
  target_version INT NULL COMMENT '目标版本（可空=最新发布版）',
  sort_no INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  FOREIGN KEY(suite_id) REFERENCES test_suite(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 九、任务 / 执行器 / 执行
-- ------------------------------------------------------------
CREATE TABLE test_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  task_name VARCHAR(160) NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  target_id BIGINT NOT NULL,
  target_version INT NULL,
  environment_id BIGINT NOT NULL,
  params_json JSON,
  created_by BIGINT,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_task_project(project_id, created_at),
  FOREIGN KEY(project_id) REFERENCES project(id),
  FOREIGN KEY(environment_id) REFERENCES test_environment(id)
) ENGINE=InnoDB;

-- 执行器（扩展授权/能力/配额/凭据，修复 B1-6）
CREATE TABLE runner_agent (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  runner_code VARCHAR(64) NOT NULL UNIQUE,
  runner_name VARCHAR(128) NOT NULL,
  version VARCHAR(32),
  tags_json JSON,
  capabilities_json JSON COMMENT '["API_RUNNER","PLAYWRIGHT","APPIUM"]',
  max_concurrency INT NOT NULL DEFAULT 1,
  api_key_hash VARCHAR(255) COMMENT '凭据哈希，不存明文',
  status VARCHAR(16) NOT NULL DEFAULT 'OFFLINE',
  last_heartbeat_at DATETIME(3),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;

-- 【新增】执行器-项目授权
CREATE TABLE runner_project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  runner_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_runner_project(runner_id, project_id),
  FOREIGN KEY(runner_id) REFERENCES runner_agent(id),
  FOREIGN KEY(project_id) REFERENCES project(id)
) ENGINE=InnoDB;

CREATE TABLE test_execution (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  execution_no VARCHAR(64) NOT NULL UNIQUE,
  runner_id BIGINT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'QUEUED',
  version_snapshot_json JSON,
  started_at DATETIME(3) NULL,
  finished_at DATETIME(3) NULL,
  duration_ms BIGINT,
  summary_json JSON,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_exec_project_time(project_id, started_at),
  KEY idx_exec_status(status),
  FOREIGN KEY(task_id) REFERENCES test_task(id),
  FOREIGN KEY(project_id) REFERENCES project(id),
  FOREIGN KEY(runner_id) REFERENCES runner_agent(id)
) ENGINE=InnoDB;

CREATE TABLE step_execution (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  execution_id BIGINT NOT NULL,
  node_key VARCHAR(64),
  step_name VARCHAR(160) NOT NULL,
  step_type VARCHAR(32) NOT NULL,
  -- 修复 B1-8：排序/迭代/尝试
  sort_no INT NOT NULL DEFAULT 0,
  iteration_no INT NOT NULL DEFAULT 0,
  attempt_no INT NOT NULL DEFAULT 0,
  status VARCHAR(24) NOT NULL,
  started_at DATETIME(3) NULL,
  finished_at DATETIME(3) NULL,
  duration_ms BIGINT,
  input_snapshot JSON,
  output_snapshot JSON,
  error_message TEXT,
  KEY idx_step_execution(execution_id, status),
  FOREIGN KEY(execution_id) REFERENCES test_execution(id)
) ENGINE=InnoDB;

CREATE TABLE execution_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  execution_id BIGINT NOT NULL,
  step_execution_id BIGINT NULL,
  log_level VARCHAR(12) NOT NULL,
  message TEXT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_exec_log(execution_id, id),
  FOREIGN KEY(execution_id) REFERENCES test_execution(id),
  FOREIGN KEY(step_execution_id) REFERENCES step_execution(id)
) ENGINE=InnoDB;

CREATE TABLE execution_artifact (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  execution_id BIGINT NOT NULL,
  step_execution_id BIGINT NULL,
  artifact_type VARCHAR(32) NOT NULL,
  storage_uri VARCHAR(1000) NOT NULL,
  content_type VARCHAR(64),
  checksum VARCHAR(64),
  file_size BIGINT,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_artifact_execution(execution_id),
  FOREIGN KEY(execution_id) REFERENCES test_execution(id),
  FOREIGN KEY(step_execution_id) REFERENCES step_execution(id)
) ENGINE=InnoDB;

CREATE TABLE test_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  execution_id BIGINT NOT NULL UNIQUE,
  report_json JSON NOT NULL,
  html_uri VARCHAR(1000),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  FOREIGN KEY(execution_id) REFERENCES test_execution(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 十、定时计划
-- ------------------------------------------------------------
CREATE TABLE schedule_job (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  job_name VARCHAR(160) NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  target_id BIGINT NOT NULL,
  target_version INT NULL,
  environment_id BIGINT NOT NULL,
  cron_expr VARCHAR(100) NOT NULL,
  timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
  concurrency_policy VARCHAR(16) NOT NULL DEFAULT 'SKIP',
  next_run_at DATETIME(3),
  last_run_at DATETIME(3),
  status TINYINT NOT NULL DEFAULT 1,
  FOREIGN KEY(project_id) REFERENCES project(id),
  FOREIGN KEY(environment_id) REFERENCES test_environment(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 十一、平台治理（新增 P1）
-- ------------------------------------------------------------
CREATE TABLE system_parameter (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  param_key VARCHAR(100) NOT NULL UNIQUE,
  param_value VARCHAR(500),
  description VARCHAR(255),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;

CREATE TABLE platform_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  title VARCHAR(160) NOT NULL,
  content VARCHAR(1000),
  is_read TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_notify(user_id, is_read)
) ENGINE=InnoDB;

CREATE TABLE common_step (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  step_code VARCHAR(64) NOT NULL,
  step_name VARCHAR(128) NOT NULL,
  definition_json JSON,
  UNIQUE KEY uk_step(project_id, step_code),
  FOREIGN KEY(project_id) REFERENCES project(id)
) ENGINE=InnoDB;

CREATE TABLE common_script (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  script_code VARCHAR(64) NOT NULL,
  script_name VARCHAR(128) NOT NULL,
  script_body TEXT,
  UNIQUE KEY uk_script(project_id, script_code),
  FOREIGN KEY(project_id) REFERENCES project(id)
) ENGINE=InnoDB;

CREATE TABLE variable_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  var_key VARCHAR(128) NOT NULL,
  description VARCHAR(255),
  UNIQUE KEY uk_vdict(project_id, var_key),
  FOREIGN KEY(project_id) REFERENCES project(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 十二、审计
-- ------------------------------------------------------------
CREATE TABLE audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  project_id BIGINT NULL,
  action VARCHAR(64) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id BIGINT NULL,
  detail_json JSON,
  ip_address VARCHAR(64),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_audit_project_time(project_id, created_at)
) ENGINE=InnoDB;
