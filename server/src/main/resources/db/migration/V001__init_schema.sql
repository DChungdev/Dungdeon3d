-- Dungeon Delvers Online — Initial Schema
-- Flyway V001

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ACCOUNTS
-- ============================================================
CREATE TABLE accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) UNIQUE,
    password_hash   VARCHAR(255),
    guest_device_id VARCHAR(128) UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT chk_account_identity CHECK (
        email IS NOT NULL OR guest_device_id IS NOT NULL
    ),
    CONSTRAINT chk_account_status CHECK (
        status IN ('ACTIVE', 'BANNED', 'SUSPENDED')
    )
);

CREATE INDEX idx_accounts_email ON accounts (email) WHERE email IS NOT NULL;
CREATE INDEX idx_accounts_guest ON accounts (guest_device_id) WHERE guest_device_id IS NOT NULL;

-- ============================================================
-- CHARACTERS
-- ============================================================
CREATE TABLE characters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES accounts (id),
    name            VARCHAR(20) NOT NULL UNIQUE,
    class_type      VARCHAR(20) NOT NULL,
    level           INT NOT NULL DEFAULT 1,
    xp              BIGINT NOT NULL DEFAULT 0,
    gold            INT NOT NULL DEFAULT 0,
    premium_gems    INT NOT NULL DEFAULT 0,
    stats_json      JSONB NOT NULL DEFAULT '{}',
    hub_shard_id    VARCHAR(50),
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_class_type CHECK (class_type IN ('WARRIOR', 'MAGE')),
    CONSTRAINT chk_level CHECK (level BETWEEN 1 AND 100),
    CONSTRAINT chk_gold CHECK (gold >= 0),
    CONSTRAINT chk_gems CHECK (premium_gems >= 0)
);

CREATE INDEX idx_characters_account ON characters (account_id);
CREATE INDEX idx_characters_name ON characters (name);

-- ============================================================
-- INVENTORY
-- ============================================================
CREATE TABLE inventory_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id    UUID NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    item_def_id     VARCHAR(50) NOT NULL,
    quantity        INT NOT NULL DEFAULT 1,
    rarity          VARCHAR(20) NOT NULL DEFAULT 'COMMON',
    enhance_level   INT NOT NULL DEFAULT 0,
    bound           BOOLEAN NOT NULL DEFAULT FALSE,
    slot_index      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_rarity CHECK (rarity IN ('COMMON', 'RARE', 'EPIC')),
    CONSTRAINT chk_enhance CHECK (enhance_level BETWEEN 0 AND 10)
);

CREATE INDEX idx_inventory_character ON inventory_items (character_id);
CREATE INDEX idx_inventory_item_def ON inventory_items (character_id, item_def_id);

-- ============================================================
-- EQUIPMENT
-- ============================================================
CREATE TABLE equipment_slots (
    character_id        UUID NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    slot_type           VARCHAR(20) NOT NULL,
    inventory_item_id   UUID REFERENCES inventory_items (id) ON DELETE SET NULL,
    PRIMARY KEY (character_id, slot_type),
    CONSTRAINT chk_slot_type CHECK (
        slot_type IN ('WEAPON', 'HELM', 'CHEST', 'GLOVES', 'BOOTS', 'ACCESSORY')
    )
);

-- ============================================================
-- QUESTS
-- ============================================================
CREATE TABLE quest_progress (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id    UUID NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    quest_def_id    VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    progress_json   JSONB NOT NULL DEFAULT '{}',
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    claimed_at      TIMESTAMPTZ,
    CONSTRAINT chk_quest_status CHECK (
        status IN ('ACTIVE', 'COMPLETED', 'CLAIMED')
    )
);

CREATE INDEX idx_quest_character ON quest_progress (character_id);
CREATE UNIQUE INDEX idx_quest_daily_unique ON quest_progress (
    character_id, quest_def_id, ((assigned_at AT TIME ZONE 'UTC')::DATE)
);

