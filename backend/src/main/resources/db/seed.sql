-- 鑫火锅炉单企业初始数据（幂等：显式主键 + INSERT IGNORE；初始密码由启动器 BCrypt 化）
INSERT IGNORE INTO tenant (id,code,name,industry,status,open_time,expire_time,max_users,max_customers,contact_name,contact_phone,remark) VALUES
(1,'HLJ','鑫火锅炉（黑龙江）有限公司','锅炉制造与销售','NORMAL','2026-01-01 00:00:00','2027-12-31 23:59:59',50,2000,'赵总','0451-88001111','企业基础资料');

INSERT IGNORE INTO tenant_user (id,tenant_id,username,password,name,phone,email,role_code,customer_id,status,remark) VALUES
(101,1,'admin','plain:123456','系统管理员','13600001001','admin@xinhuo.hlj','ADMIN',NULL,'NORMAL','系统管理员');

INSERT IGNORE INTO role_def (id,code,name,scope,permissions,sort) VALUES
(2,'ADMIN','管理员','COMPANY','*',2),
(9,'CUSTOMER','客户','COMPANY','self',9);

INSERT IGNORE INTO sys_dict_global (id,dict_type,dict_key,dict_value,sort,status,remark) VALUES
(1,'boiler_type','steam','蒸汽锅炉',1,'NORMAL','按TSG 11'),
(2,'boiler_type','hotwater','热水锅炉',2,'NORMAL',''),
(3,'boiler_type','thermal','导热油炉',3,'NORMAL',''),
(4,'boiler_type','wasteheat','余热锅炉',4,'NORMAL',''),
(10,'fuel_type','gas','燃气',1,'NORMAL','天然气/LNG'),
(11,'fuel_type','oil','燃油',2,'NORMAL',''),
(12,'fuel_type','electric','电',3,'NORMAL',''),
(13,'fuel_type','biomass','生物质',4,'NORMAL',''),
(14,'fuel_type','coal','燃煤',5,'NORMAL',''),
(20,'pressure_level','atmospheric','常压',1,'NORMAL',''),
(21,'pressure_level','low','低压',2,'NORMAL','≤1.6MPa'),
(22,'pressure_level','medium','中压',3,'NORMAL',''),
(23,'pressure_level','high','高压',4,'NORMAL',''),
(30,'capacity','1','1 t/h',1,'NORMAL','或0.7MW'),
(31,'capacity','2','2 t/h',2,'NORMAL',''),
(32,'capacity','4','4 t/h',3,'NORMAL',''),
(33,'capacity','6','6 t/h',4,'NORMAL',''),
(34,'capacity','10','10 t/h',5,'NORMAL',''),
(35,'capacity','20','20 t/h',6,'NORMAL',''),
(36,'capacity','2.8mw','2.8 MW',7,'NORMAL','热水锅炉'),
(40,'cert_type','manufacture','制造许可',1,'NORMAL','A/B级'),
(41,'cert_type','inspection','产品监检证明',2,'NORMAL',''),
(42,'cert_type','energy','能效测试报告',3,'NORMAL',''),
(43,'cert_type','install','安装许可',4,'NORMAL',''),
(44,'cert_type','periodic','定期检验记录',5,'NORMAL','年检/定检'),
(50,'demand_status','NEW','新需求',1,'NORMAL',''),
(51,'demand_status','CONSULTING','咨询中',2,'NORMAL',''),
(52,'demand_status','INTRODUCING','介绍中',3,'NORMAL',''),
(53,'demand_status','TALKING','谈价中',4,'NORMAL',''),
(54,'demand_status','DEAL','成交',5,'NORMAL',''),
(55,'demand_status','LOST','流失',6,'NORMAL',''),
(60,'ticket_type','REPAIR','报修',1,'NORMAL',''),
(61,'ticket_type','INSTALL','安装',2,'NORMAL',''),
(62,'ticket_type','MAINTAIN','保养',3,'NORMAL',''),
(63,'ticket_type','INSPECT','年检',4,'NORMAL',''),
(70,'maintenance_project','MAINTAIN','定期保养',1,'NORMAL',''),
(71,'maintenance_project','ANNUAL','年检',2,'NORMAL',''),
(72,'maintenance_project','PERIODIC','定检',3,'NORMAL',''),
(80,'customer_source','H5','H5门户',1,'NORMAL',''),
(81,'customer_source','MP','小程序',2,'NORMAL',''),
(82,'customer_source','OA','公众号',3,'NORMAL',''),
(83,'customer_source','PHONE','电话',4,'NORMAL',''),
(84,'customer_source','MANUAL','人工录入',5,'NORMAL','');

INSERT IGNORE INTO sys_dict_tenant (id,tenant_id,dict_type,dict_key,dict_value,sort,status,remark) VALUES
(1001,1,'boiler_series','XH-WNS','鑫火WNS系列',1,'NORMAL','企业自定义扩展项'),
(1002,1,'boiler_series','XH-SZL','鑫火SZL系列',2,'NORMAL','');

