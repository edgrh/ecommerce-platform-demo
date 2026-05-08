CREATE TABLE IF NOT EXISTS bc_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    parent_id BIGINT DEFAULT 0
);

INSERT IGNORE INTO bc_category (id, name, parent_id) VALUES (1, 'Default', 0);

CREATE TABLE IF NOT EXISTS bc_product_spu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    subtitle VARCHAR(512),
    detail TEXT,
    merchant_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ON_SHELF',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_product_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    spu_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    spec_json VARCHAR(512),
    price_cent INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    sold INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    role VARCHAR(32) NOT NULL,
    display_name VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bc_seckill_activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    seckill_price_cent INT NOT NULL,
    total_stock INT NOT NULL,
    sold_stock INT NOT NULL DEFAULT 0,
    limit_per_user INT NOT NULL DEFAULT 1,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_trade_order_0 (
    id BIGINT NOT NULL,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    order_type VARCHAR(32) NOT NULL,
    total_cent INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    seckill_activity_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_trade_order_1 (
    id BIGINT NOT NULL,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    order_type VARCHAR(32) NOT NULL,
    total_cent INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    seckill_activity_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_trade_order_item_0 (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price_cent INT NOT NULL,
    title_snapshot VARCHAR(256),
    PRIMARY KEY (id),
    KEY idx_order_user (order_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_trade_order_item_1 (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price_cent INT NOT NULL,
    title_snapshot VARCHAR(256),
    PRIMARY KEY (id),
    KEY idx_order_user (order_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_fulfillment_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    logistics_no VARCHAR(64),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_settlement_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    amount_cent INT NOT NULL,
    fee_cent INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_idempotency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_key VARCHAR(128) NOT NULL UNIQUE,
    order_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_risk_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_risk_audit_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    risk_score INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO bc_risk_rule (id, code, name, enabled) VALUES
 (1, 'SECKILL_FREQ', 'Seckill frequency cap', 1),
 (2, 'NEW_USER', 'New account risk', 1);
