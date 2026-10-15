using System;
using System.Threading.Tasks;
using DungeonDelvers.Gameplay;
using DungeonDelvers.Network;
using DungeonDelvers.UI;
using Newtonsoft.Json.Linq;
using UnityEngine;

namespace DungeonDelvers.Core
{
    /// <summary>
    /// Phase 0 entry point: put this on one GameObject in the Boot scene and press Play.
    /// Flow: login/register → (create Warrior) → WebSocket → JOIN_INSTANCE → play.
    /// Owns heartbeat (PING every 5 s) and auto-reconnect with backoff (TDD §11.4).
    /// </summary>
    public class GameBootstrap : MonoBehaviour
    {
        private enum Phase { Login, Connecting, InGame, Reconnecting }

        private const string DungeonId = "dungeon_goblin_cave";
        private const float PingInterval = 5f;
        private const int MaxMissedPongs = 3;
        private const float MaxBackoffSec = 30f;
        private static readonly Color ErrorColor = new Color(1f, 0.4f, 0.4f);

        private RestApiService _rest;
        private WebSocketService _ws;
        private InstanceController _instance;
        private CombatController _combat;
        private VirtualJoystick _joystick;
        private LoginScreen _login;
        private DebugOverlay _overlay;
        private CombatHud _hud;
        private LootFeed _lootFeed;
        private InventoryPanel _inventoryPanel;

        private Phase _phase = Phase.Login;
        private CharacterDto _character;
        private string _status = "";
        private float _pingTimer;
        private float _pingSentAt = -1f;
        private int _missedPongs;
        private float _rttMs = -1f;
        private int _reconnectAttempt;
        private bool _resumeInstance; // after reconnect, ask for the old room instead of a new one

        private void Awake()
        {
            Application.targetFrameRate = 60;
            Application.runInBackground = true; // keep heartbeat alive when the window loses focus

            _rest = new RestApiService();
            _ws = new WebSocketService();
            _ws.MessageReceived += OnMessage;
            _ws.Disconnected += OnDisconnected;

            var cameraFollow = SetUpCamera();
            EnsureLight();

            _joystick = gameObject.AddComponent<VirtualJoystick>();
            _joystick.enabled = false;
            _instance = gameObject.AddComponent<InstanceController>();
            _combat = gameObject.AddComponent<CombatController>();
            _combat.Init(_ws, _instance);
            _combat.enabled = false;
            _instance.Init(_ws, _joystick, cameraFollow, _combat);
            _hud = new CombatHud(_combat);

            _login = new LoginScreen(ServerConfig.RestBaseUrl);
            _login.LoginClicked += () => Authenticate(register: false);
            _login.RegisterClicked += () => Authenticate(register: true);
            _login.CreateCharacterClicked += CreateCharacter;

            _overlay = new DebugOverlay(OverlayText);
            _overlay.LeaveClicked += LeaveInstance;

            _lootFeed = new LootFeed();
            _inventoryPanel = new InventoryPanel();
            _inventoryPanel.CloseClicked += () => _inventoryPanel.Visible = false;
            _inventoryPanel.RefreshClicked += ReloadInventory;
        }

        private void Update()
        {
            _ws.Tick();
            _overlay.Tick();
            if (_phase == Phase.InGame)
            {
                TickHeartbeat();
            }
        }

        private void OnGUI()
        {
            if (_phase == Phase.Login)
            {
                _login.Draw();
            }
            else
            {
                if (_phase == Phase.InGame)
                {
                    _hud.Draw(_instance.LocalView);
                    _lootFeed.Draw();
                }
                _overlay.Draw(showLeave: _phase == Phase.InGame && _instance.InstanceId != null);
                if (_phase == Phase.InGame)
                {
                    GuiScale.Begin();
                    if (GUI.Button(new Rect(GuiScale.Width - 296, 8, 140, 44), "Túi đồ", GuiScale.Button))
                    {
                        ToggleInventory();
                    }
                    _inventoryPanel.Draw();
                }
            }
        }

        private void OnDestroy()
        {
            _ws?.Dispose();
        }

        // ---------------------------------------------------------------- login flow

        private async void Authenticate(bool register)
        {
            ServerConfig.RestBaseUrl = _login.ServerUrl;
            _login.RememberEmail();
            _login.Busy = true;
            try
            {
                if (register)
                {
                    await _rest.RegisterAsync(_login.Email.Trim(), _login.Password);
                }
                else
                {
                    await _rest.LoginAsync(_login.Email.Trim(), _login.Password);
                }
                var characters = await _rest.ListCharactersAsync();
                if (characters.Count == 0)
                {
                    _login.CurrentMode = LoginScreen.Mode.CreateCharacter;
                    _login.Status = "";
                    return;
                }
                _character = characters[0];
                await ConnectAndJoin();
            }
            catch (ApiException ex)
            {
                _login.Status = "Lỗi: " + (ex.Code == "UNAUTHORIZED" ? "Sai email hoặc mật khẩu" : ErrorMessages.ForCode(ex.Code, ex.Message));
            }
            finally
            {
                _login.Busy = false;
            }
        }