-- ============================================================
-- SOCIAL — FRIENDS
-- ============================================================
CREATE TABLE friendships (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id        UUID NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    friend_character_id UUID NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_friendship UNIQUE (character_id, friend_character_id),
    CONSTRAINT chk_not_self_friend CHECK (character_id != friend_character_id)
);

CREATE INDEX idx_friendships_character ON friendships (character_id);

-- ============================================================
-- SOCIAL — GUILDS
-- ============================================================
CREATE TABLE guilds (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(30) NOT NULL UNIQUE,
    tag                     VARCHAR(5) NOT NULL UNIQUE,
    leader_character_id     UUID NOT NULL REFERENCES characters (id),
    max_members             INT NOT NULL DEFAULT 30,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_guild_max CHECK (max_members BETWEEN 1 AND 100)
);

CREATE TABLE guild_members (
    guild_id        UUID NOT NULL REFERENCES guilds (id) ON DELETE CASCADE,
    character_id    UUID NOT NULL UNIQUE REFERENCES characters (id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (guild_id, character_id),
    CONSTRAINT chk_guild_role CHECK (role IN ('LEADER', 'OFFICER', 'MEMBER'))
);

CREATE INDEX idx_guild_members_character ON guild_members (character_id);

-- ============================================================
-- DUNGEON RUNS
-- ============================================================
CREATE TABLE dungeon_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id     UUID NOT NULL,
    dungeon_def_id  VARCHAR(50) NOT NULL,
    difficulty      VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    party_size      INT NOT NULL DEFAULT 1,
    success         BOOLEAN,
    clear_time_ms   INT,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at        TIMESTAMPTZ,
    CONSTRAINT chk_difficulty CHECK (difficulty IN ('NORMAL', 'HARD'))
);

CREATE TABLE dungeon_run_participants (
    run_id          UUID NOT NULL REFERENCES dungeon_runs (id) ON DELETE CASCADE,
    character_id    UUID NOT NULL REFERENCES characters (id),
    PRIMARY KEY (run_id, character_id)
);

CREATE INDEX idx_dungeon_runs_instance ON dungeon_runs (instance_id);

-- ============================================================
-- LEADERBOARD
-- ============================================================
CREATE TABLE leaderboard_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    season_id       VARCHAR(20) NOT NULL,
    dungeon_def_id  VARCHAR(50) NOT NULL,
    character_id    UUID NOT NULL REFERENCES characters (id),
    clear_time_ms   INT NOT NULL,
    party_size      INT NOT NULL DEFAULT 1,
    achieved_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    validated       BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_clear_time CHECK (clear_time_ms > 0)
);

CREATE INDEX idx_leaderboard_rank ON leaderboard_entries (
    season_id, dungeon_def_id, clear_time_ms
);
CREATE INDEX idx_leaderboard_character ON leaderboard_entries (character_id);

-- ============================================================
-- MONETIZATION
-- ============================================================
CREATE TABLE purchase_transactions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id              UUID NOT NULL REFERENCES accounts (id),
    product_id              VARCHAR(100) NOT NULL,
    store_transaction_id    VARCHAR(200) NOT NULL UNIQUE,
    receipt_hash            VARCHAR(64),
    amount_cents            INT,
    currency_code           VARCHAR(3) DEFAULT 'USD',
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMPTZ,
    CONSTRAINT chk_purchase_status CHECK (
        status IN ('PENDING', 'COMPLETED', 'REFUNDED', 'FAILED')
    )
);

CREATE INDEX idx_purchase_account ON purchase_transactions (account_id);

CREATE TABLE battle_pass_progress (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id        UUID NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    season_id           VARCHAR(20) NOT NULL,
    current_xp          INT NOT NULL DEFAULT 0,
    current_tier        INT NOT NULL DEFAULT 0,
    premium_unlocked    BOOLEAN NOT NULL DEFAULT FALSE,
    claimed_free_json   JSONB NOT NULL DEFAULT '[]',
    claimed_premium_json JSONB NOT NULL DEFAULT '[]',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_battle_pass_season UNIQUE (character_id, season_id)
);

