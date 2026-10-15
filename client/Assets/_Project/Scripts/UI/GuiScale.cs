using UnityEngine;

namespace DungeonDelvers.UI
{
    /// <summary>
    /// Scales IMGUI so debug screens are usable on phones: layout is done in a virtual
    /// screen 720 units tall, whatever the real resolution.
    /// </summary>
    public static class GuiScale
    {
        private const float VirtualHeight = 720f;

        public static float Factor => Mathf.Max(1f, Screen.height / VirtualHeight);
        public static float Width => Screen.width / Factor;
        public static float Height => Screen.height / Factor;

        private static GUIStyle _label;
        private static GUIStyle _button;
        private static GUIStyle _field;

        /// <summary>Call at the start of OnGUI.</summary>
        public static void Begin()
        {
            GUI.matrix = Matrix4x4.Scale(new Vector3(Factor, Factor, 1f));
            if (_label == null)
            {
                _label = new GUIStyle(GUI.skin.label) { fontSize = 18, wordWrap = true };
                _button = new GUIStyle(GUI.skin.button) { fontSize = 20 };
                _field = new GUIStyle(GUI.skin.textField) { fontSize = 20 };
            }
        }

        public static GUIStyle Label => _label;
        public static GUIStyle Button => _button;
        public static GUIStyle Field => _field;
    }
}
