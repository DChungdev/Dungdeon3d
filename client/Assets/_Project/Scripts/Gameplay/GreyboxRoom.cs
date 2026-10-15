using UnityEngine;

namespace DungeonDelvers.Gameplay
{
    /// <summary>
    /// Phase 0 greybox: 40 m × 40 m floor centred on the origin (the server clamps players to
    /// x, z ∈ [-20, 20]), low border walls and a few pillars as visual reference points.
    /// </summary>
    public static class GreyboxRoom
    {
        public const float HalfExtent = 20f;

        public static GameObject Build()
        {
            var root = new GameObject("GreyboxRoom");

            var floor = GameObject.CreatePrimitive(PrimitiveType.Cube);
            floor.name = "Floor";
            floor.transform.SetParent(root.transform, false);
            floor.transform.localScale = new Vector3(HalfExtent * 2, 0.2f, HalfExtent * 2);
            floor.transform.localPosition = new Vector3(0f, -0.1f, 0f);
            Paint(floor, new Color(0.35f, 0.36f, 0.4f));

            // Grid lines every 5 m make movement and speed easy to judge.
            for (var i = -HalfExtent; i <= HalfExtent; i += 5f)
            {
                Line(root.transform, new Vector3(i, 0.01f, 0f), new Vector3(0.05f, 0.01f, HalfExtent * 2));
                Line(root.transform, new Vector3(0f, 0.01f, i), new Vector3(HalfExtent * 2, 0.01f, 0.05f));
            }

            var wallColor = new Color(0.25f, 0.25f, 0.3f);
            const float h = 1.5f, t = 0.5f;
            Block(root.transform, "WallN", new Vector3(0, h / 2, HalfExtent + t / 2), new Vector3(HalfExtent * 2 + t * 2, h, t), wallColor);
            Block(root.transform, "WallS", new Vector3(0, h / 2, -HalfExtent - t / 2), new Vector3(HalfExtent * 2 + t * 2, h, t), wallColor);
            Block(root.transform, "WallE", new Vector3(HalfExtent + t / 2, h / 2, 0), new Vector3(t, h, HalfExtent * 2), wallColor);
            Block(root.transform, "WallW", new Vector3(-HalfExtent - t / 2, h / 2, 0), new Vector3(t, h, HalfExtent * 2), wallColor);

            var pillarColor = new Color(0.55f, 0.45f, 0.3f);
            foreach (var p in new[] { new Vector3(-10, 0, -10), new Vector3(10, 0, -10), new Vector3(-10, 0, 10), new Vector3(10, 0, 10) })
            {
                Block(root.transform, "Pillar", p + Vector3.up * 1.5f, new Vector3(1.2f, 3f, 1.2f), pillarColor);
            }
            return root;
        }

        private static void Line(Transform parent, Vector3 position, Vector3 scale)
        {
            Block(parent, "GridLine", position, scale, new Color(0.45f, 0.47f, 0.52f));
        }

        private static void Block(Transform parent, string name, Vector3 position, Vector3 scale, Color color)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Cube);
            go.name = name;
            go.transform.SetParent(parent, false);
            go.transform.localPosition = position;
            go.transform.localScale = scale;
            Object.Destroy(go.GetComponent<Collider>());
            Paint(go, color);
        }

        public static void Paint(GameObject go, Color color)
        {
            var renderer = go.GetComponent<Renderer>();
            // .material instantiates a copy, so each object can have its own colour.
            renderer.material.color = color;
            renderer.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
        }
    }
}