        private async void CreateCharacter()
        {
            _login.Busy = true;
            try
            {
                _character = await _rest.CreateCharacterAsync(_login.CharacterName.Trim(), "WARRIOR");
                await ConnectAndJoin();
            }
            catch (ApiException ex)
            {
                _login.Status = "Lỗi: " + (ex.Code == "UNAUTHORIZED" ? "Sai email hoặc mật khẩu" : ErrorMessages.ForCode(ex.Code, ex.Message));
            }
            finally
            {
                _login.Busy = false;
            }
        }

        private async Task ConnectAndJoin()
        {
            _phase = Phase.Connecting;
            _status = "Đang kết nối WebSocket…";
            _resumeInstance = false;
            try
            {
                await _ws.ConnectAsync(ServerConfig.WsUrl(_rest.Tokens.accessToken) + "&characterId=" + _character.id);
            }
            catch (Exception ex)
            {
                BackToLogin("Không kết nối được WebSocket: " + ex.Message);
            }
            // JOIN_INSTANCE is sent when CONNECTED arrives (OnMessage).
        }

        private void BackToLogin(string message)
        {
            _ws.Close();
            _instance.Clear();
            _joystick.enabled = false;
            _combat.enabled = false;
            _phase = Phase.Login;
            _login.CurrentMode = LoginScreen.Mode.Login;
            _login.Status = message;
        }

        // ---------------------------------------------------------------- messages

        private void OnMessage(string type, JObject payload)
        {
            if (_instance.HandleMessage(type, payload))
            {
                if (type == MessageTypes.InstanceEnter)
                {
                    _phase = Phase.InGame;
                    _joystick.enabled = true;
                    _combat.enabled = true;
                    _resumeInstance = false;
                    _status = "";
                }
                return;
            }

            switch (type)
            {
                case MessageTypes.Connected:
                    _reconnectAttempt = 0;
                    _missedPongs = 0;
                    _pingSentAt = -1f;
                    _pingTimer = 0f;
                    if (_resumeInstance && _instance.InstanceId != null)
                    {
                        _ws.Send(MessageTypes.Reconnect, new
                        {
                            zoneType = "INSTANCE",
                            zoneId = _instance.InstanceId,
                            lastSeq = 0
                        });
                    }
                    else
                    {
                        JoinInstance();
                    }
                    break;

                case MessageTypes.Pong:
                    if (_pingSentAt >= 0f)
                    {
                        _rttMs = (Time.realtimeSinceStartup - _pingSentAt) * 1000f;
                        _pingSentAt = -1f;
                    }
                    _missedPongs = 0;
                    break;

                case MessageTypes.Error:
                    var code = (string)payload["code"];
                    Debug.LogWarning($"[WS] ERROR {code}: {(string)payload["message"]}");
                    if (code == "INSTANCE_NOT_FOUND" && _resumeInstance)
                    {
                        // Old room is gone (e.g. destroyed while we were away): start fresh.
                        _resumeInstance = false;
                        _instance.Clear();
                        JoinInstance();
                    }
                    else
                    {
                        // Toast instead of a debug line (checklist week 6: client error handling).
                        _lootFeed.Add(ErrorMessages.ForCode(code, (string)payload["message"]), ErrorColor);
                    }
                    break;

                case MessageTypes.LootDrop:
                    OnLootDrop(payload);
                    break;

                case MessageTypes.Kick:
                    BackToLogin($"Bị ngắt kết nối: {(string)payload["message"]}");
                    break;

                default:
                    Debug.Log($"[WS] unhandled {type}");
                    break;
            }
        }

        private void JoinInstance()
        {
            _status = "Đang vào phòng…";
            _ws.Send(MessageTypes.JoinInstance, new { dungeonId = DungeonId, difficulty = "NORMAL" });
        }

        private void OnLootDrop(JObject payload)
        {
            if (payload["items"] is JArray items)
            {
                foreach (var item in items)
                {
                    var qty = (int?)item["quantity"] ?? 1;
                    _lootFeed.Add($"+{qty} {(string)item["name"]}", LootFeed.RarityColor((string)item["rarity"]));
                }
            }
            var gold = (int?)payload["gold"] ?? 0;
            if (gold > 0)
            {
                _lootFeed.Add($"+{gold} gold  (tổng {(int?)payload["goldTotal"] ?? 0})", new Color(1f, 0.85f, 0.2f));
            }
            if (_inventoryPanel.Visible)
            {
                ReloadInventory();
            }
        }

        private void ToggleInventory()
        {
            _inventoryPanel.Visible = !_inventoryPanel.Visible;
            if (_inventoryPanel.Visible)
            {
                ReloadInventory();
            }
        }

        /// <summary>Inventory is authoritative in the DB; always read it fresh over REST.</summary>
        private async void ReloadInventory()
        {
            if (_character == null)
            {
                return;
            }
            _inventoryPanel.Status = "Đang tải…";
            try
            {
                _inventoryPanel.SetInventory(await _rest.GetInventoryAsync(_character.id));
            }
            catch (ApiException ex)
            {
                _inventoryPanel.Status = "Lỗi tải túi đồ: " + ErrorMessages.ForCode(ex.Code, ex.Message);
            }
        }

