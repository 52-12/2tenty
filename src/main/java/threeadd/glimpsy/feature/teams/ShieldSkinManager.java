package threeadd.glimpsy.feature.teams;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.List;
import java.util.UUID;

/**
 * Forces every shield a team member carries to display their team's shared flag design, set via
 * /shieldskin by the owner or a trusted member. There's no single event that covers every way a
 * shield can end up in a player's inventory (crafting, pickup, dispensers, /give, trading, etc.),
 * so instead this just re-checks everyone's inventory on a short timer.
 */
public class ShieldSkinManager extends ListeningManager {

    private static final NamespacedKey SKIN_TEAM_KEY = new NamespacedKey(Glimpsy.getInstance(), "shield_skin_team");

    @Override
    public void onEnable() {
        ScheduleUtil.scheduleSync(this::tick, 1, 1, ScheduleUtil.Unit.SECOND);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyToPlayer(event.getPlayer());
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers())
            applyToPlayer(player);
    }

    public static void reapplyTeam(TeamData team) {
        for (Player player : Bukkit.getOnlinePlayers())
            if (team.getMembers().contains(player.getUniqueId()))
                applyToPlayer(player);
    }

    private static void applyToPlayer(Player player) {
        TeamData team = TeamManager.getTeam(player);
        List<Pattern> patterns = team == null ? List.of() : team.getShieldPatterns();
        UUID teamId = team == null ? null : team.getId();

        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (applyToItem(item, teamId, patterns))
                inventory.setItem(slot, item);
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (applyToItem(offHand, teamId, patterns))
            inventory.setItemInOffHand(offHand);
    }

    // Returns true if the item was changed. Only ever touches shields it previously skinned
    // itself (tracked via the PDC tag) once a player no longer has a team design to show - any
    // shield a player designed by hand and never had skinned is left alone.
    private static boolean applyToItem(@Nullable ItemStack item, @Nullable UUID teamId, List<Pattern> patterns) {
        if (item == null || item.getType() != Material.SHIELD) return false;
        if (!(item.getItemMeta() instanceof BannerMeta meta)) return false;

        String taggedTeam = meta.getPersistentDataContainer().get(SKIN_TEAM_KEY, PersistentDataType.STRING);

        if (!patterns.isEmpty()) {
            if (teamId.toString().equals(taggedTeam) && meta.getPatterns().equals(patterns))
                return false;

            meta.setPatterns(patterns);
            meta.getPersistentDataContainer().set(SKIN_TEAM_KEY, PersistentDataType.STRING, teamId.toString());
            item.setItemMeta(meta);
            return true;
        }

        if (taggedTeam == null) return false;

        meta.setPatterns(List.of());
        meta.getPersistentDataContainer().remove(SKIN_TEAM_KEY);
        item.setItemMeta(meta);
        return true;
    }
}
