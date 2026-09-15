CREATE DATABASE IF NOT EXISTS lab_inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lab_inventory_db;

CREATE TABLE IF NOT EXISTS components (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_code      VARCHAR(100) NOT NULL UNIQUE,
    manufacturer    VARCHAR(100) NOT NULL,
    component_type  VARCHAR(30) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS board_specifications (
    component_id        BIGINT PRIMARY KEY,
    cpu_architecture    VARCHAR(50) NOT NULL,
    clock_speed_mhz     INT NOT NULL,
    flash_memory_kb     INT NOT NULL,
    sram_kb             INT NOT NULL,
    operating_voltage   DECIMAL(3,1) NOT NULL,
    digital_pins_count  INT NOT NULL DEFAULT 0,
    analog_pins_count   INT NOT NULL DEFAULT 0,
    timer_count         INT NOT NULL DEFAULT 0,
    pwm_channels_count  INT NOT NULL DEFAULT 0,
    spi_count           INT NOT NULL DEFAULT 0,
    i2c_count           INT NOT NULL DEFAULT 0,
    uart_count          INT NOT NULL DEFAULT 0,
    has_wifi            BOOLEAN NOT NULL DEFAULT FALSE,
    has_bluetooth       BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_mcu_spec_component FOREIGN KEY (component_id) REFERENCES components(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inventory_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_tag            VARCHAR(50) NOT NULL UNIQUE,
    component_id        BIGINT NOT NULL,
    firmware_version    VARCHAR(30) DEFAULT 'v1.0.0',
    storage_location    VARCHAR(100) NOT NULL,
    operational_status  VARCHAR(30) NOT NULL DEFAULT 'READY_FOR_USE',
    condition_rating    INT NOT NULL DEFAULT 5,
    purchase_date       DATE NULL,
    last_inspected_at   DATETIME NULL,
    CONSTRAINT fk_item_component FOREIGN KEY (component_id) REFERENCES components(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS equipment_issues (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_item_id   BIGINT NOT NULL,
    reported_by_user_id BIGINT NOT NULL,
    issue_category      VARCHAR(30) NOT NULL,
    description         TEXT NOT NULL,
    resolution_notes    TEXT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at         DATETIME NULL,
    CONSTRAINT fk_issue_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id)
) ENGINE=InnoDB;

CREATE INDEX idx_item_component ON inventory_items(component_id);
CREATE INDEX idx_item_status ON inventory_items(operational_status);
CREATE INDEX idx_issue_item ON equipment_issues(inventory_item_id);
CREATE INDEX idx_issue_status ON equipment_issues(status);

INSERT INTO components (model_code, manufacturer, component_type) VALUES
 ('ESP32-WROOM-32', 'Espressif', 'MICROCONTROLLER'),
 ('Arduino Uno R3', 'Arduino', 'MICROCONTROLLER'),
 ('STM32F103C8T6', 'STMicroelectronics', 'MICROCONTROLLER');

INSERT INTO board_specifications
 (component_id, cpu_architecture, clock_speed_mhz, flash_memory_kb, sram_kb, operating_voltage,
  digital_pins_count, analog_pins_count, timer_count, pwm_channels_count, spi_count, i2c_count, uart_count,
  has_wifi, has_bluetooth)
VALUES
 (1, 'Xtensa LX6', 240, 4096, 520, 3.3, 34, 18, 4, 16, 4, 2, 3, TRUE, TRUE),
 (2, 'AVR', 16, 32, 2, 5.0, 14, 6, 3, 6, 1, 1, 1, FALSE, FALSE),
 (3, 'ARM Cortex-M3', 72, 64, 20, 3.3, 37, 10, 4, 4, 2, 2, 3, FALSE, FALSE);

INSERT INTO inventory_items (item_tag, component_id, storage_location, operational_status, condition_rating, purchase_date) VALUES
 ('ESP32-0001', 1, 'Lab 3, orman A', 'READY_FOR_USE', 5, '2024-09-01'),
 ('ESP32-0002', 1, 'Lab 3, orman A', 'READY_FOR_USE', 4, '2024-09-01'),
 ('UNO-0001', 2, 'Lab 3, orman B', 'PENDING_DIAGNOSTICS', 2, '2023-02-15');
