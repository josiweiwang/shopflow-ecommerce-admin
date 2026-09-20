-- =====================================================================
-- ShopFlow 电商后台管理系统 - 数据库初始化脚本
-- 适用：MySQL 8.0.16+（使用了 CHECK 约束）
-- 字符集：utf8mb4 / utf8mb4_0900_ai_ci
--
-- 设计要点：
-- 1. 逻辑删除字段 deleted 为 BIGINT，删除时写入该行主键 ID，
--    使 (sku, deleted) 这类唯一索引既能约束活跃数据、又能保留历史删除记录；
-- 2. 不建物理外键，引用完整性由应用层保证，便于后续归档与分库分表；
-- 3. 金额统一 DECIMAL(12,2)，时间字段由数据库默认值兜底。
-- =====================================================================
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS shopflow
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE shopflow;

-- ---------------------------------------------------------------------
-- 1. 用户表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username      VARCHAR(32)     NOT NULL                COMMENT '登录用户名',
    password      CHAR(60)        NOT NULL                COMMENT 'BCrypt 加密后的密码',
    nickname      VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '昵称',
    email         VARCHAR(64)              DEFAULT NULL   COMMENT '邮箱(可空唯一)',
    phone         VARCHAR(20)              DEFAULT NULL   COMMENT '手机号(可空唯一)',
    avatar        VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '头像地址',
    status        TINYINT         NOT NULL DEFAULT 1      COMMENT '状态: 0-禁用 1-启用',
    last_login_at DATETIME                 DEFAULT NULL   COMMENT '最后登录时间',
    last_login_ip VARCHAR(45)     NOT NULL DEFAULT ''     COMMENT '最后登录IP(兼容IPv6)',
    deleted       BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-未删除 非0-写入主键ID',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '创建人ID',
    update_by     BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username, deleted),
    UNIQUE KEY uk_email (email, deleted),
    UNIQUE KEY uk_phone (phone, deleted),
    KEY idx_status_create_time (status, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ---------------------------------------------------------------------
-- 2. 角色表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code   VARCHAR(32)     NOT NULL                COMMENT '角色编码: ADMIN/OPERATOR/USER',
    role_name   VARCHAR(32)     NOT NULL                COMMENT '角色名称',
    description VARCHAR(128)    NOT NULL DEFAULT ''     COMMENT '角色描述',
    status      TINYINT         NOT NULL DEFAULT 1      COMMENT '状态: 0-禁用 1-启用',
    deleted     BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-未删除 非0-写入主键ID',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

-- ---------------------------------------------------------------------
-- 3. 权限表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(64)     NOT NULL                COMMENT '权限码, 如 product:create',
    permission_name VARCHAR(64)     NOT NULL                COMMENT '权限名称',
    module          VARCHAR(32)     NOT NULL                COMMENT '所属模块: user/category/product/inventory/order/dashboard/log',
    type            TINYINT         NOT NULL DEFAULT 3      COMMENT '类型: 1-菜单 2-按钮 3-接口',
    sort            INT             NOT NULL DEFAULT 0      COMMENT '排序',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '状态: 0-禁用 1-启用',
    deleted         BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-未删除 非0-写入主键ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code, deleted),
    KEY idx_module_sort (module, sort)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '权限表';

-- ---------------------------------------------------------------------
-- 4. 用户-角色关联表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
    role_id     BIGINT UNSIGNED NOT NULL                COMMENT '角色ID',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联表';

-- ---------------------------------------------------------------------
-- 5. 角色-权限关联表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id       BIGINT UNSIGNED NOT NULL                COMMENT '角色ID',
    permission_id BIGINT UNSIGNED NOT NULL                COMMENT '权限ID',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限关联表';

