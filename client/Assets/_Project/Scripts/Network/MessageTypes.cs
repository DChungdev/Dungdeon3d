namespace DungeonDelvers.Network
{
    /// <summary>WebSocket message type names (network-protocol.md).</summary>
    public static class MessageTypes
    {
        // Connection
        public const string Connected = "CONNECTED";
        public const string Ping = "PING";
        public const string Pong = "PONG";
        public const string Ack = "ACK";
        public const string Error = "ERROR";
        public const string Kick = "KICK";
        public const string Reconnect = "RECONNECT";

        // Instance
        public const string JoinInstance = "JOIN_INSTANCE";
        public const string LeaveInstance = "LEAVE_INSTANCE";
        public const string InstanceEnter = "INSTANCE_ENTER";
        public const string MoveIntent = "MOVE_INTENT";
        public const string EntityUpdate = "ENTITY_UPDATE";
        public const string EntitySpawn = "ENTITY_SPAWN";
        public const string EntityDespawn = "ENTITY_DESPAWN";

        // Combat
        public const string AutoAttackTarget = "AUTO_ATTACK_TARGET";
        public const string SkillCast = "SKILL_CAST";
        public const string SkillResult = "SKILL_RESULT";
        public const string DamageEvent = "DAMAGE_EVENT";
        public const string EntityDeath = "ENTITY_DEATH";

        // Loot
        public const string LootDrop = "LOOT_DROP";
    }
}
