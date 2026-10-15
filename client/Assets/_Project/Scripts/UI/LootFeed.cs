using System.Collections.Generic;
using UnityEngine;

namespace DungeonDelvers.UI
{
    /// <summary>Short-lived loot notifications ("+2 Goblin Ear", "+10 gold") under the top-right buttons.</summary>
    public class LootFeed
    {
        private const float Lifetime = 4f;
        private const int MaxLines = 6;

        private readonly List<(string text, Color color, float expiresAt)> _lines = new List<(string, Color, float)>();

        public void Add(string text, Color color)
        {
            _lines.Add((text, color, Time.time + Lifetime));
            if (_lines.Count > MaxLines)
            {
                _lines.RemoveAt(0);
            }
        }

        public void Draw()
        {
            _lines.RemoveAll(l => l.expiresAt <= Time.time);
            if (_lines.Count == 0)
            {
                return;
            }
            GuiScale.Begin();
            var y = 110f;
            var previous = GUI.color;
            foreach (var (text, color, expiresAt) in _lines)
            {
                var c = color;
                c.a = Mathf.Clamp01((expiresAt - Time.time) / 1f); // fade out in the last second
                GUI.color = c;
                GUI.Label(new Rect(GuiScale.Width - 300f, y, 290f, 28f), text, GuiScale.Label);
                y += 26f;
            }
            GUI.color = previous;
        }

        public static Color RarityColor(string rarity) => rarity switch
        {
            "RARE" => new Color(0.35f, 0.6f, 1f),
            "EPIC" => new Color(0.75f, 0.4f, 1f),
            _ => Color.white
        };
    }
}