-- ---------------------------------------------------------------------
-- 6. 商品分类表（两级树，parent_id 自关联）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS category;
CREATE TABLE category (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    parent_id   BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '父分类ID, 0表示一级分类',
    name        VARCHAR(32)     NOT NULL                COMMENT '分类名称',
    level       TINYINT         NOT NULL DEFAULT 1      COMMENT '层级: 1-一级 2-二级',
    sort        INT             NOT NULL DEFAULT 0      COMMENT '排序值, 越小越靠前',
    icon        VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '分类图标',
    status      TINYINT         NOT NULL DEFAULT 1      COMMENT '状态: 0-停用 1-启用',
    deleted     BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-未删除 非0-写入主键ID',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '创建人ID',
    update_by   BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_parent_name (parent_id, name, deleted),
    KEY idx_parent_sort (parent_id, sort)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品分类表';

-- ---------------------------------------------------------------------
-- 7. 商品表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    category_id    BIGINT UNSIGNED NOT NULL                COMMENT '所属分类ID',
    name           VARCHAR(128)    NOT NULL                COMMENT '商品名称',
    sku            VARCHAR(64)     NOT NULL                COMMENT '商品编码, 全局唯一',
    subtitle       VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '副标题/卖点',
    main_image     VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '主图地址',
    detail         TEXT                                    COMMENT '商品详情大字段',
    price          DECIMAL(12, 2)  NOT NULL DEFAULT 0.00   COMMENT '销售价',
    original_price DECIMAL(12, 2)  NOT NULL DEFAULT 0.00   COMMENT '原价/划线价',
    status         TINYINT         NOT NULL DEFAULT 0      COMMENT '状态: 0-下架 1-上架',
    sales          INT             NOT NULL DEFAULT 0      COMMENT '累计销量(冗余, 支付成功后累加)',
    sort           INT             NOT NULL DEFAULT 0      COMMENT '排序值',
    deleted        BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-未删除 非0-写入主键ID',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '创建人ID',
    update_by      BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku (sku, deleted),
    KEY idx_category_status_sort (category_id, status, sort),
    KEY idx_status_create_time (status, create_time),
    KEY idx_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品表';

-- ---------------------------------------------------------------------
-- 8. 库存表（与商品一对一）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS inventory;
CREATE TABLE inventory (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    product_id      BIGINT UNSIGNED NOT NULL                COMMENT '商品ID(唯一)',
    total_stock     INT             NOT NULL DEFAULT 0      COMMENT '总库存 = 可用 + 锁定',
    available_stock INT             NOT NULL DEFAULT 0      COMMENT '可用库存',
    locked_stock    INT             NOT NULL DEFAULT 0      COMMENT '锁定库存(待支付订单占用)',
    warn_stock      INT             NOT NULL DEFAULT 0      COMMENT '库存预警阈值',
    version         INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_id (product_id),
    KEY idx_available_stock (available_stock),
    CONSTRAINT chk_available_stock CHECK (available_stock >= 0),
    CONSTRAINT chk_locked_stock CHECK (locked_stock >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品库存表';
-- ---------------------------------------------------------------------
-- 9. 库存流水表（只增不改，用于审计与对账）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS inventory_log;
CREATE TABLE inventory_log (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    product_id       BIGINT UNSIGNED NOT NULL                COMMENT '商品ID',
    order_no         VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '关联订单号, 非订单操作可为空串',
    biz_type         TINYINT         NOT NULL                COMMENT '业务类型: 1-入库 2-锁定 3-扣减 4-释放 5-盘点调整',
    quantity         INT             NOT NULL                COMMENT '变更数量, 正数增加负数减少',
    before_available INT             NOT NULL                COMMENT '变更前可用库存',
    after_available  INT             NOT NULL                COMMENT '变更后可用库存',
    operator_id      BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '操作人ID, 0表示系统',
    remark           VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '备注',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_product_create_time (product_id, create_time),
    KEY idx_order_no (order_no),
    KEY idx_biz_type_create_time (biz_type, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存流水表';

-- ---------------------------------------------------------------------
-- 10. 订单主表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    order_no         VARCHAR(32)     NOT NULL                COMMENT '业务订单号(对外暴露)',
    user_id          BIGINT UNSIGNED NOT NULL                COMMENT '下单用户ID',
    total_amount     DECIMAL(12, 2)  NOT NULL                COMMENT '商品总额',
    freight_amount   DECIMAL(10, 2)  NOT NULL DEFAULT 0.00   COMMENT '运费',
    pay_amount       DECIMAL(12, 2)  NOT NULL                COMMENT '应付金额 = 商品总额 + 运费',
    status           TINYINT         NOT NULL DEFAULT 0      COMMENT '状态: 0-待支付 1-已支付 2-配送中 3-已完成 4-已取消',
    receiver_name    VARCHAR(32)     NOT NULL                COMMENT '收货人姓名(快照)',
    receiver_phone   VARCHAR(20)     NOT NULL                COMMENT '收货人电话(快照)',
    receiver_address VARCHAR(255)    NOT NULL                COMMENT '收货地址(快照)',
    remark           VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '订单备注',
    request_no       VARCHAR(64)              DEFAULT NULL   COMMENT '客户端幂等号, 防重复提交',
    expire_time      DATETIME        NOT NULL                COMMENT '支付截止时间',
    pay_time         DATETIME                 DEFAULT NULL   COMMENT '支付时间',
    deliver_time     DATETIME                 DEFAULT NULL   COMMENT '发货时间',
    finish_time      DATETIME                 DEFAULT NULL   COMMENT '完成时间',
    cancel_time      DATETIME                 DEFAULT NULL   COMMENT '取消时间',
    cancel_reason    VARCHAR(128)    NOT NULL DEFAULT ''     COMMENT '取消原因',
    deleted          BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-未删除 非0-写入主键ID',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '创建人ID',
    update_by        BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no, deleted),
    UNIQUE KEY uk_user_request_no (user_id, request_no),
    KEY idx_user_status_create_time (user_id, status, create_time),
    KEY idx_status_expire_time (status, expire_time),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单主表';

-- ---------------------------------------------------------------------
-- 11. 订单明细表（商品快照，只增不改）
-- 订单展示不依赖商品表当前数据，商品改价/改名/下架都不影响历史订单
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    order_id      BIGINT UNSIGNED NOT NULL                COMMENT '订单ID',
    order_no      VARCHAR(32)     NOT NULL                COMMENT '订单号(冗余, 便于按单号查询)',
    product_id    BIGINT UNSIGNED NOT NULL                COMMENT '商品ID',
    sku           VARCHAR(64)     NOT NULL                COMMENT '商品SKU(快照)',
    product_name  VARCHAR(128)    NOT NULL                COMMENT '商品名称(快照)',
    product_image VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '商品主图(快照)',
    price         DECIMAL(12, 2)  NOT NULL                COMMENT '成交单价(快照)',
    quantity      INT             NOT NULL                COMMENT '购买数量',
    subtotal      DECIMAL(12, 2)  NOT NULL                COMMENT '小计 = 单价 * 数量',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_product_id (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';

-- ---------------------------------------------------------------------
-- 12. 订单状态流转日志表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS order_status_log;
CREATE TABLE order_status_log (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id      BIGINT UNSIGNED NOT NULL                COMMENT '订单ID',
    order_no      VARCHAR(32)     NOT NULL                COMMENT '订单号',
    from_status   TINYINT                  DEFAULT NULL   COMMENT '变更前状态, NULL表示订单创建',
    to_status     TINYINT         NOT NULL                COMMENT '变更后状态',
    operator_id   BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '操作人ID, 0表示系统自动',
    operator_type TINYINT         NOT NULL DEFAULT 3      COMMENT '操作人类型: 1-用户 2-管理员 3-系统',
    remark        VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '变更说明',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order_no_create_time (order_no, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单状态流转日志表';

-- ---------------------------------------------------------------------
-- 13. 后台操作日志表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '操作人ID',
    username       VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '操作人用户名(快照)',
    module         VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '业务模块',
    operation      VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '操作描述',
    request_uri    VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '请求路径',
    request_method VARCHAR(10)     NOT NULL DEFAULT ''     COMMENT '请求方法',
    request_param  TEXT                                    COMMENT '请求参数(脱敏后)',
    ip             VARCHAR(45)     NOT NULL DEFAULT ''     COMMENT '客户端IP',
    duration_ms    BIGINT          NOT NULL DEFAULT 0      COMMENT '耗时(毫秒)',
    success        TINYINT         NOT NULL DEFAULT 1      COMMENT '是否成功: 0-失败 1-成功',
    error_msg      VARCHAR(512)    NOT NULL DEFAULT ''     COMMENT '失败原因',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_create_time (user_id, create_time),
    KEY idx_module_create_time (module, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '后台操作日志表';