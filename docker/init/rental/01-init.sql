CREATE DATABASE IF NOT EXISTS lab_rental_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lab_rental_db;

CREATE TABLE IF NOT EXISTS lab_members (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_user_id    BIGINT NOT NULL UNIQUE,
    first_name          VARCHAR(50) NOT NULL,
    last_name           VARCHAR(50) NOT NULL,
    member_code         VARCHAR(30) NOT NULL UNIQUE,
    member_type         VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    membership_status   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    blocked_until       DATETIME NULL,
    block_reason        VARCHAR(255) NULL,
    penalty_points      INT NOT NULL DEFAULT 0,
    joined_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS member_blocks (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id           BIGINT NOT NULL,
    reason              VARCHAR(255) NOT NULL,
    blocked_from        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    blocked_until       DATETIME NULL,
    lifted_at           DATETIME NULL,
    created_by_user_id  BIGINT NULL,
    CONSTRAINT fk_block_member FOREIGN KEY (member_id) REFERENCES lab_members(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservation_requests (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id               BIGINT NOT NULL,
    requested_component_id  BIGINT NOT NULL,
    requested_start         DATETIME NOT NULL,
    requested_end           DATETIME NOT NULL,
    quantity                INT NOT NULL DEFAULT 1,
    purpose                 TEXT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason        VARCHAR(255) NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_member FOREIGN KEY (member_id) REFERENCES lab_members(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservations (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id              BIGINT NOT NULL,
    member_id               BIGINT NOT NULL,
    inventory_item_id       BIGINT NOT NULL,
    reservation_start       DATETIME NOT NULL,
    reservation_end         DATETIME NOT NULL,
    pickup_deadline         DATETIME NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    approved_by_user_id     BIGINT NULL,
    approved_at             DATETIME NULL,
    rejected_by_user_id     BIGINT NULL,
    rejected_at             DATETIME NULL,
    rejection_reason        VARCHAR(255) NULL,
    cancelled_at            DATETIME NULL,
    cancellation_reason     VARCHAR(255) NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_request FOREIGN KEY (request_id) REFERENCES reservation_requests(id),
    CONSTRAINT fk_reservation_member FOREIGN KEY (member_id) REFERENCES lab_members(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservation_events (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id      BIGINT NOT NULL,
    event_type          VARCHAR(30) NOT NULL,
    actor_user_id       BIGINT NULL,
    message             VARCHAR(500) NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS borrow_records (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id      BIGINT NOT NULL UNIQUE,
    borrowed_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date            DATETIME NOT NULL,
    returned_at         DATETIME NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    issued_by_user_id   BIGINT NULL,
    received_by_user_id BIGINT NULL,
    condition_before    TEXT NULL,
    condition_after     TEXT NULL,
    return_notes        TEXT NULL,
    penalty_amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_borrow_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS penalties (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id           BIGINT NOT NULL,
    borrow_record_id    BIGINT NOT NULL,
    amount              DECIMAL(10,2) NOT NULL,
    reason              VARCHAR(255) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    issued_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at             DATETIME NULL,
    waived_at           DATETIME NULL,
    waived_by_user_id   BIGINT NULL,
    CONSTRAINT fk_penalty_member FOREIGN KEY (member_id) REFERENCES lab_members(id),
    CONSTRAINT fk_penalty_borrow FOREIGN KEY (borrow_record_id) REFERENCES borrow_records(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id           BIGINT NOT NULL,
    type                VARCHAR(40) NOT NULL,
    message             VARCHAR(500) NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES lab_members(id)
) ENGINE=InnoDB;

CREATE INDEX idx_blocks_member_active ON member_blocks(member_id, lifted_at);
CREATE INDEX idx_requests_member_status ON reservation_requests(member_id, status);
CREATE INDEX idx_requests_component_period ON reservation_requests(requested_component_id, requested_start, requested_end);
CREATE INDEX idx_reservations_item_period ON reservations(inventory_item_id, reservation_start, reservation_end);
CREATE INDEX idx_reservations_member_status ON reservations(member_id, status);
CREATE INDEX idx_events_reservation ON reservation_events(reservation_id, created_at);
CREATE INDEX idx_borrow_status_due ON borrow_records(status, due_date);
CREATE INDEX idx_penalties_member_status ON penalties(member_id, status);
CREATE INDEX idx_notifications_member ON notifications(member_id, created_at);
