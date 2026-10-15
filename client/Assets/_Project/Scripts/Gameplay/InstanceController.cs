using System.Collections.Generic;
using DungeonDelvers.Network;
using DungeonDelvers.UI;
using Newtonsoft.Json.Linq;
using UnityEngine;

namespace DungeonDelvers.Gameplay
{
    /// <summary>
    /// Builds the instance scene from server messages: INSTANCE_ENTER snapshot, ENTITY_SPAWN /
    /// ENTITY_DESPAWN, 10 Hz ENTITY_UPDATE batches and combat events (DAMAGE_EVENT, ENTITY_DEATH).
    /// </summary>
    public class InstanceController : MonoBehaviour
    {
        private static readonly Color LocalColor = new Color(0.25f, 0.6f, 1f);
        private static readonly Color RemotePlayerColor = new Color(1f, 0.55f, 0.2f);
        private static readonly Color PlayerHpColor = new Color(0.2f, 0.85f, 0.3f);
        private static readonly Color MobHpColor = new Color(0.9f, 0.2f, 0.2f);

        public string InstanceId { get; private set; }
        public string LocalPlayerId { get; private set; }
        public long ServerTick { get; private set; }
        public int EntityCount => _entities.Count;
        public LocalPlayerController LocalPlayer { get; private set; }
        public EntityView LocalView { get; private set; }

        private readonly Dictionary<string, EntityView> _entities = new Dictionary<string, EntityView>();
        private readonly Dictionary<string, RemoteEntityView> _remoteMovers = new Dictionary<string, RemoteEntityView>();
        private WebSocketService _ws;
        private VirtualJoystick _input;
        private CameraFollow _camera;
        private CombatController _combat;
        private GameObject _room;

        public void Init(WebSocketService ws, VirtualJoystick input, CameraFollow cameraFollow, CombatController combat)
        {
            _ws = ws;
            _input = input;
            _camera = cameraFollow;
            _combat = combat;
        }

        /// <summary>Returns true if the message was an instance message.</summary>
        public bool HandleMessage(string type, JObject payload)
        {
            switch (type)
            {
                case MessageTypes.InstanceEnter:
                    OnInstanceEnter(payload);
                    return true;
                case MessageTypes.EntitySpawn:
                    SpawnEntity(payload["entity"] as JObject);
                    return true;
                case MessageTypes.EntityDespawn:
                    Despawn((string)payload["entityId"]);
                    return true;
                case MessageTypes.EntityUpdate:
                    OnEntityUpdate(payload);
                    return true;
                case MessageTypes.DamageEvent:
                    OnDamage(payload);
                    return true;
                case MessageTypes.EntityDeath:
                    OnDeath(payload);
                    return true;
                case MessageTypes.SkillResult:
                    _combat.OnSkillResult(payload);
                    return true;
                default:
                    return false;
            }
        }

        public bool TryGetEntity(string id, out EntityView view) => _entities.TryGetValue(id ?? "", out view);

        public EntityView NearestAliveMob(Vector3 from, float maxDistance)
        {
            EntityView best = null;
            var bestDistance = maxDistance;
            foreach (var view in _entities.Values)
            {
                if (!view.IsMob || !view.IsAlive)
                {
                    continue;
                }
                var d = Vector3.Distance(new Vector3(from.x, 0, from.z),
                    new Vector3(view.transform.position.x, 0, view.transform.position.z));
                if (d <= bestDistance)
                {
                    best = view;
                    bestDistance = d;
                }
            }
            return best;
        }

        /// <summary>Removes everything (leave instance / back to login).</summary>
        public void Clear()
        {
            foreach (var view in _entities.Values)
            {
                Destroy(view.gameObject);
            }
            _entities.Clear();
            _remoteMovers.Clear();
            LocalPlayer = null;
            LocalView = null;
            InstanceId = null;
            LocalPlayerId = null;
            _combat.ClearTarget();
            if (_camera != null)
            {
                _camera.Target = null;
            }
        }

        private void OnInstanceEnter(JObject payload)
        {
            // Also used for reconnect: rebuild from the full snapshot.
            Clear();
            InstanceId = (string)payload["instanceId"];
            LocalPlayerId = (string)payload["localPlayerId"];
            if (_room == null)
            {
                _room = GreyboxRoom.Build();
            }
            if (payload["entities"] is JArray entities)
            {
                foreach (var entity in entities)
                {
                    SpawnEntity(entity as JObject);
                }
            }
            Debug.Log($"[Instance] entered {InstanceId} with {EntityCount} entities");
        }

