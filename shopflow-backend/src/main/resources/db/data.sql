-- =====================================================================
-- ShopFlow 初始化数据
-- 执行顺序：schema.sql -> data.sql
--
-- 说明：
-- 1. 下面三个账号的密码密文均由 BCryptPasswordEncoder 真实生成并校验通过；
-- 2. 仅用于本地开发与演示，生产环境必须删除或重置密码；
-- 3. 明文密码见注释，便于演示登录。
-- =====================================================================
USE shopflow;

-- ---------------------------------------------------------------------
-- 角色
-- ---------------------------------------------------------------------
INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN', '超级管理员', '拥有系统全部权限，可管理用户、角色与所有业务数据'),
(2, 'OPERATOR', '运营人员', '负责商品、库存与订单的日常运营，不可管理用户'),
(3, 'USER', '普通用户', '前台注册用户，可浏览商品、下单与查询自己的订单');

-- ---------------------------------------------------------------------
-- 权限（粒度到接口）
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (id, permission_code, permission_name, module, type, sort) VALUES
(1,  'category:read',     '查看分类',   'category',  3, 10),
(2,  'category:create',   '新增分类',   'category',  2, 11),
(3,  'category:update',   '修改分类',   'category',  2, 12),
(4,  'category:delete',   '删除分类',   'category',  2, 13),
(5,  'product:read',      '查看商品',   'product',   3, 20),
(6,  'product:create',    '新增商品',   'product',   2, 21),
(7,  'product:update',    '修改商品',   'product',   2, 22),
(8,  'product:delete',    '删除商品',   'product',   2, 23),
(9,  'inventory:read',    '查看库存',   'inventory', 3, 30),
(10, 'inventory:update',  '调整库存',   'inventory', 2, 31),
(11, 'order:read',        '查看订单',   'order',     3, 40),
(12, 'order:update',      '处理订单',   'order',     2, 41),
(13, 'user:read',         '查看用户',   'user',      3, 50),
(14, 'user:update',       '修改用户',   'user',      2, 51),
(15, 'user:assign-role',  '分配角色',   'user',      2, 52),
(16, 'dashboard:read',    '查看数据看板', 'dashboard', 3, 60),
(17, 'log:read',          '查看操作日志', 'log',      3, 70);

-- ---------------------------------------------------------------------
-- 角色-权限关联
-- ADMIN：全部权限
-- OPERATOR：分类/商品/库存/订单/看板
-- USER：仅查看商品与自己的订单
-- ---------------------------------------------------------------------
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4),
(2, 5), (2, 6), (2, 7), (2, 8),
(2, 9), (2, 10),
(2, 11), (2, 12),
(2, 16);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3, 5), (3, 11);

-- ---------------------------------------------------------------------
-- 用户（密码为 BCrypt 密文，均已通过 matches() 校验）
-- admin    / Admin@123456
-- operator / Operator@123456
-- demo     / User@123456
-- ---------------------------------------------------------------------
INSERT INTO sys_user (id, username, password, nickname, email, phone, status) VALUES
(1, 'admin',    '$2a$10$7Q8SOgH38YC9OZxMep7h5eZJDFHV2CAyHwF2uSn4S539kQS0QqXvq', '系统管理员', 'admin@shopflow.local',    '13800000001', 1),
(2, 'operator', '$2a$10$nsof9ZR5swjzu5IVWkw3feo56d9RR8kujOS/sysVrQ0DDNk0mpFc2', '运营小王',   'operator@shopflow.local', '13800000002', 1),
(3, 'demo',     '$2a$10$PK6gGrld5qjAzmDbjTJ.f.NKT7DelAvWDaRK6He6wGzOImFVoaC/W', '演示用户',   'demo@shopflow.local',     '13800000003', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3);

-- ---------------------------------------------------------------------
-- 商品分类（两级树）
-- ---------------------------------------------------------------------
INSERT INTO category (id, parent_id, name, level, sort, status) VALUES
(1, 0, '电子数码', 1, 1, 1),
(2, 0, '电脑办公', 1, 2, 1),
(3, 0, '家用电器', 1, 3, 1),
(4, 1, '手机通讯', 2, 1, 1),
(5, 2, '笔记本电脑', 2, 1, 1),
(6, 2, '键盘鼠标', 2, 2, 1),
(7, 3, '厨房电器', 2, 1, 1);

-- ---------------------------------------------------------------------
-- 商品与库存（演示数据）
-- ---------------------------------------------------------------------
INSERT INTO product (id, category_id, name, sku, subtitle, price, original_price, status, sales, sort) VALUES
(1, 4, 'ShopFlow 智能手机 Pro 12+256G', 'SF-PHONE-0001', '6.7英寸 120Hz 高刷屏',      3999.00, 4299.00, 1, 128, 1),
(2, 5, 'ShopFlow 轻薄笔记本 14 英寸',    'SF-LAPTOP-0002', '1.2kg 轻薄机身 16G+512G',   5499.00, 5999.00, 1, 76,  2),
(3, 6, 'ShopFlow 机械键盘 87 键',        'SF-KEYBOARD-0003', '热插拔轴体 三模连接',       399.00,  499.00,  1, 315, 3),
(4, 6, 'ShopFlow 无线鼠标 静音版',       'SF-MOUSE-0004',  '静音微动 人体工学设计',     129.00,  169.00,  1, 542, 4),
(5, 7, 'ShopFlow 破壁料理机 1.75L',      'SF-BLENDER-0005', '静音降噪 12 大功能',        699.00,  899.00,  1, 89,  5),
(6, 4, 'ShopFlow 蓝牙耳机 降噪版',       'SF-EARBUDS-0006', '主动降噪 40 小时续航',      499.00,  599.00,  0, 0,   6);

INSERT INTO inventory (product_id, total_stock, available_stock, locked_stock, warn_stock, version) VALUES
(1, 500, 500, 0, 20, 0),
(2, 300, 300, 0, 10, 0),
(3, 100, 100, 0, 15, 0),
(4, 800, 800, 0, 50, 0),
(5, 200, 200, 0, 10, 0),
(6, 150, 150, 0, 10, 0);