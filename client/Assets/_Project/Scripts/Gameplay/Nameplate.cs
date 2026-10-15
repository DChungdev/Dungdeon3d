using TMPro;
using UnityEngine;

namespace DungeonDelvers.Gameplay
{
    /// <summary>World-space name label above an entity, always facing the camera.</summary>
    public class Nameplate : MonoBehaviour
    {
        private Camera _camera;

        public static Nameplate Create(Transform parent, string text, Color color)
        {
            var go = new GameObject("Nameplate");
            go.transform.SetParent(parent, false);
            go.transform.localPosition = new Vector3(0f, 1.6f, 0f);

            var label = go.AddComponent<TextMeshPro>();
            label.text = text;
            label.fontSize = 4f;
            label.color = color;
            label.alignment = TextAlignmentOptions.Center;
            label.rectTransform.sizeDelta = new Vector2(6f, 1f);

            return go.AddComponent<Nameplate>();
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
    }
}
