using System.Collections.Generic;
using UnityEngine;

namespace DungeonDelvers.Gameplay
{
    /// <summary>
    /// Renders another entity <see cref="InterpolationDelay"/> seconds in the past, interpolating
    /// between buffered server states so 10 Hz updates look smooth.
    /// </summary>
    public class RemoteEntityView : MonoBehaviour
    {
        /// <summary>Two server ticks at 10 Hz: there is almost always a newer state to lerp to.</summary>
        private const float InterpolationDelay = 0.2f;
        private const int MaxBuffered = 32;

        private readonly List<(float time, Vector3 position, float yaw)> _buffer = new List<(float, Vector3, float)>();

        public string State { get; private set; } = "IDLE";

        public void Init(Vector3 position, float yaw)
        {
            _buffer.Clear();
            _buffer.Add((Time.time, position, yaw));
            Apply(position, yaw);
        }

        public void PushState(Vector3 position, float yaw, string state)
        {
            _buffer.Add((Time.time, position, yaw));
            if (_buffer.Count > MaxBuffered)
            {
                _buffer.RemoveAt(0);
            }
            if (state != null)
            {
                State = state;
            }
        }

        private void Update()
        {
            var renderTime = Time.time - InterpolationDelay;

            // Drop samples older than the pair we interpolate between.
            while (_buffer.Count >= 2 && _buffer[1].time <= renderTime)
            {
                _buffer.RemoveAt(0);
            }

            if (_buffer.Count >= 2 && _buffer[0].time <= renderTime)
            {
                var a = _buffer[0];
                var b = _buffer[1];
                var t = Mathf.InverseLerp(a.time, b.time, renderTime);
                Apply(Vector3.Lerp(a.position, b.position, t), Mathf.LerpAngle(a.yaw, b.yaw, t));
            }
            else if (_buffer.Count > 0)
            {
                // Either the only sample, or renderTime hasn't reached the oldest one yet.
                Apply(_buffer[0].position, _buffer[0].yaw);
            }
        }

        private void Apply(Vector3 position, float yaw)
        {
            transform.SetPositionAndRotation(position + Vector3.up, Quaternion.Euler(0f, yaw, 0f));
        }
    }
}
