-- 1. BẢNG NGƯỜI DÙNG & XÁC THỰC
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY, -- UUID
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NULL, -- NULL nếu dùng Google Auth
    google_id VARCHAR(255) UNIQUE NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500) DEFAULT NULL,
    streak_count INT DEFAULT 0,
    last_active_date DATE NULL,
    gem_count INT DEFAULT 0,
    experience_points INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_settings (
    user_id VARCHAR(36) PRIMARY KEY,
    daily_reminder_time TIME DEFAULT '20:00:00',
    notifications_enabled BOOLEAN DEFAULT TRUE,
    sound_fx_enabled BOOLEAN DEFAULT TRUE,
    ai_challenge_difficulty VARCHAR(20) DEFAULT 'ADAPTIVE', -- EASY, MEDIUM, HARD, ADAPTIVE
    target_learning_minutes INT DEFAULT 15,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 2. BẢNG DANH MỤC & LỘ TRÌNH (ROADMAP NODES)
CREATE TABLE roadmap_stages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stage_number INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    min_exp_required INT DEFAULT 0
);

CREATE TABLE roadmap_nodes (
    id VARCHAR(36) PRIMARY KEY,
    stage_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    node_type ENUM('VOCABULARY', 'GRAMMAR', 'LISTENING', 'READING', 'CHECKPOINT_BOSS') NOT NULL,
    summary VARCHAR(255) NOT NULL DEFAULT '',   -- mô tả ngắn hiển thị ở bottom sheet
    position_index INT NOT NULL,
    icon_res_name VARCHAR(100),
    FOREIGN KEY (stage_id) REFERENCES roadmap_stages(id) ON DELETE CASCADE
);

-- 3. NỘI DUNG TỪ 4 FILES JSON (MASTER CONTENT)

-- 3.1. Dữ liệu từ vocabulary.json
CREATE TABLE vocabulary_lessons (
    id VARCHAR(36) PRIMARY KEY,
    node_id VARCHAR(36),
    word VARCHAR(100) NOT NULL,
    phonetic VARCHAR(100),
    part_of_speech VARCHAR(30),
    meaning_vi TEXT NOT NULL,
    definition_en TEXT,
    example_sentence TEXT,
    example_translation TEXT,
    audio_url VARCHAR(500),
    image_url VARCHAR(500),
    cefr_level VARCHAR(10) DEFAULT 'A1',
    domain VARCHAR(100),
    synonyms_json JSON,                          -- ["work", "occupation"]
    antonyms_json JSON,
    version INT DEFAULT 1,
    FOREIGN KEY (node_id) REFERENCES roadmap_nodes(id) ON DELETE SET NULL
);

-- 3.2. Dữ liệu từ grammar.json
CREATE TABLE grammar_lessons (
    id VARCHAR(36) PRIMARY KEY,
    node_id VARCHAR(36),
    topic_name VARCHAR(150) NOT NULL,
    rule_explanation_markdown TEXT NOT NULL,
    formula VARCHAR(255),
    common_mistakes_json JSON,                   -- {"incorrect": "...", "correct": "...", "reason_vi": "..."}
    examples_json JSON,                          -- [{"en": "...", "vi": "..."}]
    cefr_level VARCHAR(10) DEFAULT 'A1',
    domain VARCHAR(100),
    version INT DEFAULT 1,
    FOREIGN KEY (node_id) REFERENCES roadmap_nodes(id) ON DELETE SET NULL
);

-- 3.3. Dữ liệu từ reading.json
CREATE TABLE reading_lessons (
    id VARCHAR(36) PRIMARY KEY,
    node_id VARCHAR(36),
    title VARCHAR(200) NOT NULL,
    passage_text TEXT NOT NULL,
    word_count INT,
    key_vocabulary_json JSON,                    -- ["developer", ...]
    cefr_level VARCHAR(10) DEFAULT 'A1',
    domain VARCHAR(100),
    version INT DEFAULT 1,
    FOREIGN KEY (node_id) REFERENCES roadmap_nodes(id) ON DELETE SET NULL
);

-- 3.4. Dữ liệu từ listening.json
CREATE TABLE listening_lessons (
    id VARCHAR(36) PRIMARY KEY,
    node_id VARCHAR(36),
    title VARCHAR(200) NOT NULL,
    context TEXT,
    audio_url VARCHAR(500) NOT NULL,             -- raw resource path, e.g. raw/listen_001
    transcript JSON NOT NULL,                    -- [{"speaker": "...", "text": "..."}]
    duration_seconds INT,
    cefr_level VARCHAR(10) DEFAULT 'A1',
    domain VARCHAR(100),
    version INT DEFAULT 1,
    FOREIGN KEY (node_id) REFERENCES roadmap_nodes(id) ON DELETE SET NULL
);

