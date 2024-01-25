package de.cubeside.orefarmstats;

import de.iani.cubesidestats.api.CubesideStatisticsAPI;
import de.iani.cubesidestats.api.PlayerStatistics;
import de.iani.cubesidestats.api.StatisticKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class OreFarmStatsPlugin extends JavaPlugin {

    private final HashMap<String, KnownWorldOreLocations> prevoiusLocations = new HashMap<>();
    private final HashSet<Material> oreMaterials = new HashSet<>();
    // private long startTime;
    // private long endTime;
    private @Nullable CubesideStatisticsAPI cubesideStatistics;
    private StatisticKey oreStatsKey;

    @Override
    public void onEnable() {
        oreMaterials.add(Material.COAL_ORE);
        oreMaterials.add(Material.COPPER_ORE);
        oreMaterials.add(Material.DIAMOND_ORE);
        oreMaterials.add(Material.EMERALD_ORE);
        oreMaterials.add(Material.GOLD_ORE);
        oreMaterials.add(Material.IRON_ORE);
        oreMaterials.add(Material.LAPIS_ORE);
        oreMaterials.add(Material.REDSTONE_ORE);

        oreMaterials.add(Material.DEEPSLATE_COAL_ORE);
        oreMaterials.add(Material.DEEPSLATE_COPPER_ORE);
        oreMaterials.add(Material.DEEPSLATE_DIAMOND_ORE);
        oreMaterials.add(Material.DEEPSLATE_EMERALD_ORE);
        oreMaterials.add(Material.DEEPSLATE_GOLD_ORE);
        oreMaterials.add(Material.DEEPSLATE_IRON_ORE);
        oreMaterials.add(Material.DEEPSLATE_LAPIS_ORE);
        oreMaterials.add(Material.DEEPSLATE_REDSTONE_ORE);

        oreMaterials.add(Material.NETHER_QUARTZ_ORE);
        oreMaterials.add(Material.NETHER_GOLD_ORE);
        oreMaterials.add(Material.ANCIENT_DEBRIS);

        getDataFolder().mkdirs();
        for (String fileName : getDataFolder().list((dir, name) -> name.endsWith(".dat"))) {
            String worldName = fileName.substring(0, fileName.length() - 4);
            getKnownWorldOreLocations(worldName);
        }

        // SimpleDateFormat timeParser = new SimpleDateFormat("d.M.y H:m:s", Locale.GERMANY);
        // try {
        // startTime = timeParser.parse("1.2.2024 0:0:0").getTime();
        // endTime = timeParser.parse("1.3.2024 0:0:0").getTime();
        // } catch (ParseException e) {
        // getLogger().log(Level.SEVERE, "Could not parse start/end time!", e);
        // return;
        // }

        cubesideStatistics = getServer().getServicesManager().load(CubesideStatisticsAPI.class);
        oreStatsKey = cubesideStatistics.getStatisticKey("orestats");
        oreStatsKey.setDisplayName("Erze gemint");
        oreStatsKey.setIsMonthlyStats(true);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    @Override
    public void onDisable() {
        for (KnownWorldOreLocations e : prevoiusLocations.values()) {
            e.close();
        }
        prevoiusLocations.clear();
    }

    public KnownWorldOreLocations getKnownWorldOreLocations(World world) {
        return getKnownWorldOreLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldOreLocations(String world) {
        return prevoiusLocations.computeIfAbsent(world, (world2) -> new KnownWorldOreLocations(this, world2));
    }

    public boolean isOre(Material type) {
        return oreMaterials.contains(type);
    }

    // public boolean isActive() {
    // long now = System.currentTimeMillis();
    // return now >= startTime && now < endTime;
    // }

    public void addOreMined(Player p) {
        addOreMined(p, 1);
    }

    public void addOreMined(Player p, int count) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(oreStatsKey, count);
    }
}
