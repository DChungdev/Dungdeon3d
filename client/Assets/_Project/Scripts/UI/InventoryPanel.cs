using System;
using DungeonDelvers.Network;
using UnityEngine;

namespace DungeonDelvers.UI
{
    /// <summary>
    /// Phase 0 debug inventory: list of items from GET /characters/{id}/inventory. Data is loaded by
    /// the bootstrap (on open and after leaving an instance); this only draws it.
    /// </summary>
    public class InventoryPanel
    {
        public bool Visible { get; set; }
        public string Status { get; set; } = "";

        public event Action CloseClicked;
        public event Action RefreshClicked;

        private InventoryDto _inventory;
        private Vector2 _scroll;

        public void SetInventory(InventoryDto inventory)
        {
            _inventory = inventory;
            Status = "";
        }

        public void Draw()
        {
            if (!Visible)
            {
                return;
            }
            GuiScale.Begin();
            const float w = 460f;
            var h = Mathf.Min(520f, GuiScale.Height - 80f);
            var rect = new Rect((GuiScale.Width - w) / 2f, 60f, w, h);
            GUI.Box(rect, GUIContent.none);
            GUI.Box(rect, GUIContent.none); // double box = darker background

            GUILayout.BeginArea(new Rect(rect.x + 12, rect.y + 8, w - 24, h - 16));
            GUILayout.BeginHorizontal();
            var used = _inventory?.items?.Count ?? 0;
            var capacity = _inventory?.capacity ?? 40;
            GUILayout.Label($"<b>Túi đồ</b>  {used}/{capacity} ô   Gold: {_inventory?.gold ?? 0}", GuiScale.Label);
            if (GUILayout.Button("Tải lại", GuiScale.Button, GUILayout.Width(90), GUILayout.Height(36)))
            {
                RefreshClicked?.Invoke();
            }
            if (GUILayout.Button("Đóng", GuiScale.Button, GUILayout.Width(80), GUILayout.Height(36)))
            {
                CloseClicked?.Invoke();
            }
            GUILayout.EndHorizontal();

            if (!string.IsNullOrEmpty(Status))
            {
                GUILayout.Label(Status, GuiScale.Label);
            }

            _scroll = GUILayout.BeginScrollView(_scroll);
            if (_inventory?.items != null)
            {
                if (_inventory.items.Count == 0)
                {
                    GUILayout.Label("(trống — đánh quái để nhặt đồ)", GuiScale.Label);
                }
                var previous = GUI.color;
                foreach (var item in _inventory.items)
                {
                    GUI.color = LootFeed.RarityColor(item.rarity);
                    var qty = item.quantity > 1 ? $" x{item.quantity}" : "";
                    GUILayout.Label($"[{item.slotIndex:00}] {item.name}{qty}   <i>{item.rarity.ToLower()}</i>", GuiScale.Label);
                }
                GUI.color = previous;
            }
            GUILayout.EndScrollView();
            GUILayout.EndArea();
        }
    }
}
