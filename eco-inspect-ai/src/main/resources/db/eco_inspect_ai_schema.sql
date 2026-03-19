-- =============================================================
--  ECO-INSPECT AI — MySQL Database Schema
--  Version : 1.0
--  Created : 2026-03-08
--  Description: Full schema for the Eco-Inspect AI environmental
--               management & citizen reporting platform.
-- =============================================================

CREATE DATABASE IF NOT EXISTS eco_inspect_ai
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE eco_inspect_ai;

-- =============================================================
-- MODULE 1: USERS & ROLES
-- =============================================================

CREATE TABLE users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(100)  NOT NULL,
    phone_number   VARCHAR(20)   UNIQUE,                        -- WhatsApp identifier
    email          VARCHAR(100)  UNIQUE,
    password_hash  VARCHAR(255),                                -- NULL for WhatsApp-only users
    role           ENUM('citizen','officer','admin') NOT NULL DEFAULT 'citizen',
    profile_photo  VARCHAR(500),
    is_active      BOOLEAN       DEFAULT TRUE,
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =============================================================
-- MODULE 2: REGIONS
-- =============================================================

CREATE TABLE regions (
    region_id    INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    description  TEXT,
    boundary_geojson TEXT,                                      -- Optional GeoJSON polygon boundary
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================
-- MODULE 3: VIOLATION CATEGORIES
-- =============================================================

CREATE TABLE violation_categories (
    category_id   INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    icon_url      VARCHAR(255),
    severity_default ENUM('low','medium','high','critical') DEFAULT 'medium',
    is_active     BOOLEAN DEFAULT TRUE
);

-- Seed data for violation categories
INSERT INTO violation_categories (name, description, severity_default) VALUES
('Illegal Waste Disposal',   'Unauthorized dumping of solid or liquid waste',               'high'),
('Water Pollution',          'Contamination of rivers, lakes, or groundwater',              'critical'),
('Air Pollution',            'Illegal emissions, open burning, or toxic fumes',             'high'),
('Deforestation',            'Unauthorized clearing of trees or forested land',             'high'),
('Noise Pollution',          'Excessive noise beyond legal limits',                         'low'),
('Chemical Spill',           'Accidental or deliberate release of hazardous chemicals',     'critical'),
('Soil Contamination',       'Pollution of land with hazardous or toxic substances',        'high'),
('Wildlife Harm',            'Illegal poaching, trapping, or habitat destruction',          'high'),
('Sewage Discharge',         'Untreated sewage released into the environment',              'critical'),
('Other',                    'Environmental violation not covered by existing categories',  'medium');

-- =============================================================
-- MODULE 4: REPORTS (CITIZEN SUBMISSIONS)
-- =============================================================

CREATE TABLE reports (
    report_id          INT AUTO_INCREMENT PRIMARY KEY,
    reporter_id        INT           NOT NULL,
    channel            ENUM('whatsapp','app','web') DEFAULT 'whatsapp',
    raw_message        TEXT,                                    -- Original message from citizen
    latitude           DECIMAL(10, 8),
    longitude          DECIMAL(11, 8),
    address_text       VARCHAR(255),                           -- Human-readable address
    region_id          INT,
    status             ENUM(
                           'received',
                           'ai_processing',
                           'pending_review',
                           'assigned',
                           'in_progress',
                           'resolved',
                           'rejected'
                       )             NOT NULL DEFAULT 'received',
    submitted_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (reporter_id) REFERENCES users(user_id),
    FOREIGN KEY (region_id)   REFERENCES regions(region_id)
);

-- =============================================================
-- MODULE 5: REPORT MEDIA
-- =============================================================

CREATE TABLE report_media (
    media_id      INT AUTO_INCREMENT PRIMARY KEY,
    report_id     INT          NOT NULL,
    media_type    ENUM('image','video','audio','document') NOT NULL,
    file_url      VARCHAR(500) NOT NULL,                        -- Cloud storage URL (S3, GCS, etc.)
    file_name     VARCHAR(255),
    file_size_kb  INT,
    mime_type     VARCHAR(100),
    uploaded_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (report_id) REFERENCES reports(report_id) ON DELETE CASCADE
);

-- =============================================================
-- MODULE 6: AI ANALYSIS (AIRIA AGENT OUTPUT)
-- =============================================================

CREATE TABLE ai_analysis (
    analysis_id        INT AUTO_INCREMENT PRIMARY KEY,
    report_id          INT           NOT NULL UNIQUE,
    summary            TEXT,                                    -- AI-generated concise summary
    category_id        INT,                                     -- AI-assigned violation category
    urgency_level      ENUM('low','medium','high','critical'),
    confidence_score   DECIMAL(5, 2),                          -- 0.00 to 100.00
    keywords           JSON,                                    -- Extracted keyword array
    sentiment          ENUM('neutral','concerned','alarmed'),
    media_analysis     TEXT,                                    -- AI description of attached media
    location_verified  BOOLEAN DEFAULT FALSE,                   -- Was geo-tag verified by AI?
    raw_ai_response    JSON,                                    -- Full API response for debugging
    model_version      VARCHAR(50),                            -- Airia agent version used
    processing_time_ms INT,                                    -- Processing duration in ms
    processed_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (report_id)   REFERENCES reports(report_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES violation_categories(category_id)
);

-- =============================================================
-- MODULE 7: CASES (OFFICER WORKFLOW)
-- =============================================================

CREATE TABLE cases (
    case_id          INT AUTO_INCREMENT PRIMARY KEY,
    case_reference   VARCHAR(30) UNIQUE NOT NULL,               -- e.g., ECO-2026-00142
    report_id        INT          NOT NULL UNIQUE,
    assigned_to      INT,                                       -- Officer user_id
    assigned_by      INT,                                       -- Admin user_id
    priority         ENUM('low','medium','high','critical') NOT NULL,
    status           ENUM(
                         'open',
                         'under_investigation',
                         'action_taken',
                         'closed',
                         'escalated'
                     )           NOT NULL DEFAULT 'open',
    due_date         DATE,
    resolution_notes TEXT,
    closed_at        TIMESTAMP,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (report_id)   REFERENCES reports(report_id),
    FOREIGN KEY (assigned_to) REFERENCES users(user_id),
    FOREIGN KEY (assigned_by) REFERENCES users(user_id)
);

-- Auto-generate case reference via trigger
DELIMITER $$
CREATE TRIGGER trg_case_reference
BEFORE INSERT ON cases
FOR EACH ROW
BEGIN
    IF NEW.case_reference IS NULL OR NEW.case_reference = '' THEN
        SET NEW.case_reference = CONCAT('ECO-', YEAR(NOW()), '-', LPAD(NEW.case_id, 5, '0'));
    END IF;
END$$
DELIMITER ;

-- =============================================================
-- MODULE 8: CASE UPDATES (ACTIVITY TIMELINE)
-- =============================================================

CREATE TABLE case_updates (
    update_id    INT AUTO_INCREMENT PRIMARY KEY,
    case_id      INT          NOT NULL,
    officer_id   INT          NOT NULL,
    note         TEXT         NOT NULL,
    update_type  ENUM(
                     'status_change',
                     'field_visit',
                     'evidence_added',
                     'escalation',
                     'resolution',
                     'comment'
                 )            NOT NULL DEFAULT 'comment',
    attachment_url VARCHAR(500),                               -- Optional evidence file
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (case_id)    REFERENCES cases(case_id) ON DELETE CASCADE,
    FOREIGN KEY (officer_id) REFERENCES users(user_id)
);

-- =============================================================
-- MODULE 9: NOTIFICATIONS
-- =============================================================

CREATE TABLE notifications (
    notification_id  INT AUTO_INCREMENT PRIMARY KEY,
    user_id          INT          NOT NULL,
    report_id        INT,
    case_id          INT,
    channel          ENUM('whatsapp','push','email','sms') NOT NULL,
    message          TEXT         NOT NULL,
    status           ENUM('pending','sent','delivered','failed') DEFAULT 'pending',
    retry_count      INT          DEFAULT 0,
    sent_at          TIMESTAMP,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)   REFERENCES users(user_id),
    FOREIGN KEY (report_id) REFERENCES reports(report_id) ON DELETE SET NULL,
    FOREIGN KEY (case_id)   REFERENCES cases(case_id) ON DELETE SET NULL
);

-- =============================================================
-- MODULE 10: AUDIT LOGS
-- =============================================================

CREATE TABLE audit_logs (
    log_id        INT AUTO_INCREMENT PRIMARY KEY,
    actor_id      INT,                                         -- User who performed action
    action        VARCHAR(100)  NOT NULL,                      -- e.g., 'CASE_ASSIGNED'
    entity_type   VARCHAR(50),                                 -- e.g., 'case', 'report'
    entity_id     INT,
    old_value     JSON,
    new_value     JSON,
    ip_address    VARCHAR(45),
    user_agent    VARCHAR(255),
    logged_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (actor_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- =============================================================
-- INDEXES FOR PERFORMANCE
-- =============================================================

-- reports
CREATE INDEX idx_reports_reporter      ON reports(reporter_id);
CREATE INDEX idx_reports_status        ON reports(status);
CREATE INDEX idx_reports_region        ON reports(region_id);
CREATE INDEX idx_reports_submitted_at  ON reports(submitted_at);
CREATE INDEX idx_reports_location      ON reports(latitude, longitude);

-- ai_analysis
CREATE INDEX idx_ai_urgency            ON ai_analysis(urgency_level);
CREATE INDEX idx_ai_confidence         ON ai_analysis(confidence_score);
CREATE INDEX idx_ai_category           ON ai_analysis(category_id);

-- cases
CREATE INDEX idx_cases_officer         ON cases(assigned_to);
CREATE INDEX idx_cases_status          ON cases(status);
CREATE INDEX idx_cases_priority        ON cases(priority);
CREATE INDEX idx_cases_due_date        ON cases(due_date);

-- notifications
CREATE INDEX idx_notif_user            ON notifications(user_id);
CREATE INDEX idx_notif_status          ON notifications(status);

-- audit_logs
CREATE INDEX idx_audit_actor           ON audit_logs(actor_id);
CREATE INDEX idx_audit_entity          ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logged_at       ON audit_logs(logged_at);

-- =============================================================
-- VIEWS FOR DASHBOARD QUERIES
-- =============================================================

-- Officer Dashboard: Active cases with AI urgency
CREATE VIEW v_active_cases AS
SELECT
    c.case_id,
    c.case_reference,
    c.priority,
    c.status,
    c.due_date,
    u.full_name           AS officer_name,
    r.address_text,
    r.latitude,
    r.longitude,
    vc.name               AS violation_category,
    ai.urgency_level,
    ai.confidence_score,
    ai.summary            AS ai_summary,
    r.submitted_at
FROM cases c
JOIN reports          r   ON c.report_id   = r.report_id
JOIN ai_analysis      ai  ON r.report_id   = ai.report_id
JOIN violation_categories vc ON ai.category_id = vc.category_id
LEFT JOIN users       u   ON c.assigned_to = u.user_id
WHERE c.status NOT IN ('closed', 'escalated');

-- Analytics: Reports summary per region
CREATE VIEW v_reports_by_region AS
SELECT
    rg.name               AS region_name,
    COUNT(r.report_id)    AS total_reports,
    SUM(CASE WHEN r.status = 'resolved' THEN 1 ELSE 0 END) AS resolved,
    SUM(CASE WHEN r.status IN ('received','ai_processing','pending_review') THEN 1 ELSE 0 END) AS pending,
    AVG(ai.confidence_score) AS avg_ai_confidence
FROM regions rg
LEFT JOIN reports      r  ON rg.region_id = r.region_id
LEFT JOIN ai_analysis ai  ON r.report_id  = ai.report_id
GROUP BY rg.region_id, rg.name;

-- =============================================================
-- END OF SCHEMA
-- =============================================================