-- 3.5. Bảng câu hỏi dùng chung (Questions/Exercises) cho cả 4 kỹ năng
CREATE TABLE lesson_questions (
    id VARCHAR(36) PRIMARY KEY,
    parent_type ENUM('VOCABULARY', 'GRAMMAR', 'READING', 'LISTENING') NOT NULL,
    parent_id VARCHAR(36) NOT NULL,
    question_type ENUM('MULTIPLE_CHOICE', 'FILL_BLANK', 'MATCHING', 'REORDER', 'VOICE_INPUT') NOT NULL,
    prompt TEXT NOT NULL,
    media_url VARCHAR(500) NULL,
    options_json JSON NOT NULL, -- Mảng các lựa chọn
    correct_answer_json JSON NOT NULL, -- Đáp án chính xác
    explanation TEXT,
    points INT DEFAULT 10,
    skill_tag VARCHAR(100) NOT NULL DEFAULT ''   -- error_tag ghi vào user_weaknesses khi sai
);

-- 4. BẢNG TIẾN ĐỘ, SPACED REPETITION (SRS) & BỘ THEO DÕI ĐIỂM YẾU
CREATE TABLE user_node_progress (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(36) NOT NULL,
    status ENUM('LOCKED', 'AVAILABLE', 'COMPLETED', 'MASTERED') DEFAULT 'LOCKED',
    stars_earned INT DEFAULT 0,
    completed_at TIMESTAMP NULL,
    updated_at BIGINT NOT NULL DEFAULT 0,        -- epoch ms, last-write-wins khi sync
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (node_id) REFERENCES roadmap_nodes(id) ON DELETE CASCADE,
    UNIQUE KEY uq_user_node (user_id, node_id)
);

-- Theo dõi độ ghi nhớ (Spaced Repetition System - SM2 Algorithm)
CREATE TABLE user_item_retention (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    item_type ENUM('VOCABULARY', 'GRAMMAR') NOT NULL,
    item_id VARCHAR(36) NOT NULL,
    stability_factor DOUBLE DEFAULT 2.5,
    repetition_number INT DEFAULT 0,
    interval_days INT DEFAULT 1,
    next_review_at TIMESTAMP NOT NULL,
    last_reviewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_user_item (user_id, item_type, item_id)
);

-- Bảng phân tích điểm yếu (Crucial cho Personalized AI)
CREATE TABLE user_weaknesses (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    skill_type ENUM('VOCABULARY', 'GRAMMAR', 'LISTENING', 'READING') NOT NULL,
    error_tag VARCHAR(100) NOT NULL, -- vd: 'past_simple_vs_present_perfect', 'preposition_in_on_at', 'silent_letters'
    related_item_id VARCHAR(36) NULL,
    error_count INT DEFAULT 1,
    mastery_score FLOAT DEFAULT 0.0, -- 0.0 (rất yếu) -> 1.0 (đã khắc phục)
    last_failed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_user_error_tag (user_id, error_tag)
);

-- 5. PERSONALIZED AI LONG-TERM MEMORY & CONVERSATIONS
CREATE TABLE ai_persona_memories (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    memory_key VARCHAR(100) NOT NULL, -- vd: 'favorite_topic', 'weak_tense', 'learning_goal'
    memory_value TEXT NOT NULL,
    confidence_score FLOAT DEFAULT 1.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE ai_chat_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    session_title VARCHAR(150),
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE ai_chat_messages (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    sender ENUM('USER', 'AI_MASCOT') NOT NULL,
    message_text TEXT NOT NULL,
    detected_mistakes_json JSON NULL,
    correction_suggestions TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(id) ON DELETE CASCADE
);

-- 6. GAMIFICATION: BADGES & ACHIEVEMENTS
CREATE TABLE badges (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    icon_url VARCHAR(500) NOT NULL,
    category ENUM('STREAK', 'VOCAB_MASTER', 'GRAMMAR_PRO', 'AI_CHALLENGER') NOT NULL,
    criteria_json JSON NOT NULL
);

CREATE TABLE user_badges (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    badge_id VARCHAR(36) NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE,
    UNIQUE KEY uq_user_badge (user_id, badge_id)
);

-- 7. NHẬT KÝ HỌC (biểu đồ 7 ngày trong Hồ sơ)
CREATE TABLE daily_study_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    date_key DATE NOT NULL,
    minutes INT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_user_day (user_id, date_key)
);
