package de.cubeside.orefarmstats;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlayerListener implements Listener {
    private final OreFarmStatsPlugin plugin;

    public PlayerListener(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (plugin.isOre(e.getBlock().getType()) && plugin.isWorldLogged(e.getBlock().getWorld())) {
            Location loc = e.getBlock().getLocation();
            if (plugin.getKnownWorldOreLocations(loc.getWorld()).add(loc)) {
                // if (plugin.isActive()) {
                plugin.addOreMined(e.getPlayer());
                // }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (plugin.isOre(e.getBlock().getType()) && plugin.isWorldLogged(e.getBlock().getWorld())) {
            Location loc = e.getBlock().getLocation();
            plugin.getKnownWorldOreLocations(loc.getWorld()).add(loc);
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
        }
    }
}
