-- Dungeon Delvers Online — Seed Config Data MVP
-- Flyway V002

INSERT INTO config_meta (id, version, hash) VALUES
    (1, '1.0.0', 'seed-mvp-v1');

-- ============================================================
-- ITEMS
-- ============================================================
INSERT INTO item_definitions (id, name, description, item_type, equip_slot, stackable, max_stack, base_stats_json, level_required, vendor_price) VALUES
    ('sword_wooden', 'Wooden Sword', 'A basic training sword.', 'WEAPON', 'WEAPON', FALSE, 1, '{"atk": 5}', 1, 50),
    ('sword_iron', 'Iron Sword', 'Reliable iron blade.', 'WEAPON', 'WEAPON', FALSE, 1, '{"atk": 15}', 5, 200),
    ('sword_steel', 'Steel Sword', 'Well-forged steel sword.', 'WEAPON', 'WEAPON', FALSE, 1, '{"atk": 28}', 10, 500),
    ('staff_apprentice', 'Apprentice Staff', 'Basic mage staff.', 'WEAPON', 'WEAPON', FALSE, 1, '{"atk": 8, "maxMp": 20}', 1, 50),
    ('staff_arcane', 'Arcane Staff', 'Channels arcane energy.', 'WEAPON', 'WEAPON', FALSE, 1, '{"atk": 22, "maxMp": 50}', 10, 500),
    ('helm_leather', 'Leather Helm', 'Light head protection.', 'ARMOR', 'HELM', FALSE, 1, '{"def": 3, "maxHp": 20}', 1, 40),
    ('helm_iron', 'Iron Helm', 'Sturdy iron helm.', 'ARMOR', 'HELM', FALSE, 1, '{"def": 8, "maxHp": 50}', 8, 180),
    ('chest_leather', 'Leather Vest', 'Basic chest armor.', 'ARMOR', 'CHEST', FALSE, 1, '{"def": 5, "maxHp": 30}', 1, 60),
    ('chest_chain', 'Chainmail', 'Interlocked metal rings.', 'ARMOR', 'CHEST', FALSE, 1, '{"def": 12, "maxHp": 80}', 10, 350),
    ('gloves_leather', 'Leather Gloves', 'Simple gloves.', 'ARMOR', 'GLOVES', FALSE, 1, '{"def": 2, "atk": 1}', 1, 30),
    ('boots_leather', 'Leather Boots', 'Comfortable boots.', 'ARMOR', 'BOOTS', FALSE, 1, '{"def": 2, "moveSpeed": 0.2}', 1, 30),
    ('ring_copper', 'Copper Ring', 'A simple ring.', 'ACCESSORY', 'ACCESSORY', FALSE, 1, '{"critRate": 0.02}', 5, 100),
    ('mat_goblin_ear', 'Goblin Ear', 'Trophy from a goblin.', 'MATERIAL', NULL, TRUE, 999, '{}', 1, 10),
    ('mat_bone', 'Bone Fragment', 'Undead remains.', 'MATERIAL', NULL, TRUE, 999, '{}', 1, 15),
    ('mat_dragon_scale', 'Dragon Scale', 'Rare dragon scale.', 'MATERIAL', NULL, TRUE, 999, '{}', 1, 50),
    ('mat_iron_ore', 'Iron Ore', 'Raw iron ore.', 'MATERIAL', NULL, TRUE, 999, '{}', 1, 12),
    ('potion_hp_small', 'Small HP Potion', 'Restores 200 HP.', 'CONSUMABLE', NULL, TRUE, 20, '{"healHp": 200}', 1, 25);

