using DungeonDelvers.Network;
using Newtonsoft.Json.Linq;
using UnityEngine;
using UnityEngine.InputSystem;

namespace DungeonDelvers.Gameplay
{
    /// <summary>
    /// Player combat input (Phase 0): Attack locks the nearest mob and starts server-side
    /// auto-attack; Charge casts warrior_charge on the current target. All damage is decided by
    /// the server — this class only sends intents and shows the target / cooldown.
    /// Keyboard: Space = attack, Q = Charge.
    /// </summary>
    public class CombatController : MonoBehaviour
    {
        public const string ChargeSkillId = "warrior_charge";
        /// <summary>Lock-on search radius; the server checks the real attack range per swing.</summary>
        private const float TargetSearchRadius = 15f;
        private const float ChargeRangeM = 8f;

        public EntityView Target { get; private set; }
        public float ChargeCooldownRemaining => Mathf.Max(0f, _chargeReadyAt - Time.time);
        public string LastSkillMessage { get; private set; } = "";

        private WebSocketService _ws;
        private InstanceController _instance;
        private Transform _ring;
        private float _chargeReadyAt;

        public void Init(WebSocketService ws, InstanceController instance)
        {
            _ws = ws;
            _instance = instance;
            _ring = CreateTargetRing();
        }

        private void Update()
        {
            var kb = Keyboard.current;
            if (kb != null && _instance.LocalPlayer != null)
            {
                if (kb.spaceKey.wasPressedThisFrame) Attack();
                if (kb.qKey.wasPressedThisFrame) CastCharge();
            }

            if (Target != null && !Target.IsAlive)
            {
                Target = null; // server already cleared its auto-attack target
            }
            _ring.gameObject.SetActive(Target != null);
            if (Target != null)
            {
                var p = Target.transform.position;
                _ring.position = new Vector3(p.x, 0.03f, p.z);
            }
        }

        /// <summary>Lock the nearest alive mob (or keep the current one) and auto-attack it.</summary>
        public void Attack()
        {
            var local = _instance.LocalView;
            if (local == null || !local.IsAlive)
            {
                return;
            }
            if (Target == null || !Target.IsAlive)
            {
                Target = _instance.NearestAliveMob(local.transform.position, TargetSearchRadius);
            }
            if (Target != null)
            {
                _ws.Send(MessageTypes.AutoAttackTarget, new { targetEntityId = Target.Id });
            }
        }

        public void CastCharge()
        {
            var local = _instance.LocalView;
            if (local == null || !local.IsAlive)
            {
                return;
            }
            if (Target == null || !Target.IsAlive)
            {
                Target = _instance.NearestAliveMob(local.transform.position, ChargeRangeM);
            }
            if (Target == null)
            {
                LastSkillMessage = "Không có mục tiêu trong 8 m";
                return;
            }
            _ws.Send(MessageTypes.SkillCast, new { skillId = ChargeSkillId, targetEntityId = Target.Id });
            // Keep auto-attacking the same target after the skill.
            _ws.Send(MessageTypes.AutoAttackTarget, new { targetEntityId = Target.Id });
        }

        /// <summary>SKILL_RESULT from the server.</summary>
        public void OnSkillResult(JObject payload)
        {
            var success = (bool?)payload["success"] ?? false;
            var cooldownMs = (long?)payload["cooldownRemainingMs"] ?? 0;
            if ((string)payload["skillId"] == ChargeSkillId)
            {
                _chargeReadyAt = Time.time + cooldownMs / 1000f;
            }
            LastSkillMessage = success ? "Charge!" : "Charge thất bại: " + (string)payload["reason"];
        }

        public void ClearTarget()
        {
            Target = null;
        }

        private static Transform CreateTargetRing()
        {
            var ring = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            ring.name = "TargetRing";
            Destroy(ring.GetComponent<Collider>());
            ring.transform.localScale = new Vector3(1.6f, 0.02f, 1.6f);
            GreyboxRoom.Paint(ring, new Color(1f, 0.85f, 0.1f));
            ring.SetActive(false);
            return ring.transform;
        }
    }
}
