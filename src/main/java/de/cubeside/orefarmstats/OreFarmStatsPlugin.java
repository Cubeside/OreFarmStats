package de.cubeside.orefarmstats;

import de.iani.cubesidestats.api.CubesideStatisticsAPI;
import de.iani.cubesidestats.api.PlayerStatistics;
import de.iani.cubesidestats.api.StatisticKey;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class OreFarmStatsPlugin extends JavaPlugin {

    private final HashMap<String, KnownWorldOreLocations> previousLocations = new HashMap<>();
    private final HashMap<String, KnownWorldMultiLocations> previousBuddelLocations = new HashMap<>();
    private final HashSet<Material> oreMaterials = new HashSet<>();
    private final HashSet<Material> deepOreMaterials = new HashSet<>();
    private final HashSet<Material> logMaterials = new HashSet<>();
    private final HashSet<Material> buddlerMaterials = new HashSet<>();
    private final HashSet<Material> veggiesMaterials = new HashSet<>();
    private final Map<UUID, LinkedList<Location>> veggieLocationsPlayer = new HashMap<>();
    private final HashMap<String, KnownWorldOreLocations> previousLogLocations = new HashMap<>();
    private final HashMap<String, KnownWorldPlayerChunks> schweinereiterChunks = new HashMap<>();
    private final Set<DamageCause> fireDamageCauses = EnumSet.of(DamageCause.CAMPFIRE, DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.HOT_FLOOR, DamageCause.LAVA);
    private final Set<DamageType> fireDamageTypes = Set.of(DamageType.CAMPFIRE, DamageType.HOT_FLOOR, DamageType.IN_FIRE, DamageType.LAVA, DamageType.ON_FIRE);
    // private long startTime;
    // private long endTime;
    private @Nullable CubesideStatisticsAPI cubesideStatistics;
    private StatisticKey oreStatsKey;
    private StatisticKey deepOreStatsKey;
    private StatisticKey logStatsKey;
    private StatisticKey breedStatsKey;
    private StatisticKey veggieStatsKey;
    private StatisticKey buddelStatsKey;
    private Set<String> loggedWorlds;

    private long eventStartMillis;
    private long eventEndMillis;
    private StatisticKey eventSchweinereitenStatsKey;
    private StatisticKey eventOlympicTorchStatsKey;

    @Override
    public void onEnable() {
        deepOreMaterials.add(Material.DEEPSLATE_COAL_ORE);
        deepOreMaterials.add(Material.DEEPSLATE_COPPER_ORE);
        deepOreMaterials.add(Material.DEEPSLATE_DIAMOND_ORE);
        deepOreMaterials.add(Material.DEEPSLATE_EMERALD_ORE);
        deepOreMaterials.add(Material.DEEPSLATE_GOLD_ORE);
        deepOreMaterials.add(Material.DEEPSLATE_IRON_ORE);
        deepOreMaterials.add(Material.DEEPSLATE_LAPIS_ORE);
        deepOreMaterials.add(Material.DEEPSLATE_REDSTONE_ORE);
        deepOreMaterials.add(Material.ANCIENT_DEBRIS);

        oreMaterials.addAll(deepOreMaterials);
        oreMaterials.add(Material.COAL_ORE);
        oreMaterials.add(Material.COPPER_ORE);
        oreMaterials.add(Material.DIAMOND_ORE);
        oreMaterials.add(Material.EMERALD_ORE);
        oreMaterials.add(Material.GOLD_ORE);
        oreMaterials.add(Material.IRON_ORE);
        oreMaterials.add(Material.LAPIS_ORE);
        oreMaterials.add(Material.REDSTONE_ORE);
        oreMaterials.add(Material.NETHER_QUARTZ_ORE);
        oreMaterials.add(Material.NETHER_GOLD_ORE);

        buddlerMaterials.add(Material.SOUL_SAND);
        buddlerMaterials.add(Material.SOUL_SOIL);
        buddlerMaterials.add(Material.SAND);
        buddlerMaterials.add(Material.RED_SAND);
        buddlerMaterials.add(Material.DIRT);
        buddlerMaterials.add(Material.GRASS_BLOCK);
        buddlerMaterials.add(Material.GRAVEL);
        buddlerMaterials.add(Material.MUD);
        buddlerMaterials.add(Material.MUDDY_MANGROVE_ROOTS);
        buddlerMaterials.add(Material.PODZOL);
        buddlerMaterials.add(Material.MYCELIUM);
        buddlerMaterials.add(Material.COARSE_DIRT);
        buddlerMaterials.add(Material.ROOTED_DIRT);
        buddlerMaterials.add(Material.SNOW);
        buddlerMaterials.add(Material.SNOW_BLOCK);
        buddlerMaterials.add(Material.CLAY);
        buddlerMaterials.add(Material.DIRT_PATH);
        buddlerMaterials.add(Material.FARMLAND);

        logMaterials.addAll(Tag.LOGS.getValues());

        veggiesMaterials.add(Material.POTATOES);
        veggiesMaterials.add(Material.CARROTS);
        veggiesMaterials.add(Material.BEETROOTS);
        veggiesMaterials.add(Material.WHEAT);
        veggiesMaterials.add(Material.NETHER_WART);
        veggiesMaterials.add(Material.COCOA);

        getDataFolder().mkdirs();
        saveDefaultConfig();
        HashSet<String> loggedWorldsList = new HashSet<>(getConfig().getStringList("worlds"));
        boolean allWorldsLogged = loggedWorldsList.remove("*");
        loggedWorlds = allWorldsLogged ? null : Set.of(loggedWorldsList.toArray(new String[loggedWorldsList.size()]));

        // for (String fileName : getDataFolder().list((dir, name) -> name.endsWith(".dat"))) {
        // String worldName = fileName.substring(0, fileName.length() - 4);
        // getKnownWorldOreLocations(worldName);
        // }

        // SimpleDateFormat timeParser = new SimpleDateFormat("d.M.y H:m:s", Locale.GERMANY);
        // try {
        // startTime = timeParser.parse("1.2.2024 0:0:0").getTime();
        // endTime = timeParser.parse("1.3.2024 0:0:0").getTime();
        // } catch (ParseException e) {
        // getLogger().log(Level.SEVERE, "Could not parse start/end time!", e);
        // return;
        // }

        cubesideStatistics = getServer().getServicesManager().load(CubesideStatisticsAPI.class);
        oreStatsKey = cubesideStatistics.getStatisticKey("farmstats.ore");
        oreStatsKey.setDisplayName("Erze gemint");
        oreStatsKey.setIsMonthlyStats(true);

        deepOreStatsKey = cubesideStatistics.getStatisticKey("farmstats.deepore");
        deepOreStatsKey.setDisplayName("Tiefenerze gemint");
        deepOreStatsKey.setIsMonthlyStats(true);

        buddelStatsKey = cubesideStatistics.getStatisticKey("farmstats.buddler");
        buddelStatsKey.setDisplayName("Weggebuddelt");
        buddelStatsKey.setIsMonthlyStats(true);

        logStatsKey = cubesideStatistics.getStatisticKey("farmstats.log");
        logStatsKey.setDisplayName("Holz gefarmt");
        logStatsKey.setIsMonthlyStats(true);

        breedStatsKey = cubesideStatistics.getStatisticKey("farmstats.breeding");
        breedStatsKey.setDisplayName("Tiere vermehrt");
        breedStatsKey.setIsMonthlyStats(true);

        veggieStatsKey = cubesideStatistics.getStatisticKey("farmstats.plants");
        veggieStatsKey.setDisplayName("Gemüse gefarmt");
        veggieStatsKey.setIsMonthlyStats(true);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.set(2025, Calendar.AUGUST, 10, 0, 0, 0);
        eventStartMillis = c.getTimeInMillis();
        c.set(2025, Calendar.AUGUST, 17, 0, 0, 0);
        eventEndMillis = c.getTimeInMillis();

        eventSchweinereitenStatsKey = cubesideStatistics.getStatisticKey("sommerspiele.2025.schweinereiten");
        eventSchweinereitenStatsKey.setDisplayName("Schweinereiten");

        eventOlympicTorchStatsKey = cubesideStatistics.getStatisticKey("sommerspiele.2025.olympischefackel");
        eventOlympicTorchStatsKey.setDisplayName("Olympische Fackel");

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    @Override
    public void onDisable() {
        for (KnownWorldOreLocations e : previousLocations.values()) {
            e.close();
        }
        previousLocations.clear();

        for (KnownWorldOreLocations e : previousLogLocations.values()) {
            e.close();
        }
        previousLogLocations.clear();

        for (KnownWorldMultiLocations e : previousBuddelLocations.values()) {
            e.close();
        }
        previousBuddelLocations.clear();

        for (KnownWorldPlayerChunks e : schweinereiterChunks.values()) {
            e.close();
        }
        schweinereiterChunks.clear();
    }

    public KnownWorldOreLocations getKnownWorldOreLocations(World world) {
        return getKnownWorldOreLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldOreLocations(String world) {
        return previousLocations.computeIfAbsent(world, (world2) -> new KnownWorldOreLocations(this, world2));
    }

    public boolean isOre(Material type) {
        return oreMaterials.contains(type);
    }

    public boolean isDeepOre(Material type) {
        return deepOreMaterials.contains(type);
    }

    public boolean isLog(Material type) {
        return logMaterials.contains(type);
    }

    public boolean isBuddelzeug(Material type) {
        return buddlerMaterials.contains(type);
    }

    // public boolean isActive() {
    // long now = System.currentTimeMillis();
    // return now >= startTime && now < endTime;
    // }

    public boolean isWorldLogged(World world) {
        return loggedWorlds == null || loggedWorlds.contains(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldLogLocations(World world) {
        return getKnownWorldLogLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldLogLocations(String world) {
        return previousLogLocations.computeIfAbsent(world, (world2) -> new KnownWorldOreLocations(this, world2, "log"));
    }

    public void addOreMined(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(oreStatsKey, 1);
    }

    public void addBuddeled(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(buddelStatsKey, 1);
    }

    public void addDeepOreMined(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(deepOreStatsKey, 1);
    }

    public void addLogFarmed(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(logStatsKey, 1);
    }

    public void addAnimalBreed(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(breedStatsKey, 1);
    }

    public void addVeggie(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        veggieLocationsPlayer.putIfAbsent(uuid, new LinkedList<>());
        LinkedList<Location> locations = veggieLocationsPlayer.get(uuid);
        if (locations.contains(location)) {
            return;
        }

        locations.addFirst(location);
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(uuid);
        playerStats.increaseScore(veggieStatsKey, 1);

        if (locations.size() > 50) {
            locations.removeLast();
        }
        veggieLocationsPlayer.put(uuid, locations);
    }

    public boolean isVeggie(Material material) {
        return veggiesMaterials.contains(material);
    }

    public boolean isFireDamage(DamageCause cause) {
        return fireDamageCauses.contains(cause);
    }

    public boolean isFireDamage(DamageType type) {
        return fireDamageTypes.contains(type);
    }

    public KnownWorldMultiLocations getKnownWorldBuddelLocations(World world) {
        return previousBuddelLocations.computeIfAbsent(world.getName(), (world2) -> new KnownWorldMultiLocations(this, world2, 10, "buddel"));
    }

    public KnownWorldPlayerChunks getKnownWorldSchweinereiterLocations(World world) {
        return schweinereiterChunks.computeIfAbsent(world.getName(), (world2) -> new KnownWorldPlayerChunks(this, world2, "schweinereiter"));
    }

    public boolean isNowInEvent() {
        long now = System.currentTimeMillis();
        return now >= eventStartMillis && now < eventEndMillis;
    }

    public StatisticKey getEventSchweinereitenStatsKey() {
        return eventSchweinereitenStatsKey;
    }

    public void addSchweinereitenScore(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(eventSchweinereitenStatsKey, 1);
    }

    public void addOlympicTorchScore(Player p, int score) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        if (score > 0) {
            playerStats.increaseScore(eventOlympicTorchStatsKey, score);
        } else if (score < 0) {
            playerStats.decreaseScore(eventOlympicTorchStatsKey, -score);
        }
    }
}
