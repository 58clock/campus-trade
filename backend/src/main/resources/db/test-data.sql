-- 测试数据: 用户、商品、订单

USE campus_trade;

-- 普通用户
INSERT INTO `user` (`username`, `password`, `email`, `phone`, `nickname`, `school`, `role`, `status`) VALUES
('zhangsan', '$2a$10$PvX4ay.e7/3M2bsVcLIRe.c3E66pEdAnOnhrRPR8ojErrVhiSmf5O', 'zhangsan@qq.com', '13800000001', '张三', '北京大学', 'USER', 1),
('lisi',     '$2a$10$PvX4ay.e7/3M2bsVcLIRe.c3E66pEdAnOnhrRPR8ojErrVhiSmf5O', 'lisi@qq.com',     '13800000002', '李四', '清华大学', 'USER', 1),
('wangwu',   '$2a$10$PvX4ay.e7/3M2bsVcLIRe.c3E66pEdAnOnhrRPR8ojErrVhiSmf5O', 'wangwu@qq.com',   '13800000003', '王五', '浙江大学', 'USER', 0);

-- 商品（seller_id 对应上面的用户 ID）
INSERT INTO `product` (`user_id`, `title`, `description`, `category`, `price`, `original_price`, `condition_level`, `images`, `status`, `view_count`, `created_at`) VALUES
(2, 'iPhone 15 Pro 256G 国行', '9成新，无划痕，带原装配件', 'ELECTRONICS', 6800.00, 8999.00, 'LIKE_NEW', '["/uploads/products/1.jpg","/uploads/products/2.jpg"]', 'ON_SALE', 156, '2026-06-20 10:30:00'),
(2, '高等数学（第七版）同济大学', '只用了一学期，几乎全新', 'BOOK', 25.00, 56.00, 'GOOD', '["/uploads/products/3.jpg"]', 'ON_SALE', 89, '2026-06-22 14:00:00'),
(3, '考研英语词汇红宝书', '背了前10页，基本全新', 'BOOK', 19.90, 45.00, 'LIKE_NEW', '["/uploads/products/4.jpg"]', 'ON_SALE', 42, '2026-06-23 09:00:00'),
(3, '雷蛇游戏鼠标 DeathAdder V3', '用了半年，功能正常', 'ELECTRONICS', 199.00, 499.00, 'FAIR', '["/uploads/products/5.jpg","/uploads/products/6.jpg"]', 'ON_SALE', 210, '2026-06-24 16:00:00'),
(3, '折叠桌 宿舍用', '毕业甩卖，桌面有轻微划痕', 'LIFESTYLE', 30.00, 99.00, 'FAIR', '["/uploads/products/7.jpg"]', 'OFF_SHELF', 55, '2026-06-25 11:00:00'),
(3, 'Java编程思想（第四版）', '经典书籍，适合Java入门', 'BOOK', 35.00, 79.00, 'GOOD', '[]', 'DELETED', 12, '2026-06-21 08:00:00'),
(4, 'iPad Air 5 64G WiFi版', '买来吃灰了，电池100%', 'ELECTRONICS', 3200.00, 4799.00, 'LIKE_NEW', '["/uploads/products/8.jpg","/uploads/products/9.jpg"]', 'ON_SALE', 320, '2026-06-26 13:00:00'),
(4, '线性代数 同济第六版', '考研必备', 'BOOK', 15.00, 38.00, 'GOOD', '["/uploads/products/10.jpg"]', 'ON_SALE', 28, '2026-06-26 15:00:00'),
(2, 'Nike篮球鞋 42码', '穿了一个月，几乎全新', 'SPORTS', 299.00, 799.00, 'GOOD', '["/uploads/products/11.jpg"]', 'ON_SALE', 178, '2026-06-27 10:00:00'),
(4, '台灯 LED护眼', '宿舍神器，带USB充电口', 'LIFESTYLE', 45.00, 129.00, 'GOOD', '["/uploads/products/12.jpg"]', 'ON_SALE', 67, '2026-06-27 12:00:00');

-- 订单（product_id 对应上面的商品，buyer_id/seller_id 对应用户）
INSERT INTO `order` (`buyer_id`, `seller_id`, `product_id`, `amount`, `status`, `paid_at`, `shipped_at`, `received_at`, `completed_at`, `created_at`) VALUES
(4, 2, 1, 6800.00, 'COMPLETED',        '2026-06-21 10:30:00', '2026-06-21 15:00:00', '2026-06-24 18:00:00', '2026-06-24 18:00:00', '2026-06-21 10:00:00'),
(4, 2, 2, 25.00,   'COMPLETED',        '2026-06-23 10:00:00', '2026-06-23 14:00:00', '2026-06-25 20:00:00', '2026-06-25 20:00:00', '2026-06-23 09:00:00'),
(2, 3, 3, 19.90,   'COMPLETED',        '2026-06-24 08:00:00', '2026-06-24 12:00:00', '2026-06-26 16:00:00', '2026-06-26 16:00:00', '2026-06-24 07:00:00'),
(2, 3, 4, 199.00,  'PENDING_RECEIPT',  '2026-06-25 09:00:00', '2026-06-25 16:00:00', NULL,                NULL,                '2026-06-25 08:00:00'),
(3, 2, 2, 25.00,   'CANCELLED',        NULL,                  NULL,                  NULL,                NULL,                '2026-06-26 10:00:00'),
(4, 3, 4, 199.00,  'PENDING_SHIPMENT', '2026-06-27 08:00:00', NULL,                  NULL,                NULL,                '2026-06-27 07:00:00'),
(2, 3, 3, 19.90,   'PENDING_PAYMENT',  NULL,                  NULL,                  NULL,                NULL,                '2026-06-27 14:00:00');
