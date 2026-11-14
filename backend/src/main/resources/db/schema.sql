-- 鑫火锅炉单企业数据库结构（MySQL 8 / utf8mb4）
CREATE TABLE IF NOT EXISTS tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  industry VARCHAR(64),
  status VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  open_time DATETIME,
  expire_time DATETIME,
  max_users INT DEFAULT 20,
  max_customers INT DEFAULT 500,
  contact_name VARCHAR(64),
  contact_phone VARCHAR(64),
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_company_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(128) NOT NULL,
  name VARCHAR(64),
  phone VARCHAR(32),
  email VARCHAR(128),
  role_code VARCHAR(32) NOT NULL,
  customer_id BIGINT,
  status VARCHAR(16) DEFAULT 'NORMAL',
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_tenant_username (tenant_id, username),
  KEY idx_tenant_user_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_def (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64),
  scope VARCHAR(16) DEFAULT 'COMPANY',
  permissions TEXT,
  sort INT DEFAULT 0,
  UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_dict_global (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dict_type VARCHAR(64) NOT NULL,
  dict_key VARCHAR(64) NOT NULL,
  dict_value VARCHAR(128) NOT NULL,
  sort INT DEFAULT 0,
  status VARCHAR(16) DEFAULT 'NORMAL',
  remark VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_global_dict (dict_type, dict_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_dict_tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  dict_type VARCHAR(64) NOT NULL,
  dict_key VARCHAR(64) NOT NULL,
  dict_value VARCHAR(128) NOT NULL,
  sort INT DEFAULT 0,
  status VARCHAR(16) DEFAULT 'NORMAL',
  remark VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_tenant_dict (tenant_id, dict_type, dict_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS platform_param (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  param_key VARCHAR(64) NOT NULL,
  param_value VARCHAR(500),
  remark VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_param_key (param_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  config_key VARCHAR(64) NOT NULL,
  config_value TEXT,
  group_name VARCHAR(32) DEFAULT 'BASIC',
  encrypted TINYINT DEFAULT 0,
  remark VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_tenant_config (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS language (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(16) NOT NULL,
  name VARCHAR(32) NOT NULL,
  enabled TINYINT DEFAULT 1,
  sort INT DEFAULT 0,
  default_flag VARCHAR(2) DEFAULT 'N',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_tenant_lang (tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  target VARCHAR(16) NOT NULL DEFAULT 'ADMIN',
  parent_id BIGINT DEFAULT 0,
  title VARCHAR(64) NOT NULL,
  icon VARCHAR(64),
  path VARCHAR(128),
  menu_type VARCHAR(16) DEFAULT 'PAGE',
  external_url VARCHAR(500),
  sort INT DEFAULT 0,
  visible TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_menu (tenant_id, target)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT,
  title VARCHAR(200) NOT NULL,
  content MEDIUMTEXT,
  status VARCHAR(16) DEFAULT 'PUBLISHED',
  source VARCHAR(16) DEFAULT 'SYSTEM',
  publish_time DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_announcement (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT,
  username VARCHAR(64),
  module VARCHAR(64),
  action VARCHAR(64),
  target VARCHAR(128),
  detail TEXT,
  ip VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS storage_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  driver VARCHAR(16) NOT NULL DEFAULT 'LOCAL',
  config_json TEXT,
  active TINYINT DEFAULT 1,
  status VARCHAR(16) DEFAULT 'NORMAL',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_storage_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wechat_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  mp_appid VARCHAR(64),
  mp_secret_enc VARCHAR(500),
  mp_token VARCHAR(128),
  mp_aes_key_enc VARCHAR(500),
  mp_server_url VARCHAR(500),
  mp_request_domain VARCHAR(255),
  mp_upload_domain VARCHAR(255),
  mp_download_domain VARCHAR(255),
  oa_appid VARCHAR(64),
  oa_secret_enc VARCHAR(500),
  oa_server_url VARCHAR(500),
  oa_token VARCHAR(128),
  oa_aes_key_enc VARCHAR(500),
  oauth_domain VARCHAR(255),
  js_domain VARCHAR(255),
  open_platform_appid VARCHAR(64),
  welcome_text VARCHAR(1000),
  keyword_reply TEXT,
  menu_json MEDIUMTEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_wechat_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS template_message_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_name VARCHAR(64),
  template_id VARCHAR(128),
  title VARCHAR(128),
  enabled TINYINT DEFAULT 1,
  fields_json TEXT,
  min_interval_seconds INT DEFAULT 60,
  jump_url VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_template_biz (tenant_id, biz_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  user_type VARCHAR(16) DEFAULT 'ADMIN',
  user_id BIGINT,
  customer_id BIGINT,
  title VARCHAR(200),
  content TEXT,
  biz_type VARCHAR(32),
  read_flag TINYINT DEFAULT 0,
  read_time DATETIME,
  link VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_notification (tenant_id, user_type, user_id, customer_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notify_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  channel VARCHAR(16),
  biz_type VARCHAR(32),
  customer_id BIGINT,
  target VARCHAR(128),
  template_id VARCHAR(128),
  status VARCHAR(16),
  result VARCHAR(1000),
  retry_count INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_notify_log (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS file_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  original_name VARCHAR(255),
  stored_name VARCHAR(255),
  url VARCHAR(500),
  storage_path VARCHAR(500),
  size BIGINT,
  content_type VARCHAR(128),
  category VARCHAR(64),
  driver VARCHAR(16),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_file_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wechat_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(200),
  media_type VARCHAR(32) DEFAULT 'NEWS',
  media_id VARCHAR(128),
  content MEDIUMTEXT,
  url VARCHAR(500),
  status VARCHAR(16) DEFAULT 'DRAFT',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_material_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wechat_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(64),
  wx_tag_id BIGINT,
  fan_count INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_wx_tag (tenant_id, wx_tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wechat_fan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  openid VARCHAR(128),
  unionid VARCHAR(128),
  nickname VARCHAR(128),
  tags VARCHAR(255),
  subscribed TINYINT DEFAULT 1,
  bind_customer_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_wx_fan (tenant_id, openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wechat_mass_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  target_type VARCHAR(16),
  tag_id VARCHAR(64),
  openids VARCHAR(1000),
  content_type VARCHAR(16),
  content TEXT,
  media_id VARCHAR(128),
  status VARCHAR(16),
  result VARCHAR(1000),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_mass (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  customer_no VARCHAR(64) NOT NULL,
  name VARCHAR(200) NOT NULL,
  industry VARCHAR(64),
  scale VARCHAR(64),
  region VARCHAR(64),
  address VARCHAR(255),
  source VARCHAR(32),
  category VARCHAR(32),
  level VARCHAR(16),
  status VARCHAR(16) DEFAULT 'NORMAL',
  owner_id BIGINT,
  remark VARCHAR(1000),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_customer_no (tenant_id, customer_no),
  UNIQUE KEY uk_customer_name (tenant_id, name),
  KEY idx_customer_owner (tenant_id, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contact (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  name VARCHAR(64),
  title VARCHAR(64),
  phone_enc VARCHAR(255),
  phone_mask VARCHAR(32),
  email VARCHAR(128),
  wechat_openid VARCHAR(128),
  unionid VARCHAR(128),
  primary_flag TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_contact_customer (tenant_id, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(64) NOT NULL,
  model VARCHAR(128) NOT NULL,
  boiler_type VARCHAR(32),
  fuel_type VARCHAR(32),
  capacity VARCHAR(64),
  pressure_level VARCHAR(32),
  usage_desc VARCHAR(255),
  tech_params TEXT,
  images TEXT,
  guide_price DECIMAL(14,2),
  online_status VARCHAR(16) DEFAULT 'ONLINE',
  compliance_status VARCHAR(16) DEFAULT 'OK',
  cert_summary VARCHAR(500),
  remark VARCHAR(1000),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_product_code (tenant_id, code),
  KEY idx_product_type (tenant_id, boiler_type, fuel_type, online_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS certificate (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  cert_type VARCHAR(32) NOT NULL,
  cert_no VARCHAR(64) NOT NULL,
  issuer VARCHAR(128),
  valid_from DATE,
  valid_to DATE,
  product_id BIGINT,
  customer_id BIGINT,
  equipment_id BIGINT,
  file_url VARCHAR(500),
  status VARCHAR(16) DEFAULT 'VALID',
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_cert_no (tenant_id, cert_no),
  KEY idx_cert_warn (tenant_id, valid_to, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS demand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  demand_no VARCHAR(64) NOT NULL,
  customer_id BIGINT,
  contact_id BIGINT,
  contact_name VARCHAR(64),
  contact_phone_mask VARCHAR(32),
  boiler_type VARCHAR(32),
  fuel_type VARCHAR(32),
  capacity VARCHAR(64),
  pressure_level VARCHAR(32),
  usage_desc VARCHAR(255),
  budget_min DECIMAL(14,2),
  budget_max DECIMAL(14,2),
  region VARCHAR(64),
  expect_time DATE,
  source VARCHAR(32),
  owner_id BIGINT,
  owner_name VARCHAR(64),
  status VARCHAR(16) DEFAULT 'NEW',
  follow_next_time DATETIME,
  last_follow_time DATETIME,
  description VARCHAR(1000),
  share_token VARCHAR(64),
  assign_remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_demand_no (tenant_id, demand_no),
  KEY idx_demand_status (tenant_id, status, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS follow_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  demand_id BIGINT NOT NULL,
  user_id BIGINT,
  user_name VARCHAR(64),
  follow_time DATETIME,
  method VARCHAR(32),
  content VARCHAR(1000),
  next_time DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_follow_demand (tenant_id, demand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS consultation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  customer_id BIGINT,
  demand_id BIGINT,
  channel VARCHAR(16) DEFAULT 'ONLINE',
  question TEXT,
  answer TEXT,
  consultant_id BIGINT,
  consultant_name VARCHAR(64),
  status VARCHAR(16) DEFAULT 'WAITING',
  faq_id BIGINT,
  customer_name VARCHAR(200),
  contact_phone_mask VARCHAR(32),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_consult (tenant_id, customer_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS faq (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  category VARCHAR(64),
  question VARCHAR(500),
  answer TEXT,
  keywords VARCHAR(255),
  status VARCHAR(16) DEFAULT 'NORMAL',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_faq (tenant_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS solution (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  solution_no VARCHAR(64) NOT NULL,
  demand_id BIGINT,
  name VARCHAR(200),
  version INT DEFAULT 1,
  product_id BIGINT,
  product_name VARCHAR(128),
  content MEDIUMTEXT,
  attachments TEXT,
  status VARCHAR(16) DEFAULT 'DRAFT',
  share_token VARCHAR(64),
  view_count INT DEFAULT 0,
  confirm_time DATETIME,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_solution_no (tenant_id, solution_no),
  KEY idx_solution_demand (tenant_id, demand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS case_library (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(200),
  industry VARCHAR(64),
  scene VARCHAR(128),
  params VARCHAR(255),
  effect VARCHAR(500),
  images TEXT,
  content MEDIUMTEXT,
  status VARCHAR(16) DEFAULT 'PUBLISHED',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_case (tenant_id, industry)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS quote (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  quote_no VARCHAR(64) NOT NULL,
  demand_id BIGINT,
  solution_id BIGINT,
  customer_id BIGINT,
  version INT DEFAULT 1,
  product_id BIGINT,
  product_name VARCHAR(128),
  unit_price DECIMAL(14,2),
  quantity INT DEFAULT 1,
  tax_rate DECIMAL(6,4),
  amount DECIMAL(14,2),
  freight DECIMAL(14,2) DEFAULT 0,
  install_fee DECIMAL(14,2) DEFAULT 0,
  total_amount DECIMAL(14,2),
  payment_terms VARCHAR(500),
  warranty VARCHAR(64),
  valid_until DATE,
  floor_price DECIMAL(14,2),
  status VARCHAR(16) DEFAULT 'DRAFT',
  approval_status VARCHAR(16) DEFAULT 'NONE',
  approval_by BIGINT,
  approval_comment VARCHAR(500),
  share_token VARCHAR(64),
  confirm_time DATETIME,
  negotiation_count INT DEFAULT 0,
  sent_time DATETIME,
  owner_id BIGINT,
  customer_name VARCHAR(200),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_quote_no (tenant_id, quote_no),
  KEY idx_quote_status (tenant_id, status, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS negotiation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  quote_id BIGINT NOT NULL,
  round_no INT DEFAULT 1,
  party VARCHAR(16),
  amount DECIMAL(14,2),
  remark VARCHAR(500),
  operator VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_negotiation (tenant_id, quote_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contract (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  contract_no VARCHAR(64) NOT NULL,
  quote_id BIGINT,
  customer_id BIGINT,
  customer_name VARCHAR(200),
  title VARCHAR(200),
  amount DECIMAL(14,2),
  payment_terms VARCHAR(500),
  template_name VARCHAR(128),
  audit_status VARCHAR(16) DEFAULT 'PENDING',
  sign_status VARCHAR(16) DEFAULT 'UNSIGNED',
  sign_type VARCHAR(16),
  sign_time DATETIME,
  archive_url VARCHAR(500),
  owner_id BIGINT,
  status VARCHAR(16) DEFAULT 'DRAFT',
  content MEDIUMTEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_contract_no (tenant_id, contract_no),
  KEY idx_contract_customer (tenant_id, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contract_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(128),
  content MEDIUMTEXT,
  status VARCHAR(16) DEFAULT 'NORMAL',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_contract_tpl (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  contract_id BIGINT,
  production_progress VARCHAR(64),
  delivery_status VARCHAR(32),
  expected_delivery DATE,
  shipped_at DATETIME,
  arrived_at DATETIME,
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_order_no (tenant_id, order_no),
  KEY idx_order_contract (tenant_id, contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  contract_id BIGINT,
  phase VARCHAR(32),
  amount DECIMAL(14,2),
  plan_date DATE,
  received_date DATE,
  received_amount DECIMAL(14,2) DEFAULT 0,
  status VARCHAR(16) DEFAULT 'UNPAID',
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_payment_contract (tenant_id, contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  contract_id BIGINT,
  invoice_no VARCHAR(64),
  title VARCHAR(200),
  tax_no VARCHAR(64),
  amount DECIMAL(14,2),
  status VARCHAR(16) DEFAULT 'ISSUED',
  issued_at DATE,
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_invoice_contract (tenant_id, contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS installation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  contract_id BIGINT,
  stage VARCHAR(32),
  plan_date DATE,
  actual_date DATE,
  photos TEXT,
  acceptance_url VARCHAR(500),
  acceptance_signer VARCHAR(64),
  remark VARCHAR(500),
  status VARCHAR(16) DEFAULT 'PENDING',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_install_contract (tenant_id, contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS equipment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  equipment_no VARCHAR(64) NOT NULL,
  customer_id BIGINT,
  contract_id BIGINT,
  product_id BIGINT,
  model VARCHAR(128),
  capacity VARCHAR(64),
  pressure_level VARCHAR(32),
  install_addr VARCHAR(255),
  install_date DATE,
  warranty_end DATE,
  status VARCHAR(16) DEFAULT 'ACTIVE',
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_equipment_no (tenant_id, equipment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS service_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  ticket_no VARCHAR(64) NOT NULL,
  customer_id BIGINT,
  customer_name VARCHAR(200),
  equipment_id BIGINT,
  equipment_no VARCHAR(64),
  type VARCHAR(32),
  description VARCHAR(1000),
  photos TEXT,
  priority VARCHAR(16) DEFAULT 'NORMAL',
  technician_id BIGINT,
  technician_name VARCHAR(64),
  status VARCHAR(16) DEFAULT 'ACCEPTED',
  accepted_time DATETIME,
  done_time DATETIME,
  solution VARCHAR(1000),
  satisfaction_score INT,
  review VARCHAR(1000),
  owner_id BIGINT,
  contact_name VARCHAR(64),
  contact_phone_mask VARCHAR(32),
  source VARCHAR(16),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_ticket_no (tenant_id, ticket_no),
  KEY idx_ticket_status (tenant_id, status, technician_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ticket_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  ticket_id BIGINT NOT NULL,
  operator VARCHAR(64),
  action VARCHAR(32),
  content VARCHAR(1000),
  photos TEXT,
  parts_json TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_ticket_record (tenant_id, ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS maintenance_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  equipment_id BIGINT,
  equipment_no VARCHAR(64),
  customer_id BIGINT,
  customer_name VARCHAR(200),
  project VARCHAR(64),
  cycle_months INT DEFAULT 12,
  next_date DATE,
  last_done_date DATE,
  status VARCHAR(16) DEFAULT 'ACTIVE',
  remind_days INT DEFAULT 30,
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_maint_warn (tenant_id, status, next_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spare_part (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128),
  model VARCHAR(128),
  spec VARCHAR(128),
  unit VARCHAR(16),
  supplier VARCHAR(128),
  unit_price DECIMAL(14,2),
  stock INT DEFAULT 0,
  safety_stock INT DEFAULT 0,
  image VARCHAR(500),
  status VARCHAR(16) DEFAULT 'NORMAL',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_spare_code (tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spare_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  part_id BIGINT,
  type VARCHAR(16),
  quantity INT,
  ticket_id BIGINT,
  operator VARCHAR(64),
  remark VARCHAR(500),
  stock_after INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_spare_record (tenant_id, part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS referral (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  source_customer_id BIGINT,
  source_customer_name VARCHAR(200),
  target_company VARCHAR(200),
  contact_name VARCHAR(64),
  phone_mask VARCHAR(32),
  demand_desc VARCHAR(500),
  status VARCHAR(16) DEFAULT 'NEW',
  owner_id BIGINT,
  reward VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_referral (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_visit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  customer_id BIGINT,
  customer_name VARCHAR(200),
  contract_id BIGINT,
  plan_date DATE,
  visit_time DATETIME,
  content VARCHAR(1000),
  score INT,
  status VARCHAR(16) DEFAULT 'PLANNED',
  owner_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_visit (tenant_id, status, plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS satisfaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  ticket_id BIGINT,
  contract_id BIGINT,
  customer_id BIGINT,
  score INT,
  comment VARCHAR(1000),
  source VARCHAR(16),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_satisfaction (tenant_id, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  category VARCHAR(64),
  title VARCHAR(200),
  content MEDIUMTEXT,
  keywords VARCHAR(255),
  status VARCHAR(16) DEFAULT 'PUBLISHED',
  view_count INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_knowledge (tenant_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS share_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  biz_type VARCHAR(32),
  biz_id BIGINT,
  biz_no VARCHAR(64),
  source VARCHAR(32),
  ip VARCHAR(64),
  user_agent VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  KEY idx_share (tenant_id, biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS id_sequence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  current_value BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sequence (tenant_id, biz_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