INSERT IGNORE INTO platform_param (id,param_key,param_value,remark) VALUES
(1,'certWarnDays','90','资质证书到期预警天数'),
(2,'maintenanceWarnDays','30','维保/年检到期预警天数'),
(3,'paymentOverdueDays','7','回款逾期提醒天数'),
(4,'demandFollowTimeoutDays','2','需求超时未跟进天数'),
(5,'tenantExpireWarnDays','30','企业服务到期预警天数'),
(6,'quoteApprovalThreshold','0.95','报价低于底价比例触发审批'),
(7,'autoDisableExpiredTenant','true','企业服务到期自动停用');

INSERT IGNORE INTO tenant_config (id,tenant_id,config_key,config_value,group_name,encrypted,remark) VALUES
(1,1,'company_name','鑫火锅炉（黑龙江）有限公司','BASIC',0,'企业名称'),
(2,1,'logo','','BASIC',0,'Logo'),
(3,1,'service_phone','400-800-6688','BASIC',0,'客服电话'),
(4,1,'wechat_oa','xinhuo-boiler','WECHAT',0,'公众号'),
(5,1,'mp_appid','','WECHAT',0,'小程序 AppID'),
(6,1,'payment_enabled','false','PAYMENT',0,'微信支付开关'),
(7,1,'notify_wechat','true','NOTIFY',0,'公众号模板消息开关'),
(8,1,'notify_sms','false','NOTIFY',0,'短信通道开关'),
(9,1,'notify_station','true','NOTIFY',0,'站内消息开关'),
(10,1,'default_language','zh-CN','BASIC',0,'默认语言'),
(11,1,'seo_keywords','锅炉厂家,工业锅炉选型,燃气锅炉,蒸汽锅炉,锅炉价格','PORTAL',0,'门户SEO关键词'),
(12,1,'seo_description','鑫火锅炉，专业工业锅炉制造与全生命周期服务，提供燃气/燃油/生物质锅炉选型、报价、安装与维保。','PORTAL',0,'门户SEO描述'),
(13,1,'icp','黑ICP备2026000001号','PORTAL',0,'ICP备案');

INSERT IGNORE INTO language (id,tenant_id,code,name,enabled,sort,default_flag) VALUES
(1,1,'zh-CN','简体中文',1,1,'Y'),
(2,1,'en','English',0,2,'N');


INSERT IGNORE INTO storage_config (id,tenant_id,driver,config_json,active,status) VALUES
(1,1,'LOCAL','{"basePath":"./uploads"}',1,'NORMAL');

INSERT IGNORE INTO wechat_config (id,tenant_id,mp_token,oa_token,welcome_text,keyword_reply) VALUES
(1,1,'xinhuo-mp-token','xinhuo-oa-token','欢迎关注鑫火锅炉，回复“需求”提交采购需求，回复“报价”查询报价，回复“报修”进入售后报修。','需求:https://portal.xinhuo.local/demand|报价:https://portal.xinhuo.local/account?tab=quotes|报修:https://portal.xinhuo.local/account?tab=service|找顾问:400-800-6688|产品:https://portal.xinhuo.local/products');

INSERT IGNORE INTO template_message_config (id,tenant_id,biz_type,biz_name,template_id,title,enabled,fields_json,min_interval_seconds,jump_url) VALUES
(1,1,'DEMAND_CONFIRM','需求确认通知','','需求已受理','1','demandNo,boilerType,capacity,status','60','/account'),
(2,1,'CONSULT_REPLY','咨询回复提醒','','顾问已回复','1','demandNo,answer,link','60','/account'),
(3,1,'SOLUTION_PUSH','方案推送通知','','您的新方案已送达','1','demandNo,solutionName,link','60','/account'),
(4,1,'QUOTE_REMIND','报价提醒','','报价单待查看','1','quoteNo,amount,validUntil','60','/account'),
(5,1,'QUOTE_EXPIRE','报价有效期提醒','','报价即将到期','1','quoteNo,expireDate,link','60','/account'),
(6,1,'DEAL_NOTICE','成交通知','','合同已成交','1','contractNo,amount,paymentProgress','60','/account'),
(7,1,'DELIVERY_NOTICE','发货安装通知','','设备履约进度更新','1','contractNo,stage,progress','60','/account'),
(8,1,'MAINTENANCE_REMIND','维保定检提醒','','设备维保/定检到期提醒','1','equipmentNo,project,dueDate','60','/account'),
(9,1,'TICKET_NOTICE','工单进度通知','','售后服务工单进度','1','ticketNo,status,technician','60','/account'),
(10,1,'VISIT_INVITE','回访邀请','','邀请您评价本次服务','1','contractNo,reviewLink','60','/account');

