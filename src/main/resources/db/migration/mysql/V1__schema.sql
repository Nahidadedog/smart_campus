-- 智慧校园服务平台 · MySQL 8 建表脚本
-- 与 h2/V1__schema.sql 字段一一对应

CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    real_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(50)  DEFAULT NULL,
    email       VARCHAR(100) DEFAULT NULL,
    gender      VARCHAR(20)  DEFAULT NULL,
    avatar      VARCHAR(500) DEFAULT NULL,
    role        VARCHAR(20)  NOT NULL,
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL,
    update_time DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE student (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    student_no      VARCHAR(20)  NOT NULL,
    department      VARCHAR(100) DEFAULT NULL,
    major           VARCHAR(100) DEFAULT NULL,
    class_name      VARCHAR(50)  DEFAULT NULL,
    enrollment_year VARCHAR(20)  DEFAULT NULL,
    status          VARCHAR(20)  NOT NULL,
    dormitory       VARCHAR(100) DEFAULT NULL,
    total_credits   DOUBLE       DEFAULT 0,
    card_balance    DOUBLE       DEFAULT 0,
    create_time     DATETIME     NOT NULL,
    update_time     DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_user (user_id),
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_student_no (student_no),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE teacher (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    teacher_no       VARCHAR(20)  NOT NULL,
    department       VARCHAR(100) DEFAULT NULL,
    title            VARCHAR(100) DEFAULT NULL,
    research_area    VARCHAR(100) DEFAULT NULL,
    teacher_type     VARCHAR(50)  DEFAULT NULL,
    office           VARCHAR(100) DEFAULT NULL,
    political_status VARCHAR(50)  DEFAULT NULL,
    create_time      DATETIME     NOT NULL,
    update_time      DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_user (user_id),
    UNIQUE KEY uk_teacher_no (teacher_no),
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE course (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    name             VARCHAR(100) NOT NULL,
    code             VARCHAR(20)  DEFAULT NULL,
    credits          DOUBLE       DEFAULT NULL,
    hours            INT          DEFAULT NULL,
    type             VARCHAR(50)  DEFAULT NULL,
    description      VARCHAR(200) DEFAULT NULL,
    teacher_id       BIGINT       DEFAULT NULL,
    semester         VARCHAR(50)  DEFAULT NULL,
    schedule         VARCHAR(100) DEFAULT NULL,
    classroom        VARCHAR(100) DEFAULT NULL,
    max_students     INT          DEFAULT NULL,
    current_students INT          NOT NULL DEFAULT 0,
    create_time      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_code (code),
    KEY idx_course_teacher (teacher_id),
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE course_selection (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    student_id  BIGINT      NOT NULL,
    course_id   BIGINT      NOT NULL,
    status      VARCHAR(20) NOT NULL,
    create_time DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_selection (student_id, course_id),
    KEY idx_selection_student (student_id, status),
    KEY idx_selection_course (course_id, status),
    CONSTRAINT fk_selection_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_selection_course  FOREIGN KEY (course_id)  REFERENCES course (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE grade (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    student_id  BIGINT      NOT NULL,
    course_id   BIGINT      NOT NULL,
    score       DOUBLE      NOT NULL,
    grade_level VARCHAR(20) NOT NULL,
    credits     DOUBLE      DEFAULT NULL,
    status      VARCHAR(20) NOT NULL,
    semester    VARCHAR(50) DEFAULT NULL,
    create_time DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_grade (student_id, course_id, semester),
    CONSTRAINT fk_grade_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_grade_course  FOREIGN KEY (course_id)  REFERENCES course (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE announcement (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(200) NOT NULL,
    content      TEXT,
    type         VARCHAR(50)  DEFAULT NULL,
    publisher_id BIGINT       DEFAULT NULL,
    top          TINYINT(1)   NOT NULL DEFAULT 0,
    publish_time DATETIME     NOT NULL,
    create_time  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_announcement_time (top, publish_time),
    CONSTRAINT fk_announcement_publisher FOREIGN KEY (publisher_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE news (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(200) NOT NULL,
    content      TEXT,
    cover_image  VARCHAR(500) DEFAULT NULL,
    category     VARCHAR(50)  DEFAULT NULL,
    author_id    BIGINT       DEFAULT NULL,
    view_count   INT          NOT NULL DEFAULT 0,
    publish_time DATETIME     NOT NULL,
    create_time  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_news_time (publish_time),
    CONSTRAINT fk_news_author FOREIGN KEY (author_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
