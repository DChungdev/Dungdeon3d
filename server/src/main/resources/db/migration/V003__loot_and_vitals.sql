-- ============================================================
-- Phase 0 week 5: loot persistence + character vitals
-- ============================================================

-- HP/MP saved when a character leaves an instance. NULL = full (never saved yet).
ALTER TABLE characters
    ADD COLUMN current_hp INT,
    ADD COLUMN current_mp INT,
    ADD CONSTRAINT chk_current_hp CHECK (current_hp IS NULL OR current_hp >= 0),
    ADD CONSTRAINT chk_current_mp CHECK (current_mp IS NULL OR current_mp >= 0);

-- One reward per (mob kill, character): makes loot idempotent even if a kill is processed twice.
-- mob_entity_id is the runtime entity id of that specific spawn (a respawned mob gets a new id).
CREATE TABLE mob_kill_rewards (
    mob_entity_id   UUID NOT NULL,
    character_id    UUID NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    mob_def_id      VARCHAR(50) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (mob_entity_id, character_id)
);

CREATE INDEX idx_mob_kill_rewards_character ON mob_kill_rewards (character_id, created_at DESC);

-- Item audit: loot drops are logged as CREATE with reason MOB_DROP.
ALTER TABLE item_audit_log DROP CONSTRAINT chk_item_action;
ALTER TABLE item_audit_log ADD CONSTRAINT chk_item_action CHECK (
    action IN ('CREATE', 'STACK', 'DELETE', 'TRADE', 'ADMIN_GRANT')
);
