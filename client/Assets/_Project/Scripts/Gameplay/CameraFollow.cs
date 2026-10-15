using UnityEngine;

namespace DungeonDelvers.Gameplay
{
    /// <summary>Fixed-angle top-down follow camera. World +Z is "up" on screen, matching the joystick.</summary>
    public class CameraFollow : MonoBehaviour
    {
        private static readonly Vector3 Offset = new Vector3(0f, 14f, -9f);
        private const float Smoothing = 8f;

        public Transform Target { get; set; }

        private void LateUpdate()
        {
            var focus = Target != null ? Target.position : Vector3.zero;
            var desired = focus + Offset;
            transform.position = Vector3.Lerp(transform.position, desired, 1f - Mathf.Exp(-Smoothing * Time.deltaTime));
            transform.rotation = Quaternion.LookRotation(focus - desired, Vector3.up);
        }
    }
}