INSERT IGNORE INTO menu (id,tenant_id,target,parent_id,title,icon,path,menu_type,external_url,sort,visible) VALUES
(1,1,'ADMIN',0,'工作台','DataBoard','/dashboard','PAGE','',1,1),
(2,1,'ADMIN',0,'客户档案','User','/customers','PAGE','',2,1),
(3,1,'ADMIN',0,'锅炉产品','Box','/products','PAGE','',3,1),
(4,1,'ADMIN',0,'资质证书','Document','/certificates','PAGE','',4,1),
(5,1,'ADMIN',0,'需求线索','Tickets','/demands','PAGE','',5,1),
(6,1,'ADMIN',0,'售前咨询','ChatDotRound','/consultations','PAGE','',6,1),
(7,1,'ADMIN',0,'方案与案例','Reading','/solutions','PAGE','',7,1),
(8,1,'ADMIN',0,'报价议价','Money','/quotes','PAGE','',8,1),
(9,1,'ADMIN',0,'合同履约','Files','/contracts','PAGE','',9,1),
(10,1,'ADMIN',0,'售后服务','Service','/tickets','PAGE','',10,1),
(11,1,'ADMIN',0,'维保计划','Calendar','/maintenance','PAGE','',11,1),
(12,1,'ADMIN',0,'备品备件','Setting','/spares','PAGE','',12,1),
(13,1,'ADMIN',0,'服务知识库','Collection','/knowledge','PAGE','',13,1),
(14,1,'ADMIN',0,'客户回访','Phone','/visits','PAGE','',14,1),
(15,1,'ADMIN',0,'转介绍','Share','/referrals','PAGE','',15,1),
(16,1,'ADMIN',0,'报表中心','TrendCharts','/reports','PAGE','',16,1),
(17,1,'ADMIN',0,'预警中心','Warning','/warnings','PAGE','',17,1),
(18,1,'ADMIN',0,'消息通知','Bell','/notifications','PAGE','',18,1),
(19,1,'ADMIN',0,'用户管理','Avatar','/users','PAGE','',19,1),
(20,1,'ADMIN',0,'数据字典','Grid','/dicts','PAGE','',20,1),
(21,1,'ADMIN',0,'菜单配置','Menu','/menus','PAGE','',21,1),
(22,1,'ADMIN',0,'多语言','Switch','/i18n','PAGE','',22,1),
(23,1,'ADMIN',0,'对象存储','Cloudy','/storage','PAGE','',23,1),
(24,1,'ADMIN',0,'微信配置','ChatLineRound','/wechat','PAGE','',24,1),
(25,1,'ADMIN',0,'系统设置','Tools','/settings','PAGE','',25,1),
(26,1,'ADMIN',0,'审计日志','List','/audits','PAGE','',26,1),
(50,1,'MP',0,'首页','HomeFilled','/pages/index/index','PAGE','',1,1),
(51,1,'MP',0,'需求','Tickets','/pages/demand/list','PAGE','',2,1),
(52,1,'MP',0,'售后','Service','/pages/service/tickets','PAGE','',3,1),
(53,1,'MP',0,'我的','User','/pages/profile/index','PAGE','',4,1),
(60,1,'H5',0,'首页','HomeFilled','/','PAGE','',1,1),
(61,1,'H5',0,'产品中心','Box','/products','PAGE','',2,1),
(62,1,'H5',0,'需求提交','Tickets','/demand','PAGE','',3,1),
(63,1,'H5',0,'资质核验','DocumentChecked','/verify','PAGE','',4,1),
(64,1,'H5',0,'资讯公告','Bell','/announcements','PAGE','',5,1),
(65,1,'H5',0,'客户中心','User','/account','PAGE','',6,1);















INSERT IGNORE INTO contract_template (id,tenant_id,name,content,status) VALUES
(1,1,'标准锅炉销售合同','<h2>锅炉销售合同</h2><p>甲方（买方）：{{customerName}}</p><p>乙方（卖方）：{{tenantName}}</p><p>产品型号：{{model}}，数量：{{quantity}}，合同金额：{{amount}}元。</p><p>付款条件：{{paymentTerms}}</p><p>质保期：{{warranty}}</p>','NORMAL'),
(2,1,'安装调试合同模板','<h2>安装调试合同</h2><p>安装范围、工期、验收标准与特种设备合规责任。</p>','NORMAL');

















INSERT IGNORE INTO id_sequence (id,tenant_id,biz_type,current_value) VALUES
(1,1,'CUSTOMER',1),(2,1,'DEMAND',4),(3,1,'SOLUTION',2),(4,1,'QUOTE',3),(5,1,'CONTRACT',1),(6,1,'TICKET',3),(7,1,'EQUIPMENT',2),(8,1,'ORDER',1),(9,1,'INVOICE',1),(10,1,'CERT',0),
(13,1,'VISIT',1),(14,1,'SPARE',0);
