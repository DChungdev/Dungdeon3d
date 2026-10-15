using UnityEngine;

namespace DungeonDelvers.Gameplay
{
    /// <summary>World-space HP bar above an entity, facing the camera.</summary>
    public class HpBar : MonoBehaviour
    {
        private const float Width = 1.2f;
        private const float Height = 0.14f;

        private Transform _fill;
        private Camera _camera;

        public static HpBar Create(Transform parent, Color fillColor)
        {
            var root = new GameObject("HpBar");
            root.transform.SetParent(parent, false);
            root.transform.localPosition = new Vector3(0f, 1.3f, 0f);

            var background = Quad(root.transform, "Background", new Color(0.1f, 0.1f, 0.1f));
            background.localScale = new Vector3(Width + 0.06f, Height + 0.06f, 1f);

            var bar = root.AddComponent<HpBar>();
            bar._fill = Quad(root.transform, "Fill", fillColor);
            bar._fill.localPosition = new Vector3(0f, 0f, -0.01f); // in front of the background
            bar.SetFraction(1f);
            return bar;
        }

        public void SetFraction(float fraction)
        {
            fraction = Mathf.Clamp01(fraction);
            _fill.localScale = new Vector3(Width * fraction, Height, 1f);
            // Keep the bar left-aligned while it shrinks.
            _fill.localPosition = new Vector3(-Width * (1f - fraction) / 2f, 0f, -0.01f);
        }

        private void LateUpdate()
        {
            if (_camera == null)
            {
                _camera = Camera.main;
                if (_camera == null)
                {
                    return;
                }
            }
            transform.rotation = _camera.transform.rotation;
        }

        private static Transform Quad(Transform parent, string name, Color color)
        {
            var quad = GameObject.CreatePrimitive(PrimitiveType.Quad);
            quad.name = name;
            Destroy(quad.GetComponent<Collider>());
            quad.transform.SetParent(parent, false);
            GreyboxRoom.Paint(quad, color);
            return quad.transform;
        }
    }
}