-- ============================================================
-- SKILLS — WARRIOR
-- ============================================================
INSERT INTO skill_definitions (id, class_type, name, description, cooldown_sec, mp_cost, base_damage, atk_scaling, target_type, range_m, aoe_radius_m, effect_json) VALUES
    ('warrior_charge', 'WARRIOR', 'Charge', 'Rush to target dealing damage.', 8.0, 15, 20, 0.8, 'ENEMY', 8.0, 0, '{"knockback": 2.0}'),
    ('warrior_shield_bash', 'WARRIOR', 'Shield Bash', 'Stun target for 1.5 seconds.', 12.0, 20, 15, 0.5, 'ENEMY', 2.5, 0, '{"stunSec": 1.5}'),
    ('warrior_whirlwind', 'WARRIOR', 'Whirlwind', 'Spin attack hitting nearby enemies.', 10.0, 25, 30, 0.6, 'POINT_BLANK_AOE', 0, 3.0, '{}'),
    ('warrior_battle_cry', 'WARRIOR', 'Battle Cry', 'Increase party ATK by 20%.', 20.0, 30, 0, 0, 'SELF', 0, 10.0, '{"buffId": "battle_cry", "atkPercent": 0.2, "durationSec": 8}');

-- ============================================================
-- SKILLS — MAGE
-- ============================================================
INSERT INTO skill_definitions (id, class_type, name, description, cooldown_sec, mp_cost, base_damage, atk_scaling, target_type, range_m, aoe_radius_m, effect_json) VALUES
    ('mage_firebolt', 'MAGE', 'Firebolt', 'Hurl a bolt of fire.', 3.0, 10, 25, 1.0, 'ENEMY', 10.0, 0, '{}'),
    ('mage_frost_nova', 'MAGE', 'Frost Nova', 'Freeze nearby enemies slowing 40%.', 12.0, 30, 20, 0.5, 'POINT_BLANK_AOE', 0, 4.0, '{"slowPercent": 0.4, "durationSec": 3}'),
    ('mage_arcane_missile', 'MAGE', 'Arcane Missile', 'Channel 3 missiles at target.', 8.0, 20, 15, 0.7, 'ENEMY', 12.0, 0, '{"hits": 3, "channelSec": 1.5}'),
    ('mage_mana_shield', 'MAGE', 'Mana Shield', 'Absorb damage for 5 seconds.', 18.0, 40, 0, 0, 'SELF', 0, 0, '{"buffId": "mana_shield", "absorbPercent": 0.3, "durationSec": 5}');

-- ============================================================
-- MOBS
-- ============================================================
INSERT INTO mob_definitions (id, name, mob_type, level, hp, atk, def, move_speed, aggro_radius_m, leash_radius_m, attack_range_m, ai_profile, loot_table_id) VALUES
    ('mob_goblin_grunt', 'Goblin Grunt', 'NORMAL', 1, 150, 8, 3, 3.5, 8.0, 20.0, 2.0, 'MELEE_BASIC', 'loot_goblin_normal'),
    ('mob_goblin_archer', 'Goblin Archer', 'NORMAL', 3, 120, 12, 2, 3.0, 10.0, 22.0, 8.0, 'RANGED_BASIC', 'loot_goblin_normal'),
    ('mob_goblin_shaman', 'Goblin Shaman', 'ELITE', 5, 300, 15, 5, 3.0, 8.0, 20.0, 6.0, 'RANGED_BASIC', 'loot_goblin_elite'),
    ('mob_boss_goblin_king', 'Goblin King', 'BOSS', 5, 2000, 25, 10, 3.5, 12.0, 30.0, 3.0, 'BOSS_MELEE', 'loot_boss_goblin'),
    ('mob_skeleton', 'Skeleton', 'NORMAL', 10, 250, 18, 8, 3.0, 8.0, 20.0, 2.0, 'MELEE_BASIC', 'loot_undead_normal'),
    ('mob_zombie', 'Zombie', 'NORMAL', 12, 400, 15, 12, 2.0, 6.0, 18.0, 2.0, 'MELEE_SLOW', 'loot_undead_normal'),
    ('mob_boss_lich', 'Lich', 'BOSS', 15, 5000, 35, 15, 2.5, 15.0, 35.0, 8.0, 'BOSS_CASTER', 'loot_boss_lich'),
    ('mob_boss_dragon', 'Young Dragon', 'BOSS', 22, 8000, 45, 20, 4.0, 15.0, 40.0, 5.0, 'BOSS_DRAGON', 'loot_boss_dragon');

