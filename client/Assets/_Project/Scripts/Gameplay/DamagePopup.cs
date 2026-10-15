using TMPro;
using UnityEngine;

namespace DungeonDelvers.Gameplay
{
    /// <summary>Floating damage number (cosmetic only, driven by DAMAGE_EVENT).</summary>
    public class DamagePopup : MonoBehaviour
    {
        private const float Lifetime = 0.9f;
        private const float RiseSpeed = 1.6f;

        private TextMeshPro _label;
        private Camera _camera;
        private float _age;
        private Color _color;

        public static void Spawn(Vector3 worldPosition, int amount, bool crit, Color color)
        {
            var go = new GameObject("DamagePopup");
            // Small random sideways offset so consecutive hits don't overlap.
            go.transform.position = worldPosition + new Vector3(Random.Range(-0.3f, 0.3f), 2.1f, 0f);
            var popup = go.AddComponent<DamagePopup>();
            popup._label = go.AddComponent<TextMeshPro>();
            popup._label.text = crit ? amount + "!" : amount.ToString();
            popup._label.fontSize = crit ? 7f : 5f;
            popup._label.fontStyle = crit ? FontStyles.Bold : FontStyles.Normal;
            popup._label.alignment = TextAlignmentOptions.Center;
            popup._color = crit ? new Color(1f, 0.85f, 0.1f) : color;
            popup._label.color = popup._color;
        }

        private void Update()
        {
            _age += Time.deltaTime;
            if (_age >= Lifetime)
            {
                Destroy(gameObject);
                return;
            }
            transform.position += Vector3.up * (RiseSpeed * Time.deltaTime);
            var c = _color;
            c.a = 1f - _age / Lifetime;
            _label.color = c;

            if (_camera == null)
            {
                _camera = Camera.main;
            }
            if (_camera != null)
            {
                transform.rotation = _camera.transform.rotation;
            }
        }
    }
}
