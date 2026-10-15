using System;
using System.Text;
using UnityEngine;

namespace DungeonDelvers.UI
{
    /// <summary>
    /// Network debug overlay (unity-client-guide §11): RTT, last message, server tick, etc.
    /// Content comes from a callback so this class has no gameplay dependencies.
    /// </summary>
    public class DebugOverlay
    {
        private readonly Func<string> _content;
        private readonly StringBuilder _fpsText = new StringBuilder();
        private float _fpsTimer;
        private int _frames;
        private float _fps;

        public event Action LeaveClicked;

        public DebugOverlay(Func<string> content)
        {
            _content = content;
        }

        /// <summary>Call from Update.</summary>
        public void Tick()
        {
            _frames++;
            _fpsTimer += Time.unscaledDeltaTime;
            if (_fpsTimer >= 0.5f)
            {
                _fps = _frames / _fpsTimer;
                _frames = 0;
                _fpsTimer = 0f;
            }
        }

        public void Draw(bool showLeave)
        {
            GuiScale.Begin();
            _fpsText.Clear().Append("FPS ").Append(_fps.ToString("0")).Append('\n').Append(_content());
            GUI.Box(new Rect(8, 8, 330, 190), GUIContent.none);
            GUI.Label(new Rect(16, 12, 320, 186), _fpsText.ToString(), GuiScale.Label);

            if (showLeave && GUI.Button(new Rect(GuiScale.Width - 148, 8, 140, 44), "Rời phòng", GuiScale.Button))
            {
                LeaveClicked?.Invoke();
            }
        }
    }
}