-- ============================================================
-- DUNGEONS
-- ============================================================
INSERT INTO dungeon_definitions (id, name, description, min_level, max_party_size, room_count, par_time_ms, layout_type, layout_json) VALUES
    ('dungeon_goblin_cave', 'Goblin Cave', 'A cave infested with goblins.', 1, 4, 3, 600000, 'FIXED',
     '{"rooms": ["goblin_room_1", "goblin_room_2", "goblin_boss"], "mobs": {"goblin_room_1": ["mob_goblin_grunt", "mob_goblin_grunt", "mob_goblin_archer"], "goblin_room_2": ["mob_goblin_grunt", "mob_goblin_shaman"], "goblin_boss": ["mob_boss_goblin_king"]}}'),
    ('dungeon_undead_crypt', 'Undead Crypt', 'Crypt of the restless dead.', 10, 4, 4, 900000, 'FIXED',
     '{"rooms": ["crypt_room_1", "crypt_room_2", "crypt_room_3", "crypt_boss"], "mobs": {"crypt_room_1": ["mob_skeleton", "mob_skeleton"], "crypt_room_2": ["mob_skeleton", "mob_zombie"], "crypt_room_3": ["mob_zombie", "mob_zombie", "mob_skeleton"], "crypt_boss": ["mob_boss_lich"]}}'),
    ('dungeon_dragon_nest', 'Dragon Nest', 'Nest of a young dragon.', 20, 4, 5, 1200000, 'FIXED',
     '{"rooms": ["dragon_room_1", "dragon_room_2", "dragon_room_3", "dragon_room_4", "dragon_boss"], "mobs": {"dragon_boss": ["mob_boss_dragon"]}}'),
    ('dungeon_procedural', 'Rift Dungeon', 'A shifting dimensional rift.', 15, 4, 6, 900000, 'PROCEDURAL',
     '{"roomTemplates": ["proc_room_empty", "proc_room_mob", "proc_room_elite", "proc_room_boss"], "minRooms": 4, "maxRooms": 7}');

-- ============================================================
-- LOOT TABLES
-- ============================================================
INSERT INTO loot_tables (id, name) VALUES
    ('loot_goblin_normal', 'Goblin Normal Drops'),
    ('loot_goblin_elite', 'Goblin Elite Drops'),
    ('loot_boss_goblin', 'Goblin King Drops'),
    ('loot_undead_normal', 'Undead Normal Drops'),
    ('loot_boss_lich', 'Lich Drops'),
    ('loot_boss_dragon', 'Dragon Drops');

INSERT INTO loot_table_entries (loot_table_id, item_def_id, weight, min_qty, max_qty, min_rarity) VALUES
    ('loot_goblin_normal', 'mat_goblin_ear', 400, 1, 2, 'COMMON'),
    ('loot_goblin_normal', 'sword_wooden', 50, 1, 1, 'COMMON'),
    ('loot_goblin_normal', 'helm_leather', 50, 1, 1, 'COMMON'),
    ('loot_goblin_normal', 'sword_iron', 20, 1, 1, 'RARE'),
    ('loot_goblin_elite', 'mat_goblin_ear', 200, 2, 3, 'COMMON'),
    ('loot_goblin_elite', 'sword_iron', 100, 1, 1, 'RARE'),
    ('loot_goblin_elite', 'chest_leather', 80, 1, 1, 'RARE'),
    ('loot_boss_goblin', 'mat_goblin_ear', 100, 3, 5, 'COMMON'),
    ('loot_boss_goblin', 'sword_steel', 150, 1, 1, 'RARE'),
    ('loot_boss_goblin', 'sword_steel', 30, 1, 1, 'EPIC'),
    ('loot_undead_normal', 'mat_bone', 400, 1, 2, 'COMMON'),
    ('loot_undead_normal', 'helm_iron', 30, 1, 1, 'RARE'),
    ('loot_boss_lich', 'mat_bone', 100, 3, 5, 'COMMON'),
    ('loot_boss_lich', 'chest_chain', 120, 1, 1, 'RARE'),
    ('loot_boss_lich', 'staff_arcane', 25, 1, 1, 'EPIC'),
    ('loot_boss_dragon', 'mat_dragon_scale', 200, 1, 3, 'RARE'),
    ('loot_boss_dragon', 'chest_chain', 100, 1, 1, 'EPIC'),
    ('loot_boss_dragon', 'ring_copper', 50, 1, 1, 'EPIC');

