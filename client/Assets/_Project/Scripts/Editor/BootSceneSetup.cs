using System.IO;
using DungeonDelvers.Core;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;

namespace DungeonDelvers.EditorTools
{
    /// <summary>Menu: Dungeon Delvers → Create Boot Scene. Creates/overwrites the Phase 0 boot scene.</summary>
    public static class BootSceneSetup
    {
        private const string ScenePath = "Assets/_Project/Scenes/Boot.unity";

        [MenuItem("Dungeon Delvers/Create Boot Scene")]
        public static void CreateBootScene()
        {
            if (!EditorSceneManager.SaveCurrentModifiedScenesIfUserWantsTo())
            {
                return;
            }
            Directory.CreateDirectory(Path.GetDirectoryName(ScenePath));

            var scene = EditorSceneManager.NewScene(NewSceneSetup.DefaultGameObjects, NewSceneMode.Single);
            new GameObject("Game").AddComponent<GameBootstrap>();
            EditorSceneManager.SaveScene(scene, ScenePath);

            EditorBuildSettings.scenes = new[] { new EditorBuildSettingsScene(ScenePath, true) };
            Debug.Log($"Created {ScenePath} and set it as the only scene in Build Settings. Press Play.");
        }
    }
}
