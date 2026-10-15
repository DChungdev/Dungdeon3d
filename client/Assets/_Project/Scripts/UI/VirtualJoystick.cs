using UnityEngine;
using UnityEngine.InputSystem;

namespace DungeonDelvers.UI
{
    /// <summary>
    /// Floating joystick on the left half of the screen (touch, or mouse in the Editor),
    /// combined with WASD / arrow keys. <see cref="Value"/> is the move direction, length ≤ 1.
    /// </summary>
    public class VirtualJoystick : MonoBehaviour
    {
        private const float RadiusInches = 0.5f;
        private const float DeadZone = 0.15f;

        public Vector2 Value { get; private set; }

        private int _touchId = -1;      // active touch, or -1; mouse uses id 0 with _mouseActive
        private bool _mouseActive;
        private Vector2 _origin;        // screen pixels, bottom-left origin (Input System)
        private Vector2 _current;
        private Texture2D _circle;

        private float RadiusPx => (Screen.dpi > 0 ? Screen.dpi : 160f) * RadiusInches;

        private void Awake()
        {
            _circle = MakeCircleTexture(128);
        }

        private void Update()
        {
            var stick = ReadPointerStick();
            var keys = ReadKeyboard();
            var v = keys.sqrMagnitude > 0 ? keys : stick;
            Value = v.magnitude < DeadZone ? Vector2.zero : Vector2.ClampMagnitude(v, 1f);
        }

        private Vector2 ReadPointerStick()
        {
            var touchscreen = Touchscreen.current;
            if (touchscreen != null)
            {
                if (_touchId < 0)
                {
                    foreach (var touch in touchscreen.touches)
                    {
                        if (touch.press.wasPressedThisFrame && touch.position.ReadValue().x < Screen.width * 0.5f)
                        {
                            _touchId = touch.touchId.ReadValue();
                            _origin = _current = touch.position.ReadValue();
                            break;
                        }
                    }
                }
                else
                {
                    var stillDown = false;
                    foreach (var touch in touchscreen.touches)
                    {
                        if (touch.touchId.ReadValue() == _touchId && touch.press.isPressed)
                        {
                            _current = touch.position.ReadValue();
                            stillDown = true;
                            break;
                        }
                    }
                    if (!stillDown)
                    {
                        _touchId = -1;
                    }
                }
                if (_touchId >= 0)
                {
                    return StickVector();
                }
            }

            var mouse = Mouse.current;
            if (mouse != null)
            {
                if (!_mouseActive && mouse.leftButton.wasPressedThisFrame
                    && mouse.position.ReadValue().x < Screen.width * 0.5f
                    && !IsOverGuiButtonArea(mouse.position.ReadValue()))
                {
                    _mouseActive = true;
                    _origin = _current = mouse.position.ReadValue();
                }
                else if (_mouseActive && !mouse.leftButton.isPressed)
                {
                    _mouseActive = false;
                }
                if (_mouseActive)
                {
                    _current = mouse.position.ReadValue();
                    return StickVector();
                }
            }
            return Vector2.zero;
        }

        private static Vector2 ReadKeyboard()
        {
            var kb = Keyboard.current;
            if (kb == null)
            {
                return Vector2.zero;
            }
            var v = Vector2.zero;
            if (kb.wKey.isPressed || kb.upArrowKey.isPressed) v.y += 1;
            if (kb.sKey.isPressed || kb.downArrowKey.isPressed) v.y -= 1;
            if (kb.dKey.isPressed || kb.rightArrowKey.isPressed) v.x += 1;
            if (kb.aKey.isPressed || kb.leftArrowKey.isPressed) v.x -= 1;
            return v.normalized;
        }

        private Vector2 StickVector() => Vector2.ClampMagnitude((_current - _origin) / RadiusPx, 1f);

        /// <summary>Top strip is used by the debug overlay buttons; don't start a stick there.</summary>
        private static bool IsOverGuiButtonArea(Vector2 screenPos) => screenPos.y > Screen.height * 0.85f;

        private void OnGUI()
        {
            if (_touchId < 0 && !_mouseActive)
            {
                return;
            }
            var r = RadiusPx;
            var knob = _origin + Vector2.ClampMagnitude(_current - _origin, r);
            DrawCircle(_origin, r, new Color(1f, 1f, 1f, 0.15f));
            DrawCircle(knob, r * 0.45f, new Color(1f, 1f, 1f, 0.5f));
        }

        private void DrawCircle(Vector2 centerBottomLeft, float radius, Color color)
        {
            var guiCenter = new Vector2(centerBottomLeft.x, Screen.height - centerBottomLeft.y);
            var previous = GUI.color;
            GUI.color = color;
            GUI.DrawTexture(new Rect(guiCenter.x - radius, guiCenter.y - radius, radius * 2, radius * 2), _circle);
            GUI.color = previous;
        }

        private static Texture2D MakeCircleTexture(int size)
        {
            var tex = new Texture2D(size, size, TextureFormat.RGBA32, false) { wrapMode = TextureWrapMode.Clamp };
            var c = (size - 1) / 2f;
            for (var y = 0; y < size; y++)
            {
                for (var x = 0; x < size; x++)
                {
                    var d = Mathf.Sqrt((x - c) * (x - c) + (y - c) * (y - c));
                    tex.SetPixel(x, y, new Color(1, 1, 1, Mathf.Clamp01(c - d)));
                }
            }
            tex.Apply();
            return tex;
        }
    }
}