-- ============================================================
-- RECIPES
-- ============================================================
INSERT INTO recipe_definitions (id, name, output_item_id, output_qty, gold_cost, inputs_json, level_required) VALUES
    ('recipe_sword_iron', 'Craft Iron Sword', 'sword_iron', 1, 100, '{"mat_iron_ore": 5, "mat_goblin_ear": 3}', 5),
    ('recipe_helm_iron', 'Craft Iron Helm', 'helm_iron', 1, 80, '{"mat_iron_ore": 3, "mat_bone": 2}', 8),
    ('recipe_potion_hp', 'Craft HP Potion', 'potion_hp_small', 3, 30, '{"mat_goblin_ear": 2}', 1);

-- ============================================================
-- QUESTS
-- ============================================================
INSERT INTO quest_definitions (id, name, description, quest_type, objective_json, reward_json, is_daily, is_tutorial) VALUES
    ('quest_tutorial_move', 'First Steps', 'Learn to move.', 'TALK', '{"step": 1}', '{"xp": 50}', FALSE, TRUE),
    ('quest_tutorial_combat', 'First Blood', 'Defeat goblins.', 'KILL', '{"mobId": "mob_goblin_grunt", "count": 3}', '{"xp": 100, "gold": 50}', FALSE, TRUE),
    ('quest_tutorial_dungeon', 'Cave Delver', 'Clear Goblin Cave.', 'CLEAR_DUNGEON', '{"dungeonId": "dungeon_goblin_cave"}', '{"xp": 200, "itemDefId": "sword_iron"}', FALSE, TRUE),
    ('quest_daily_clear', 'Daily Delve', 'Clear any dungeon.', 'CLEAR_DUNGEON', '{"count": 1}', '{"gold": 50, "xp": 100}', TRUE, FALSE),
    ('quest_daily_kill', 'Pest Control', 'Slay 30 monsters.', 'KILL', '{"count": 30}', '{"gold": 50, "xp": 80}', TRUE, FALSE),
    ('quest_daily_craft', 'Artisan Work', 'Craft 1 item.', 'CRAFT', '{"count": 1}', '{"gold": 50, "xp": 60}', TRUE, FALSE);

-- ============================================================
-- SHOP (NPC + GEM placeholder)
-- ============================================================
INSERT INTO shop_catalog (id, name, shop_type, item_def_id, gem_cost, gold_cost, active) VALUES
    ('shop_potion_hp', 'HP Potion', 'NPC', 'potion_hp_small', NULL, 25, TRUE),
    ('shop_sword_iron', 'Iron Sword', 'NPC', 'sword_iron', NULL, 200, TRUE);

-- ============================================================
-- BATTLE PASS SEASON 1 (skeleton)
-- ============================================================
INSERT INTO battle_pass_tiers (season_id, tier, xp_required, free_reward_json, premium_reward_json) VALUES
    ('season_1', 1, 100, '{"gold": 100}', '{"cosmeticId": "skin_shadow_t1"}'),
    ('season_1', 5, 500, '{"gold": 200}', '{"cosmeticId": "emote_dance"}'),
    ('season_1', 10, 1000, '{"itemDefId": "ring_copper"}', '{"cosmeticId": "weapon_glow_shadow"}'),
    ('season_1', 15, 1500, '{"gold": 500}', '{"cosmeticId": "skin_shadow_full"}'),
    ('season_1', 30, 3000, '{"gold": 1000}', '{"cosmeticId": "title_shadow_delver"}');