CREATE TABLE owned_cosmetics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id    UUID NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    cosmetic_id     VARCHAR(50) NOT NULL,
    cosmetic_type   VARCHAR(20) NOT NULL,
    acquired_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_owned_cosmetic UNIQUE (character_id, cosmetic_id),
    CONSTRAINT chk_cosmetic_type CHECK (
        cosmetic_type IN ('SKIN', 'WEAPON_SKIN', 'EMOTE', 'TITLE', 'FRAME')
    )
);

-- ============================================================
-- AUDIT LOGS
-- ============================================================
CREATE TABLE currency_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    character_id    UUID NOT NULL REFERENCES characters (id),
    currency_type   VARCHAR(20) NOT NULL,
    delta           INT NOT NULL,
    balance_after   INT NOT NULL,
    reason          VARCHAR(100) NOT NULL,
    ref_id          UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_currency_type CHECK (currency_type IN ('GOLD', 'GEMS'))
);

CREATE INDEX idx_currency_audit_character ON currency_audit_log (character_id, created_at DESC);

CREATE TABLE item_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    character_id    UUID NOT NULL REFERENCES characters (id),
    inventory_item_id UUID,
    item_def_id     VARCHAR(50) NOT NULL,
    rarity          VARCHAR(20) NOT NULL,
    action          VARCHAR(20) NOT NULL,
    reason          VARCHAR(100) NOT NULL,
    ref_id          UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_item_action CHECK (
        action IN ('CREATE', 'DELETE', 'TRADE', 'ADMIN_GRANT')
    )
);

CREATE INDEX idx_item_audit_character ON item_audit_log (character_id, created_at DESC);

-- ============================================================
-- GAME CONFIG TABLES
-- ============================================================
CREATE TABLE config_meta (
    id              INT PRIMARY KEY DEFAULT 1,
    version         VARCHAR(20) NOT NULL,
    hash            VARCHAR(64) NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT single_row CHECK (id = 1)
);

CREATE TABLE item_definitions (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    item_type       VARCHAR(20) NOT NULL,
    equip_slot      VARCHAR(20),
    stackable       BOOLEAN NOT NULL DEFAULT FALSE,
    max_stack       INT NOT NULL DEFAULT 1,
    base_stats_json JSONB NOT NULL DEFAULT '{}',
    level_required  INT NOT NULL DEFAULT 1,
    vendor_price    INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_item_type CHECK (
        item_type IN ('WEAPON', 'ARMOR', 'ACCESSORY', 'MATERIAL', 'CONSUMABLE')
    )
);

CREATE TABLE skill_definitions (
    id              VARCHAR(50) PRIMARY KEY,
    class_type      VARCHAR(20) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    cooldown_sec    NUMERIC(5, 2) NOT NULL,
    mp_cost         INT NOT NULL DEFAULT 0,
    base_damage     INT NOT NULL DEFAULT 0,
    atk_scaling     NUMERIC(4, 2) NOT NULL DEFAULT 0,
    target_type     VARCHAR(20) NOT NULL,
    range_m         NUMERIC(5, 2) NOT NULL DEFAULT 0,
    aoe_radius_m    NUMERIC(5, 2) NOT NULL DEFAULT 0,
    effect_json     JSONB NOT NULL DEFAULT '{}',
    CONSTRAINT chk_skill_class CHECK (class_type IN ('WARRIOR', 'MAGE')),
    CONSTRAINT chk_target_type CHECK (
        target_type IN ('SELF', 'ENEMY', 'ALLY', 'GROUND_AOE', 'POINT_BLANK_AOE')
    )
);

