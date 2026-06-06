-- ============================================================
-- V1_19: Create Student Achievement + Alumni Record tables
-- ============================================================

CREATE TABLE IF NOT EXISTS student_achievement (
    achievement_id     BIGSERIAL    PRIMARY KEY,
    student_id         BIGINT       NOT NULL,
    category           VARCHAR(32)  NOT NULL,
    title              VARCHAR(200) NOT NULL,
    description        TEXT,
    achievement_date   DATE,
    location           VARCHAR(200),
    awarded_by         VARCHAR(200),
    icon               VARCHAR(50),
    organization_id    BIGINT       NOT NULL,
    created_by         VARCHAR(50),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(50),
    last_modified_date TIMESTAMP,
    version            BIGINT       DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_student_achievement_org      ON student_achievement(organization_id);
CREATE INDEX IF NOT EXISTS idx_student_achievement_student  ON student_achievement(student_id);
CREATE INDEX IF NOT EXISTS idx_student_achievement_category ON student_achievement(category);


CREATE TABLE IF NOT EXISTS alumni_record (
    alumni_id          BIGSERIAL    PRIMARY KEY,
    student_id         BIGINT,
    full_name          VARCHAR(200) NOT NULL,
    batch_year         VARCHAR(10),
    year_passed        VARCHAR(10),
    course             VARCHAR(100),
    occupation         VARCHAR(100),
    employer           VARCHAR(200),
    contact            VARCHAR(30),
    email              VARCHAR(100),
    city               VARCHAR(100),
    graduation_date    DATE,
    linked_in          VARCHAR(255),
    organization_id    BIGINT       NOT NULL,
    created_by         VARCHAR(50),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(50),
    last_modified_date TIMESTAMP,
    version            BIGINT       DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_alumni_org     ON alumni_record(organization_id);
CREATE INDEX IF NOT EXISTS idx_alumni_student ON alumni_record(student_id);
CREATE INDEX IF NOT EXISTS idx_alumni_batch   ON alumni_record(batch_year);
