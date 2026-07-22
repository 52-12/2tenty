package threeadd.glimpsy.feature.simple;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import threeadd.glimpsy.Glimpsy;

import java.util.concurrent.ThreadLocalRandom;

public class LavaCauldronListener implements Listener {

    private static final int REQUIRED_TUFF = 32;
    private static final int OBSIDIAN_FORM_TICKS = 200;

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        if (item.getItemStack().getType() != Material.TUFF) return;
        if (item.getItemStack().getAmount() != REQUIRED_TUFF) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (item.getItemStack().getAmount() != REQUIRED_TUFF || item.isOnGround() || item.isInRain()
                        || item.getTicksLived() > 20L * 60L) {
                    this.cancel();
                    return;
                }

                Block block = item.getLocation().getBlock();
                if (block.getType() != Material.CAULDRON) return;
                if (!isLitCampfireBelow(block)) return;

                item.remove();
                animateObsidianFormation(event.getPlayer(), block);
                this.cancel();
            }
        }.runTaskTimer(Glimpsy.getInstance(), 0L, 5L);
    }

    private boolean isLitCampfireBelow(Block block) {
        Block below = block.getRelative(BlockFace.DOWN);
        return below.getType() == Material.CAMPFIRE && ((Campfire) below.getBlockData()).isLit();
    }

    private void animateObsidianFormation(Player player, Block cauldronBlock) {
        player.playSound(cauldronBlock.getLocation(), Sound.ITEM_BUNDLE_INSERT, 1.0F, 1.0F);
        player.playSound(cauldronBlock.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0F, 1.0F);

        int[] ticksLeft = {OBSIDIAN_FORM_TICKS};
        int[] particlePulse = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                Block block = cauldronBlock.getLocation().getBlock();

                if (ticksLeft[0] < 1) {
                    block.getWorld().dropItem(block.getLocation().add(0, 1, 0), new ItemStack(Material.OBSIDIAN, 1));
                    this.cancel();
                    return;
                }

                if (block.getType() != Material.CAULDRON || !isLitCampfireBelow(block)) {
                    this.cancel();
                    return;
                }

                if (particlePulse[0] > 20) {
                    Particle.DustTransition transition = new Particle.DustTransition(Color.ORANGE, Color.RED, 5);
                    Location location = block.getLocation().toCenterLocation().add(0, 1.2, 0);

                    new ParticleBuilder(Particle.DUST_COLOR_TRANSITION).location(location).count(5).data(transition).spawn();
                    location.getWorld().playSound(location, Sound.BLOCK_LAVA_AMBIENT, 1.0F, 1.0F);

                    if (ThreadLocalRandom.current().nextInt(100) <= 25) {
                        location.getWorld().playSound(location, Sound.BLOCK_FIRE_AMBIENT, 1.0F, 1.0F);
                    }
                    particlePulse[0] = 0;
                }

                particlePulse[0] += 1;
                ticksLeft[0] -= 1;
            }
        }.runTaskTimer(Glimpsy.getInstance(), 0L, 1L);
    }
}
