using Newtonsoft.Json.Linq;
using UnityEngine;

namespace DungeonDelvers.Network
{
    /// <summary>Conversions between protocol JSON and Unity types. Gameplay is on the XZ plane.</summary>
    public static class NetMath
    {
        public static Vector3 ToVector3(JToken vec)
        {
            if (vec == null || vec.Type != JTokenType.Object)
            {
                return Vector3.zero;
            }
            return new Vector3((float)vec["x"], 0f, (float)vec["z"]);
        }

        public static object FromVector3(Vector3 v) => new { x = v.x, y = 0f, z = v.z };
    }
}
