package de.cubeside.orefarmstats;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    private final OreFarmStatsPlugin plugin;

    public PlayerListener(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::onTick, 1, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Material material = e.getBlock().getType();

        if (plugin.isOre(material) && plugin.isWorldLogged(e.getBlock().getWorld())) {
            Location loc = e.getBlock().getLocation();
            if (plugin.getKnownWorldOreLocations(loc.getWorld()).add(loc)) {
                plugin.addOreMined(e.getPlayer());
                if (plugin.isDeepOre(material)) {
                    plugin.addDeepOreMined(e.getPlayer());
                }
                /*
                if (plugin.isNowInEvent() && plugin.isMedal(material)) {
                    plugin.addMedalMined(e.getPlayer(), plugin.getMedalScore(material));
                }
                */
            }
        }

        if (plugin.isBuddelzeug(material) && plugin.isWorldLogged(e.getBlock().getWorld())) {
            Location loc = e.getBlock().getLocation();
            if (plugin.getKnownWorldBuddelLocations(loc.getWorld()).add(loc)) {
                plugin.addBuddeled(e.getPlayer());
            }
        }

        if (plugin.isLog(material)) {
            Location loc = e.getBlock().getLocation();
            if (!plugin.getKnownWorldLogLocations(loc.getWorld()).remove(loc)) {
                ItemStack tool = e.getPlayer().getInventory().getItemInMainHand();
                if (tool == null || tool.getEnchantmentLevel(Enchantment.EFFICIENCY) <= 5) {
                    plugin.addLogFarmed(e.getPlayer());
                }
            }
        }

        if (plugin.isVeggie(material, false)) {
            if (e.getBlock().getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    plugin.addVeggie(e.getPlayer(), e.getBlock().getLocation());
                }
            }
        }
/*
        if (plugin.isNowInEvent() && plugin.isGrass(material) && plugin.isWorldLogged(e.getBlock().getWorld())) {
            Location loc = e.getBlock().getLocation();
            if (plugin.getKnownWorldGrasscutLocations(loc.getWorld()).add(loc)) {
                plugin.addGrassCut(e.getPlayer());
            }
        }
*/
        if (plugin.isNowInEvent() && plugin.isVeggie(material, true)) {
            if (e.getBlock().getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    plugin.addHerbstfestScore(e.getPlayer(), material, e.getBlock().getLocation());
                }
            } else {
                plugin.addHerbstfestScore(e.getPlayer(), material, e.getBlock().getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        EntityType type = e.getEntity().getType();
        Player player = e.getEntity().getKiller();

        if (plugin.isNowInEvent() && plugin.isFly(type) && player != null) {
            plugin.addFlySwat(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (plugin.isOre(e.getBlock().getType()) && plugin.isWorldLogged(e.getBlock().getWorld())) {
            Location loc = e.getBlock().getLocation();
            plugin.getKnownWorldOreLocations(loc.getWorld()).add(loc);
        }
        if (plugin.isLog(e.getBlock().getType())) {
            Location loc = e.getBlock().getLocation();
            plugin.getKnownWorldLogLocations(loc.getWorld()).add(loc);
        }
        if (plugin.isGrass(e.getBlock().getType())) {
            Location loc = e.getBlock().getLocation();
            plugin.getKnownWorldGrasscutLocations(loc.getWorld()).add(loc);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            if (plugin.isOre(b.getType()) && plugin.isWorldLogged(b.getWorld())) {
                Block newBlock = b.getRelative(e.getDirection());
                Location loc = newBlock.getLocation();
                plugin.getKnownWorldOreLocations(loc.getWorld()).add(loc);
            }
            if (plugin.isLog(e.getBlock().getType())) {
                Block newBlock = b.getRelative(e.getDirection());
                Location loc = newBlock.getLocation();
                plugin.getKnownWorldLogLocations(loc.getWorld()).add(loc);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            if (plugin.isOre(b.getType()) && plugin.isWorldLogged(b.getWorld())) {
                Block newBlock = b.getRelative(e.getDirection());
                Location loc = newBlock.getLocation();
                plugin.getKnownWorldOreLocations(loc.getWorld()).add(loc);
            }
            if (plugin.isLog(e.getBlock().getType())) {
                Block newBlock = b.getRelative(e.getDirection());
                Location loc = newBlock.getLocation();
                plugin.getKnownWorldLogLocations(loc.getWorld()).add(loc);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(EntityBreedEvent e) {
        if (e.getBreeder() instanceof Player player) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (e.getEntity().isValid()) {
                    plugin.addAnimalBreed(player);
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!plugin.isNowInEvent()) {
            return;
        }
        if (!plugin.isFireDamage(event.getCause())) {
            return;
        }
        plugin.addOlympicTorchScore(player, event.getFinalDamage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.isNowInEvent()) {
            return;
        }
        if (!plugin.isFireDamage(event.getDamageSource().getDamageType())) {
            return;
        }
        plugin.addOlympicTorchScore(event.getPlayer(), -50);
    }

    public void onTick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getVehicle() instanceof Pig && p.getWorld().getEnvironment() == Environment.NETHER && plugin.isNowInEvent()) {
                Location loc = p.getLocation();
                if (plugin.getKnownWorldSchweinereiterLocations(loc.getWorld()).add(p.getUniqueId(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    // plugin.getLogger().info("Player " + p.getName() + " moved on pig");
                    plugin.addSchweinereitenScore(p);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent e) {
        if (plugin.isNowInEvent()) {
            Entity vehicle = e.getVehicle();
            if (vehicle instanceof Boat) {
                Location pold = e.getFrom();
                Location pnew = e.getTo();
                if (pold.getWorld() == pnew.getWorld() && pold.getWorld() != null) {
                    double dist = pold.distance(pnew);
                    if (!Double.isNaN(dist) && !Double.isInfinite(dist) && dist > 0 && dist < 10) {
                        for (Entity passenger : vehicle.getPassengers()) {
                            if (passenger instanceof Player player) {
                                if (player.getGameMode() != GameMode.CREATIVE
                                        && player.getGameMode() != GameMode.SPECTATOR) {
                                    plugin.addBoatTravel(player, dist);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
