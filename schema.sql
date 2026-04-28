-- Expense Tracking System Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS fintrac;
USE fintrac;

-- Users Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Categories Table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    type ENUM('INCOME', 'EXPENSE') NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(20),
    user_id BIGINT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Transactions Table
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    type ENUM('INCOME', 'EXPENSE') NOT NULL,
    category_id BIGINT,
    payment_type ENUM('MERCHANT', 'PERSONAL') NOT NULL DEFAULT 'PERSONAL',
    merchant_name VARCHAR(100),
    date DATE NOT NULL,
    description TEXT,
    user_id BIGINT NOT NULL,
    is_emergency BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_date (date),
    INDEX idx_category_id (category_id),
    INDEX idx_type (type),
    INDEX idx_user_date (user_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Budgets Table
CREATE TABLE budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    month VARCHAR(7) NOT NULL,
    initial_budget DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    adjusted_budget DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    emergency_spent DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_spent DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_month (user_id, month),
    INDEX idx_user_id (user_id),
    INDEX idx_month (month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Alerts Table
CREATE TABLE alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type ENUM('WARNING', 'CRITICAL', 'INFO') NOT NULL,
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert Default Categories
INSERT INTO categories (name, type, icon, color, is_default) VALUES
('Salary', 'INCOME', 'briefcase', '#10B981', TRUE),
('Freelance', 'INCOME', 'laptop', '#3B82F6', TRUE),
('Investments', 'INCOME', 'trending-up', '#8B5CF6', TRUE),
('Other Income', 'INCOME', 'plus-circle', '#6B7280', TRUE),
('Food & Dining', 'EXPENSE', 'utensils', '#EF4444', TRUE),
('Transportation', 'EXPENSE', 'car', '#F59E0B', TRUE),
('Shopping', 'EXPENSE', 'shopping-bag', '#EC4899', TRUE),
('Entertainment', 'EXPENSE', 'film', '#8B5CF6', TRUE),
('Bills & Utilities', 'EXPENSE', 'zap', '#6366F1', TRUE),
('Healthcare', 'EXPENSE', 'heart', '#10B981', TRUE),
('Education', 'EXPENSE', 'book', '#3B82F6', TRUE),
('Travel', 'EXPENSE', 'plane', '#14B8A6', TRUE),
('Emergency', 'EXPENSE', 'alert-triangle', '#DC2626', TRUE),
('Personal', 'EXPENSE', 'user', '#6B7280', TRUE),
('Other Expense', 'EXPENSE', 'more-horizontal', '#9CA3AF', TRUE);
