using DungeonDelvers.Gameplay;
using UnityEngine;

namespace DungeonDelvers.UI
{
    /// <summary>
    /// Phase 0 combat HUD (IMGUI): local HP/MP bars, target info, Attack + Charge buttons on the
    /// right half of the screen (the left half belongs to the joystick).
    /// </summary>
    public class CombatHud
    {
        private const float ButtonSize = 110f;
        private const float Margin = 24f;

        private readonly CombatController _combat;
        private Texture2D _white;

        public CombatHud(CombatController combat)
        {
            _combat = combat;
        }

        public void Draw(EntityView local)
        {
            if (local == null)
            {
                return;
            }
            GuiScale.Begin();
            if (_white == null)
            {
                _white = Texture2D.whiteTexture;
            }

            var w = GuiScale.Width;
            var h = GuiScale.Height;

            // HP / MP bars, top centre.
            const float barW = 320f;
            var x = (w - barW) / 2f;
            Bar(new Rect(x, 12, barW, 22), (float)local.Hp / Mathf.Max(1, local.MaxHp),
                new Color(0.2f, 0.75f, 0.3f), $"HP {local.Hp}/{local.MaxHp}");
            Bar(new Rect(x, 38, barW, 16), (float)local.Mp / Mathf.Max(1, local.MaxMp),
                new Color(0.25f, 0.45f, 0.95f), $"MP {local.Mp}/{local.MaxMp}");

            var target = _combat.Target;
            if (target != null)
            {
                Bar(new Rect(x, 60, barW, 18), (float)target.Hp / Mathf.Max(1, target.MaxHp),
                    new Color(0.85f, 0.2f, 0.2f), $"{target.DisplayName} {target.Hp}/{target.MaxHp}");
            }

            if (!local.IsAlive)
            {
                GUI.Label(new Rect(x, 90, barW, 30), "<b>Bạn đã gục — hồi sinh sau vài giây…</b>", GuiScale.Label);
                return;
            }

            // Buttons, bottom right: Attack (big) and Charge above it.
            var attackRect = new Rect(w - Margin - ButtonSize, h - Margin - ButtonSize, ButtonSize, ButtonSize);
            if (GUI.Button(attackRect, "Đánh\n(Space)", GuiScale.Button))
            {
                _combat.Attack();
            }

            var cd = _combat.ChargeCooldownRemaining;
            var chargeRect = new Rect(attackRect.x - ButtonSize - 16f, attackRect.y + 20f, ButtonSize - 10f, ButtonSize - 10f);
            GUI.enabled = cd <= 0f && local.Mp >= 15;
            if (GUI.Button(chargeRect, cd > 0f ? $"Charge\n{cd:0.0}s" : "Charge\n(Q)", GuiScale.Button))
            {
                _combat.CastCharge();
            }
            GUI.enabled = true;

            if (!string.IsNullOrEmpty(_combat.LastSkillMessage))
            {
                GUI.Label(new Rect(chargeRect.x - 60f, chargeRect.y - 34f, 260f, 30f), _combat.LastSkillMessage,
                    GuiScale.Label);
            }
        }

        private void Bar(Rect rect, float fraction, Color color, string text)
        {
            var previous = GUI.color;
            GUI.color = new Color(0f, 0f, 0f, 0.6f);
            GUI.DrawTexture(rect, _white);
            GUI.color = color;
            GUI.DrawTexture(new Rect(rect.x, rect.y, rect.width * Mathf.Clamp01(fraction), rect.height), _white);
            GUI.color = previous;
            var style = new GUIStyle(GuiScale.Label) { alignment = TextAnchor.MiddleCenter, fontSize = 14 };
            GUI.Label(rect, text, style);
        }
    }
}