        private void SpawnEntity(JObject entity)
        {
            if (entity == null)
            {
                return;
            }
            var id = (string)entity["entityId"];
            if (id == null || _entities.ContainsKey(id))
            {
                return;
            }
            var entityType = (string)entity["entityType"] ?? "PLAYER";
            var isMob = entityType == "MOB";
            var isLocal = id == LocalPlayerId;
            var name = (string)entity["name"];
            var position = NetMath.ToVector3(entity["position"]);
            var yaw = (float?)entity["rotation"] ?? 0f;

            var body = isMob ? CreateMobBody((string)entity["defId"]) : GameObject.CreatePrimitive(PrimitiveType.Capsule);
            body.name = (isLocal ? "LocalPlayer_" : isMob ? "Mob_" : "Remote_") + name;
            Destroy(body.GetComponent<Collider>());
            var bodyColor = isMob ? MobColor((string)entity["defId"]) : isLocal ? LocalColor : RemotePlayerColor;
            GreyboxRoom.Paint(body, bodyColor);
            AddFacingMarker(body.transform);
            Nameplate.Create(body.transform, $"{name}  Lv{(int?)entity["level"] ?? 1}",
                isLocal ? Color.cyan : isMob ? new Color(1f, 0.6f, 0.6f) : Color.white);

            var view = body.AddComponent<EntityView>();
            view.Init(id, entityType, name, (int?)entity["hp"] ?? 1, (int?)entity["maxHp"] ?? 1,
                (int?)entity["mp"] ?? 0, (int?)entity["maxMp"] ?? 0,
                bodyColor, isMob ? MobHpColor : PlayerHpColor);
            _entities[id] = view;

            if (isLocal)
            {
                LocalPlayer = body.AddComponent<LocalPlayerController>();
                LocalPlayer.Init(_ws, _input, position, yaw);
                LocalPlayer.SetDead(!view.IsAlive);
                LocalView = view;
                if (_camera != null)
                {
                    _camera.Target = body.transform;
                }
            }
            else
            {
                var mover = body.AddComponent<RemoteEntityView>();
                mover.Init(position, yaw);
                _remoteMovers[id] = mover;
            }
        }

        private void Despawn(string entityId)
        {
            if (entityId == null || !_entities.TryGetValue(entityId, out var view))
            {
                return;
            }
            if (_combat.Target == view)
            {
                _combat.ClearTarget();
            }
            Destroy(view.gameObject);
            _entities.Remove(entityId);
            _remoteMovers.Remove(entityId);
        }

        private void OnEntityUpdate(JObject payload)
        {
            ServerTick = (long?)payload["tick"] ?? ServerTick;
            if (!(payload["entities"] is JArray entities))
            {
                return;
            }
            foreach (var token in entities)
            {
                var id = (string)token["entityId"];
                if (id == null || !_entities.TryGetValue(id, out var view))
                {
                    continue;
                }
                var position = NetMath.ToVector3(token["position"]);
                if (token["hp"] != null)
                {
                    view.SetHp((int)token["hp"]);
                }
                if (token["mp"] != null)
                {
                    view.SetMp((int)token["mp"]);
                }

                if (id == LocalPlayerId)
                {
                    LocalPlayer.SetDead(!view.IsAlive);
                    LocalPlayer.OnServerState(position, (long?)token["lastInputSeq"] ?? 0);
                }
                else if (_remoteMovers.TryGetValue(id, out var mover))
                {
                    mover.PushState(position, (float?)token["rotation"] ?? 0f, (string)token["state"]);
                }
            }
        }

        private void OnDamage(JObject payload)
        {
            var targetId = (string)payload["targetEntityId"];
            if (!_entities.TryGetValue(targetId ?? "", out var target))
            {
                return;
            }
            target.SetHp((int?)payload["targetHpRemaining"] ?? target.Hp);
            var amount = (int?)payload["amount"] ?? 0;
            var crit = (bool?)payload["crit"] ?? false;
            var color = targetId == LocalPlayerId ? new Color(1f, 0.35f, 0.35f) : Color.white;
            DamagePopup.Spawn(target.transform.position, amount, crit, color);
        }

        private void OnDeath(JObject payload)
        {
            var id = (string)payload["entityId"];
            if (!_entities.TryGetValue(id ?? "", out var view))
            {
                return;
            }
            view.Die();
            if (id == LocalPlayerId)
            {
                LocalPlayer.SetDead(true);
                _combat.ClearTarget();
            }
        }

        private static GameObject CreateMobBody(string defId)
        {
            var body = GameObject.CreatePrimitive(PrimitiveType.Capsule);
            // Greybox silhouettes so mob types are distinguishable without art.
            body.transform.localScale = defId == "mob_goblin_archer"
                ? new Vector3(0.6f, 1f, 0.6f)
                : new Vector3(0.9f, 1f, 0.9f);
            return body;
        }

        private static Color MobColor(string defId) => defId == "mob_goblin_archer"
            ? new Color(0.3f, 0.55f, 0.25f)
            : new Color(0.75f, 0.2f, 0.2f);

        /// <summary>Small box on the capsule's front so rotation is visible.</summary>
        private static void AddFacingMarker(Transform body)
        {
            var nose = GameObject.CreatePrimitive(PrimitiveType.Cube);
            nose.name = "Facing";
            Destroy(nose.GetComponent<Collider>());
            nose.transform.SetParent(body, false);
            nose.transform.localPosition = new Vector3(0f, 0.4f, 0.45f);
            nose.transform.localScale = new Vector3(0.3f, 0.15f, 0.3f);
            GreyboxRoom.Paint(nose, Color.black);
        }
    }
}
