CREATE DATABASE IF NOT EXISTS authentication_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE authentication_db;

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS roles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(30) NOT NULL UNIQUE,
    description     VARCHAR(255) NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    assigned_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT INTO roles (name, description) VALUES
 ('STUDENT', 'Student laboratorije'),
 ('ENGINEER', 'Laboratorijski inzenjer'),
 ('ADMIN', 'Administrator sistema');