        private void LeaveInstance()
        {
            // Checklist: instance exit → reload inventory over REST.
            _inventoryPanel.Visible = true;
            ReloadInventory();
            _ws.Send(MessageTypes.LeaveInstance);
            _instance.Clear();
            _joystick.enabled = false;
            _combat.enabled = false;
            _status = "Đã rời phòng. Đang vào lại…";
            JoinInstance();
        }

        // ---------------------------------------------------------------- heartbeat & reconnect

        private void TickHeartbeat()
        {
            _pingTimer += Time.unscaledDeltaTime;
            if (_pingTimer < PingInterval)
            {
                return;
            }
            _pingTimer = 0f;
            if (_pingSentAt >= 0f && ++_missedPongs >= MaxMissedPongs)
            {
                Debug.LogWarning("[WS] missed 3 PONGs, reconnecting");
                _ws.Close();
                OnDisconnected("heartbeat timeout");
                return;
            }
            _pingSentAt = Time.realtimeSinceStartup;
            _ws.Send(MessageTypes.Ping);
        }

        private void OnDisconnected(string reason)
        {
            if (_phase == Phase.Login || _phase == Phase.Reconnecting)
            {
                return;
            }
            Debug.LogWarning("[WS] disconnected: " + reason);
            _phase = Phase.Reconnecting;
            _joystick.enabled = false;
            _combat.enabled = false;
            _ = ReconnectLoop();
        }

        private async Task ReconnectLoop()
        {
            while (_phase == Phase.Reconnecting && this != null)
            {
                var delay = Mathf.Min(Mathf.Pow(2, _reconnectAttempt), MaxBackoffSec); // 1, 2, 4, 8 … 30 s
                _reconnectAttempt++;
                _status = $"Mất kết nối. Thử lại sau {delay:0}s (lần {_reconnectAttempt})…";
                await Task.Delay(TimeSpan.FromSeconds(delay));
                if (_phase != Phase.Reconnecting || this == null)
                {
                    return;
                }

                try
                {
                    // Access tokens live 15 min; always refresh before reconnecting.
                    await _rest.RefreshAsync();
                }
                catch (ApiException ex) when (ex.Code != "NETWORK_ERROR")
                {
                    BackToLogin("Phiên đăng nhập hết hạn, hãy đăng nhập lại.");
                    return;
                }
                catch (ApiException)
                {
                    continue; // server unreachable, try again
                }

                try
                {
                    _status = "Đang kết nối lại…";
                    _resumeInstance = true;
                    await _ws.ConnectAsync(ServerConfig.WsUrl(_rest.Tokens.accessToken) + "&characterId=" + _character.id);
                    _phase = Phase.Connecting; // CONNECTED → RECONNECT → INSTANCE_ENTER → InGame
                    return;
                }
                catch (Exception ex)
                {
                    Debug.LogWarning("[WS] reconnect failed: " + ex.Message);
                }
            }
        }

        // ---------------------------------------------------------------- scene setup & overlay

        private static CameraFollow SetUpCamera()
        {
            var cam = Camera.main;
            if (cam == null)
            {
                cam = new GameObject("Main Camera") { tag = "MainCamera" }.AddComponent<Camera>();
            }
            cam.clearFlags = CameraClearFlags.SolidColor;
            cam.backgroundColor = new Color(0.08f, 0.08f, 0.1f);
            // Not "??": Unity's fake-null objects from GetComponent defeat the null-coalescing operator.
            var follow = cam.gameObject.GetComponent<CameraFollow>();
            return follow != null ? follow : cam.gameObject.AddComponent<CameraFollow>();
        }

        private static void EnsureLight()
        {
            if (FindObjectOfType<Light>() != null)
            {
                return;
            }
            var light = new GameObject("Directional Light").AddComponent<Light>();
            light.type = LightType.Directional;
            light.transform.rotation = Quaternion.Euler(50f, -30f, 0f);
        }

        private string OverlayText()
        {
            var local = _instance.LocalPlayer;
            var pos = local != null ? local.transform.position : Vector3.zero;
            return
                $"{_phase}  WS: {_ws.State}\n" +
                $"RTT: {(_rttMs < 0 ? "-" : _rttMs.ToString("0") + " ms")}\n" +
                $"Last msg: {_ws.LastMessageType ?? "-"}\n" +
                $"Server tick: {_instance.ServerTick}  Entities: {_instance.EntityCount}\n" +
                $"Pos: {pos.x:0.0}, {pos.z:0.0}\n" +
                $"Corrections: {(local != null ? local.CorrectionCount : 0)}" +
                (local != null && local.CorrectionCount > 0 ? $" (last {local.LastCorrectionError:0.00} m)" : "") +
                (string.IsNullOrEmpty(_status) ? "" : "\n" + _status);
        }
    }
}