CREATE TABLE mob_definitions (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    mob_type        VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    level           INT NOT NULL DEFAULT 1,
    hp              INT NOT NULL,
    atk             INT NOT NULL,
    def             INT NOT NULL,
    move_speed      NUMERIC(4, 2) NOT NULL DEFAULT 3.0,
    aggro_radius_m  NUMERIC(5, 2) NOT NULL DEFAULT 8.0,
    leash_radius_m  NUMERIC(5, 2) NOT NULL DEFAULT 20.0,
    attack_range_m  NUMERIC(5, 2) NOT NULL DEFAULT 2.0,
    ai_profile      VARCHAR(50) NOT NULL DEFAULT 'MELEE_BASIC',
    loot_table_id   VARCHAR(50),
    CONSTRAINT chk_mob_type CHECK (mob_type IN ('NORMAL', 'ELITE', 'BOSS'))
);

CREATE TABLE dungeon_definitions (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    min_level       INT NOT NULL DEFAULT 1,
    max_party_size  INT NOT NULL DEFAULT 4,
    room_count      INT NOT NULL DEFAULT 3,
    par_time_ms     INT NOT NULL DEFAULT 900000,
    layout_type     VARCHAR(20) NOT NULL DEFAULT 'FIXED',
    layout_json     JSONB NOT NULL DEFAULT '{}',
    CONSTRAINT chk_layout_type CHECK (layout_type IN ('FIXED', 'PROCEDURAL'))
);

CREATE TABLE loot_tables (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL
);

CREATE TABLE loot_table_entries (
    id              SERIAL PRIMARY KEY,
    loot_table_id   VARCHAR(50) NOT NULL REFERENCES loot_tables (id) ON DELETE CASCADE,
    item_def_id     VARCHAR(50) NOT NULL REFERENCES item_definitions (id),
    weight          INT NOT NULL DEFAULT 100,
    min_qty         INT NOT NULL DEFAULT 1,
    max_qty         INT NOT NULL DEFAULT 1,
    min_rarity      VARCHAR(20) NOT NULL DEFAULT 'COMMON',
    CONSTRAINT chk_loot_weight CHECK (weight > 0)
);

CREATE INDEX idx_loot_entries_table ON loot_table_entries (loot_table_id);

CREATE TABLE recipe_definitions (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    output_item_id  VARCHAR(50) NOT NULL REFERENCES item_definitions (id),
    output_qty      INT NOT NULL DEFAULT 1,
    gold_cost       INT NOT NULL DEFAULT 0,
    inputs_json     JSONB NOT NULL,
    level_required  INT NOT NULL DEFAULT 1
);

CREATE TABLE quest_definitions (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    quest_type      VARCHAR(20) NOT NULL,
    objective_json  JSONB NOT NULL,
    reward_json     JSONB NOT NULL,
    is_daily        BOOLEAN NOT NULL DEFAULT FALSE,
    is_tutorial     BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_quest_type CHECK (
        quest_type IN ('KILL', 'CLEAR_DUNGEON', 'COLLECT', 'CRAFT', 'TALK')
    )
);

CREATE TABLE shop_catalog (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    shop_type       VARCHAR(20) NOT NULL,
    item_def_id     VARCHAR(50) REFERENCES item_definitions (id),
    cosmetic_id     VARCHAR(50),
    gem_cost        INT,
    gold_cost       INT,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_shop_type CHECK (shop_type IN ('NPC', 'GEM', 'PREMIUM'))
);

CREATE TABLE battle_pass_tiers (
    season_id       VARCHAR(20) NOT NULL,
    tier            INT NOT NULL,
    xp_required     INT NOT NULL,
    free_reward_json  JSONB NOT NULL DEFAULT '{}',
    premium_reward_json JSONB NOT NULL DEFAULT '{}',
    PRIMARY KEY (season_id, tier)
);

-- ============================================================
-- ADMIN
-- ============================================================
CREATE TABLE admin_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    admin_account   VARCHAR(255) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    target_id       UUID,
    details_json    JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TRIGGERS
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_characters_updated
    BEFORE UPDATE ON characters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_battle_pass_updated
    BEFORE UPDATE ON battle_pass_progress
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
