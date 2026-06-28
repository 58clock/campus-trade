-- ============================================
-- 校园二手交易平台 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS campus_trade
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_trade;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    `username`      VARCHAR(50)     NOT NULL                 COMMENT '用户名',
    `password`      VARCHAR(255)    NOT NULL                 COMMENT '密码(BCrypt)',
    `email`         VARCHAR(100)    DEFAULT NULL             COMMENT '邮箱',
    `phone`         VARCHAR(20)     DEFAULT NULL             COMMENT '手机号',
    `nickname`      VARCHAR(50)     DEFAULT NULL             COMMENT '昵称',
    `school`        VARCHAR(100)    DEFAULT NULL             COMMENT '学校',
    `avatar`        VARCHAR(255)    DEFAULT NULL             COMMENT '头像URL',
    `role`          VARCHAR(20)     NOT NULL DEFAULT 'USER'  COMMENT '角色: USER/ADMIN',
    `status`        TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 1正常 0封禁',
    `login_fail_count` INT          NOT NULL DEFAULT 0       COMMENT '登录失败次数',
    `locked_until`  DATETIME        DEFAULT NULL             COMMENT '账号冻结截止时间',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '商品ID',
    `user_id`         BIGINT          NOT NULL                 COMMENT '发布者ID',
    `title`           VARCHAR(200)    NOT NULL                 COMMENT '标题',
    `description`     TEXT            DEFAULT NULL             COMMENT '描述',
    `category`        VARCHAR(50)     NOT NULL                 COMMENT '分类: BOOK/ELECTRONICS/LIFESTYLE/SPORTS/OTHER',
    `price`           DECIMAL(10,2)   NOT NULL                 COMMENT '售价',
    `original_price`  DECIMAL(10,2)   DEFAULT NULL             COMMENT '原价',
    `condition_level` VARCHAR(20)     NOT NULL                 COMMENT '成色: NEW/LIKE_NEW/GOOD/FAIR',
    `images`          TEXT            DEFAULT NULL             COMMENT '图片URL列表(JSON数组)',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'ON_SALE' COMMENT '状态: ON_SALE/OFF_SHELF/DELETED',
    `view_count`      INT             NOT NULL DEFAULT 0       COMMENT '浏览量',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_price` (`price`),
    KEY `idx_created_at` (`created_at`),
    FULLTEXT KEY `ft_title_desc` (`title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '订单ID',
    `buyer_id`        BIGINT          NOT NULL                 COMMENT '买家ID',
    `seller_id`       BIGINT          NOT NULL                 COMMENT '卖家ID',
    `product_id`      BIGINT          NOT NULL                 COMMENT '商品ID',
    `amount`          DECIMAL(10,2)   NOT NULL                 COMMENT '成交价',
    `status`          VARCHAR(30)     NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '状态: PENDING_PAYMENT/PENDING_SHIPMENT/PENDING_RECEIPT/COMPLETED/CANCELLED',
    `paid_at`         DATETIME        DEFAULT NULL             COMMENT '付款时间',
    `shipped_at`      DATETIME        DEFAULT NULL             COMMENT '发货时间',
    `received_at`     DATETIME        DEFAULT NULL             COMMENT '收货时间',
    `completed_at`    DATETIME        DEFAULT NULL             COMMENT '完成时间',
    `cancelled_at`    DATETIME        DEFAULT NULL             COMMENT '取消时间',
    `cancel_reason`   VARCHAR(200)    DEFAULT NULL             COMMENT '取消原因',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 留言表
CREATE TABLE IF NOT EXISTS `message` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '留言ID',
    `product_id`      BIGINT          NOT NULL                 COMMENT '商品ID',
    `sender_id`       BIGINT          NOT NULL                 COMMENT '发送者ID',
    `receiver_id`     BIGINT          DEFAULT NULL             COMMENT '接收者ID',
    `parent_id`       BIGINT          DEFAULT NULL             COMMENT '父留言ID(嵌套回复)',
    `content`         TEXT            NOT NULL                 COMMENT '内容',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='留言表';

-- 评价表
CREATE TABLE IF NOT EXISTS `review` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '评价ID',
    `order_id`        BIGINT          NOT NULL                 COMMENT '订单ID',
    `reviewer_id`     BIGINT          NOT NULL                 COMMENT '评价者ID',
    `target_id`       BIGINT          NOT NULL                 COMMENT '被评价者ID',
    `rating`          TINYINT         NOT NULL                 COMMENT '评分(1-5)',
    `content`         TEXT            DEFAULT NULL             COMMENT '评价内容',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'SHOW'  COMMENT '状态: SHOW/HIDDEN',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_target_id` (`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

-- 举报表
CREATE TABLE IF NOT EXISTS `report` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '举报ID',
    `reporter_id`     BIGINT          NOT NULL                 COMMENT '举报者ID',
    `target_type`     VARCHAR(20)     NOT NULL                 COMMENT '举报类型: PRODUCT/MESSAGE/USER',
    `target_id`       BIGINT          NOT NULL                 COMMENT '举报目标ID',
    `reason`          TEXT            NOT NULL                 COMMENT '举报原因',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RESOLVED/DISMISSED',
    `handler_note`    TEXT            DEFAULT NULL             COMMENT '处理备注',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- 浏览历史表
CREATE TABLE IF NOT EXISTS `browse_history` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '记录ID',
    `user_id`         BIGINT          NOT NULL                 COMMENT '用户ID',
    `product_id`      BIGINT          NOT NULL                 COMMENT '商品ID',
    `category`        VARCHAR(50)     NOT NULL                 COMMENT '商品分类',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category` (`category`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览历史表';

-- 初始管理员账号: admin / admin123
INSERT INTO `user` (`username`, `password`, `role`, `nickname`) VALUES
('admin', '$2a$10$PvX4ay.e7/3M2bsVcLIRe.c3E66pEdAnOnhrRPR8ojErrVhiSmf5O', 'ADMIN', '系统管理员');
