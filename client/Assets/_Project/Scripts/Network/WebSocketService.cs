using System;
using System.Collections.Concurrent;
using System.IO;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using UnityEngine;

namespace DungeonDelvers.Network
{
    /// <summary>
    /// Game WebSocket (network-protocol.md). Receives on a background task into a queue;
    /// <see cref="Tick"/> drains it on the main thread, so all events fire on the main thread.
    /// </summary>
    public class WebSocketService : IDisposable
    {
        public enum ConnectionState { Disconnected, Connecting, Connected }

        /// <summary>Server message: (type, payload).</summary>
        public event Action<string, JObject> MessageReceived;

        /// <summary>Raised once per connection when it closes or fails (with a reason for logs).</summary>
        public event Action<string> Disconnected;

        public ConnectionState State { get; private set; } = ConnectionState.Disconnected;
        public string LastMessageType { get; private set; }

        private readonly ConcurrentQueue<Action> _mainThreadQueue = new ConcurrentQueue<Action>();
        private readonly SemaphoreSlim _sendLock = new SemaphoreSlim(1, 1);
        private ClientWebSocket _socket;
        private CancellationTokenSource _cts;
        private long _seq;
        private int _generation;

        public async Task ConnectAsync(string url)
        {
            Close();
            var generation = ++_generation;
            var cts = new CancellationTokenSource();
            var socket = new ClientWebSocket();
            _cts = cts;
            _socket = socket;
            _seq = 0; // server expects seq to restart at 1 on every connection
            State = ConnectionState.Connecting;

            try
            {
                await socket.ConnectAsync(new Uri(url), cts.Token);
            }
            catch (Exception ex)
            {
                if (generation == _generation)
                {
                    State = ConnectionState.Disconnected;
                }
                throw new IOException("WebSocket connect failed: " + ex.Message, ex);
            }
            if (generation != _generation)
            {
                // Close() was called while connecting.
                socket.Abort();
                throw new OperationCanceledException("Connection superseded");
            }

            State = ConnectionState.Connected;
            _ = Task.Run(() => ReceiveLoop(socket, generation, cts.Token));
        }

        /// <summary>Sends <c>{type, seq, ts, payload}</c>. Silently dropped if not connected.</summary>
        public void Send(string type, object payload = null)
        {
            if (State != ConnectionState.Connected)
            {
                return;
            }
            var envelope = new JObject
            {
                ["type"] = type,
                ["seq"] = ++_seq,
                ["ts"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                ["payload"] = payload == null ? new JObject() : JObject.FromObject(payload)
            };
            _ = SendRawAsync(envelope.ToString(Formatting.None), _socket);
        }

        /// <summary>Call every frame from the main thread.</summary>
        public void Tick()
        {
            while (_mainThreadQueue.TryDequeue(out var action))
            {
                action();
            }
        }

        public void Close()
        {
            _generation++; // ignore events from the old socket
            _cts?.Cancel();
            if (_socket != null && _socket.State == WebSocketState.Open)
            {
                _ = _socket.CloseOutputAsync(WebSocketCloseStatus.NormalClosure, "bye", CancellationToken.None);
            }
            _socket = null;
            State = ConnectionState.Disconnected;
        }

        public void Dispose() => Close();

        private async Task SendRawAsync(string json, ClientWebSocket socket)
        {
            var bytes = Encoding.UTF8.GetBytes(json);
            await _sendLock.WaitAsync();
            try
            {
                if (socket != null && socket.State == WebSocketState.Open)
                {
                    await socket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true,
                        CancellationToken.None);
                }
            }
            catch (Exception ex)
            {
                Debug.LogWarning("[WS] send failed: " + ex.Message);
            }
            finally
            {
                _sendLock.Release();
            }
        }

        private async Task ReceiveLoop(ClientWebSocket socket, int generation, CancellationToken token)
        {
            var buffer = new byte[8192];
            var message = new MemoryStream();
            var reason = "closed";
            try
            {
                while (!token.IsCancellationRequested && socket.State == WebSocketState.Open)
                {
                    var result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), token)
                        .ConfigureAwait(false);
                    if (result.MessageType == WebSocketMessageType.Close)
                    {
                        reason = "server closed (" + result.CloseStatus + ")";
                        break;
                    }
                    message.Write(buffer, 0, result.Count);
                    if (!result.EndOfMessage)
                    {
                        continue;
                    }
                    var text = Encoding.UTF8.GetString(message.GetBuffer(), 0, (int)message.Length);
                    message.SetLength(0);
                    _mainThreadQueue.Enqueue(() => Dispatch(text, generation));
                }
            }
            catch (OperationCanceledException)
            {
                reason = "cancelled";
            }
            catch (Exception ex)
            {
                reason = ex.Message;
            }

            _mainThreadQueue.Enqueue(() =>
            {
                if (generation != _generation)
                {
                    return;
                }
                State = ConnectionState.Disconnected;
                Disconnected?.Invoke(reason);
            });
        }

        private void Dispatch(string text, int generation)
        {
            if (generation != _generation)
            {
                return;
            }
            JObject envelope;
            try
            {
                envelope = JObject.Parse(text);
            }
            catch (JsonException)
            {
                Debug.LogWarning("[WS] unparseable message: " + text);
                return;
            }
            var type = (string)envelope["type"];
            LastMessageType = type;
            if ((bool?)envelope["ackRequired"] == true)
            {
                // Reliable events (LOOT_DROP, INSTANCE_COMPLETE): network-protocol §2.3.
                Send(MessageTypes.Ack, new { ackSeq = (long?)envelope["seq"] ?? 0 });
            }
            MessageReceived?.Invoke(type, envelope["payload"] as JObject ?? new JObject());
        }
    }
}
