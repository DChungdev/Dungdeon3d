using System;
using UnityEngine;

namespace DungeonDelvers.Network
{
    /// <summary>
    /// Server address, editable on the login screen and remembered in PlayerPrefs.
    /// Editor default is localhost; on a phone enter the PC's LAN IP (e.g. http://192.168.1.10:8080).
    /// </summary>
    public static class ServerConfig
    {
        private const string PrefKey = "ddo.serverUrl";

        public static string RestBaseUrl
        {
            get => PlayerPrefs.GetString(PrefKey, DefaultUrl());
            set
            {
                PlayerPrefs.SetString(PrefKey, value.Trim().TrimEnd('/'));
                PlayerPrefs.Save();
            }
        }

        /// <summary>http://host:port → ws://host:port/ws/game (https → wss).</summary>
        public static string WsUrl(string accessToken)
        {
            var baseUrl = RestBaseUrl;
            var wsBase = baseUrl.StartsWith("https://", StringComparison.OrdinalIgnoreCase)
                ? "wss://" + baseUrl.Substring("https://".Length)
                : "ws://" + baseUrl.Substring(baseUrl.IndexOf("://", StringComparison.Ordinal) + 3);
            return wsBase + "/ws/game?token=" + Uri.EscapeDataString(accessToken);
        }

        private static string DefaultUrl()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            return "http://10.0.2.2:8080"; // Android emulator → host machine
#else
            return "http://localhost:8080";
#endif
        }
    }
}
