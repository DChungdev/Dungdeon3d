using UnityEngine;

namespace DungeonDelvers.Gameplay
{
    /// <summary>
    /// Common presentation state of any entity in the instance (player or mob): identity,
    /// HP from the server, HP bar and death visuals. Movement lives in
    /// <see cref="LocalPlayerController"/> or <see cref="RemoteEntityView"/>.
    /// </summary>
    public class EntityView : MonoBehaviour
    {
        public string Id { get; private set; }
        public string EntityType { get; private set; }
        public string DisplayName { get; private set; }
        public int Hp { get; private set; }
        public int MaxHp { get; private set; }
        public int Mp { get; private set; }
        public int MaxMp { get; private set; }
        public bool IsAlive => Hp > 0;
        public bool IsMob => EntityType == "MOB";

        private HpBar _hpBar;
        private Renderer _body;
        private Color _bodyColor;

        public void Init(string id, string entityType, string displayName, int hp, int maxHp, int mp, int maxMp,
            Color bodyColor, Color hpBarColor)
        {
            Id = id;
            EntityType = entityType;
            DisplayName = displayName;
            MaxHp = Mathf.Max(1, maxHp);
            MaxMp = maxMp;
            _body = GetComponent<Renderer>();
            _bodyColor = bodyColor;
            _hpBar = HpBar.Create(transform, hpBarColor);
            SetHp(hp);
            SetMp(mp);
            if (!IsAlive)
            {
                ShowDead(true); // spawned already dead (snapshot during the despawn delay)
            }
        }

        public void SetHp(int hp)
        {
            var wasAlive = IsAlive;
            Hp = Mathf.Clamp(hp, 0, MaxHp);
            _hpBar.SetFraction((float)Hp / MaxHp);
            if (wasAlive && !IsAlive)
            {
                ShowDead(true);
            }
            else if (!wasAlive && IsAlive)
            {
                ShowDead(false); // respawn
            }
        }

        public void SetMp(int mp) => Mp = Mathf.Max(0, mp);

        /// <summary>Called on ENTITY_DEATH (HP may already be 0 from the DAMAGE_EVENT).</summary>
        public void Die()
        {
            if (IsAlive)
            {
                Hp = 0;
                _hpBar.SetFraction(0f);
            }
            ShowDead(true);
        }

        private void ShowDead(bool dead)
        {
            if (_body != null)
            {
                _body.material.color = dead ? new Color(0.3f, 0.3f, 0.3f) : _bodyColor;
            }
            _hpBar.gameObject.SetActive(!dead);
        }
    }
}
