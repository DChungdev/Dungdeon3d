using System;
using UnityEngine;

namespace DungeonDelvers.UI
{
    /// <summary>
    /// Phase 0 debug login / character creation screen (IMGUI). Holds only form state;
    /// actions are callbacks into <see cref="Core.GameBootstrap"/>.
    /// </summary>
    public class LoginScreen
    {
        public enum Mode { Login, CreateCharacter }

        private const string EmailPref = "ddo.email";

        public Mode CurrentMode { get; set; } = Mode.Login;
        public bool Busy { get; set; }
        public string Status { get; set; } = "";

        public string ServerUrl;
        public string Email;
        public string Password = "";
        public string CharacterName = "";

        public event Action LoginClicked;
        public event Action RegisterClicked;
        public event Action CreateCharacterClicked;

        public LoginScreen(string serverUrl)
        {
            ServerUrl = serverUrl;
            Email = PlayerPrefs.GetString(EmailPref, "");
        }

        public void RememberEmail()
        {
            PlayerPrefs.SetString(EmailPref, Email);
            PlayerPrefs.Save();
        }

        public void Draw()
        {
            GuiScale.Begin();
            const float w = 460f;
            var x = (GuiScale.Width - w) / 2f;
            GUILayout.BeginArea(new Rect(x, 40f, w, GuiScale.Height - 80f));
            GUILayout.Label("<b>Dungeon Delvers Online</b> — Phase 0", GuiScale.Label);
            GUILayout.Space(12);

            GUI.enabled = !Busy;
            if (CurrentMode == Mode.Login)
            {
                DrawLogin();
            }
            else
            {
                DrawCreateCharacter();
            }
            GUI.enabled = true;

            GUILayout.Space(12);
            GUILayout.Label(Busy ? "Đang xử lý…" : Status, GuiScale.Label);
            GUILayout.EndArea();
        }

        private void DrawLogin()
        {
            GUILayout.Label("Server", GuiScale.Label);
            ServerUrl = GUILayout.TextField(ServerUrl, GuiScale.Field, GUILayout.Height(40));
            GUILayout.Label("Email", GuiScale.Label);
            Email = GUILayout.TextField(Email, GuiScale.Field, GUILayout.Height(40));
            GUILayout.Label("Mật khẩu (≥ 8 ký tự)", GuiScale.Label);
            Password = GUILayout.PasswordField(Password, '*', GuiScale.Field, GUILayout.Height(40));
            GUILayout.Space(12);
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Đăng nhập", GuiScale.Button, GUILayout.Height(50)))
            {
                LoginClicked?.Invoke();
            }
            if (GUILayout.Button("Đăng ký", GuiScale.Button, GUILayout.Height(50)))
            {
                RegisterClicked?.Invoke();
            }
            GUILayout.EndHorizontal();
        }

        private void DrawCreateCharacter()
        {
            GUILayout.Label("Tài khoản chưa có nhân vật. Tạo Warrior:", GuiScale.Label);
            GUILayout.Label("Tên (3–16 ký tự, chữ/số/_)", GuiScale.Label);
            CharacterName = GUILayout.TextField(CharacterName, 16, GuiScale.Field, GUILayout.Height(40));
            GUILayout.Space(12);
            if (GUILayout.Button("Tạo nhân vật", GuiScale.Button, GUILayout.Height(50)))
            {
                CreateCharacterClicked?.Invoke();
            }
        }
    }
}
