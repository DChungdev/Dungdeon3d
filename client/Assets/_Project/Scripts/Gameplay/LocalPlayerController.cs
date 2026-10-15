using System;
using System.Collections.Generic;
using DungeonDelvers.Network;
using DungeonDelvers.UI;
using UnityEngine;

namespace DungeonDelvers.Gameplay
{
    /// <summary>
    /// Client-side prediction + server reconciliation for the local player (TDD §7.5).
    ///
    /// <para><c>_simPosition</c> is the predicted position (what we tell the server). Each MOVE_INTENT
    /// is recorded with its inputSeq; when ENTITY_UPDATE acknowledges seq N at position S, the
    /// error S − predicted(N) is applied to the prediction instantly, while the visible transform
    /// catches up smoothly (or snaps if the error is large).</para>
    /// </summary>
    public class LocalPlayerController : MonoBehaviour
    {
        /// <summary>Matches the server's Warrior moveSpeed (Phase 0 has only Warrior).</summary>
        private const float MoveSpeed = 5f;
        /// <summary>Slightly under 20 Hz so timer jitter never exceeds the server's 20/s limit.</summary>
        private const float SendInterval = 0.055f;
        private const float RoomHalfExtent = 20f;
        private const float CorrectionThreshold = 0.05f;
        private const float SnapThreshold = 1.5f;
        private const float SmoothCorrectionTime = 0.2f;

        public int CorrectionCount { get; private set; }
        public float LastCorrectionError { get; private set; }

        private readonly LinkedList<(long seq, Vector3 position)> _pending = new LinkedList<(long, Vector3)>();
        private WebSocketService _ws;
        private VirtualJoystick _input;
        private Vector3 _simPosition;
        private Vector3 _visualOffset;   // rendered = sim + offset; decays to zero
        private float _yaw;
        private float _sendTimer;
        private long _inputSeq;
        private bool _wasMoving;
        private bool _dead;

        public void Init(WebSocketService ws, VirtualJoystick input, Vector3 spawn, float yaw)
        {
            _ws = ws;
            _input = input;
            ResetTo(spawn, yaw);
        }

        /// <summary>Hard reset (instance enter / reconnect snapshot).</summary>
        public void ResetTo(Vector3 position, float yaw)
        {
            _simPosition = position;
            _visualOffset = Vector3.zero;
            _yaw = yaw;
            _pending.Clear();
            ApplyTransform();
        }

        /// <summary>
        /// Dead players can't move; inputs still in flight will never be acknowledged (the server
        /// rejects moves while dead), so they are dropped and the respawn position is taken as-is.
        /// </summary>
        public void SetDead(bool dead)
        {
            if (dead && !_dead)
            {
                _pending.Clear();
                _wasMoving = false;
            }
            _dead = dead;
        }

        private void Update()
        {
            var dt = Time.deltaTime;
            var stick = _input != null && !_dead ? _input.Value : Vector2.zero;
            var dir = new Vector3(stick.x, 0f, stick.y);
            var moving = dir.sqrMagnitude > 0.0001f;

            if (moving)
            {
                var next = _simPosition + dir * (MoveSpeed * dt);
                next.x = Mathf.Clamp(next.x, -RoomHalfExtent, RoomHalfExtent);
                next.z = Mathf.Clamp(next.z, -RoomHalfExtent, RoomHalfExtent);
                _simPosition = next;
                _yaw = Mathf.Atan2(dir.x, dir.z) * Mathf.Rad2Deg;
            }

            _sendTimer += dt;
            if (_sendTimer >= SendInterval)
            {
                _sendTimer = Mathf.Min(_sendTimer - SendInterval, SendInterval);
                // Send while moving, plus one final packet when stopping.
                if (moving || _wasMoving)
                {
                    SendIntent(moving ? dir * MoveSpeed : Vector3.zero);
                    _wasMoving = moving;
                }
            }

            // Visible position catches up with the corrected prediction.
            _visualOffset = Vector3.Lerp(_visualOffset, Vector3.zero, Mathf.Clamp01(dt / SmoothCorrectionTime));
            ApplyTransform();
        }

        private void SendIntent(Vector3 velocity)
        {
            _inputSeq++;
            _pending.AddLast((_inputSeq, _simPosition));
            if (_pending.Count > 200)
            {
                _pending.RemoveFirst(); // server stopped acking; don't grow forever
            }
            _ws.Send(MessageTypes.MoveIntent, new
            {
                position = NetMath.FromVector3(_simPosition),
                velocity = NetMath.FromVector3(velocity),
                rotation = NormalizeYaw(_yaw),
                inputSeq = _inputSeq
            });
        }

        /// <summary>Authoritative state for this player from ENTITY_UPDATE.</summary>
        public void OnServerState(Vector3 serverPosition, long lastInputSeq)
        {
            Vector3? predictedAtAck = null;
            while (_pending.Count > 0 && _pending.First.Value.seq <= lastInputSeq)
            {
                if (_pending.First.Value.seq == lastInputSeq)
                {
                    predictedAtAck = _pending.First.Value.position;
                }
                _pending.RemoveFirst();
            }

            Vector3 error;
            if (predictedAtAck.HasValue)
            {
                error = serverPosition - predictedAtAck.Value;
            }
            else if (_pending.Count == 0)
            {
                // Nothing in flight: the server position is simply where we should be.
                error = serverPosition - _simPosition;
            }
            else
            {
                return; // ack for an input we no longer track
            }
            error.y = 0f;
            if (error.magnitude <= CorrectionThreshold)
            {
                return;
            }

            CorrectionCount++;
            LastCorrectionError = error.magnitude;
            _simPosition += error;
            // Inputs still in flight were predicted from the wrong base; shift them too.
            for (var node = _pending.First; node != null; node = node.Next)
            {
                node.Value = (node.Value.seq, node.Value.position + error);
            }
            _visualOffset = error.magnitude > SnapThreshold ? Vector3.zero : _visualOffset - error;
        }

        private void ApplyTransform()
        {
            transform.SetPositionAndRotation(_simPosition + _visualOffset + Vector3.up, Quaternion.Euler(0f, _yaw, 0f));
        }

        private static float NormalizeYaw(float yaw)
        {
            yaw %= 360f;
            return yaw < 0 ? yaw + 360f : yaw;
        }
    }
}
