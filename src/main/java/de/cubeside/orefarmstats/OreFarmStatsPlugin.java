package de.cubeside.orefarmstats;

import de.cubeside.orefarmstats.commands.AddToStatsDisplayCommand;
import de.cubeside.orefarmstats.commands.CreateStatsDisplayCommand;
import de.cubeside.orefarmstats.commands.DrawWinnerCommand;
import de.cubeside.orefarmstats.commands.ListStatsDisplayCommand;
import de.cubeside.orefarmstats.commands.RemoveFromStatsDisplayCommand;
import de.cubeside.orefarmstats.commands.RemoveStatsDisplayCommand;
import de.iani.cubesidestats.api.CubesideStatisticsAPI;
import de.iani.cubesidestats.api.GlobalStatisticKey;
import de.iani.cubesidestats.api.GlobalStatistics;
import de.iani.cubesidestats.api.Ordering;
import de.iani.cubesidestats.api.PlayerStatistics;
import de.iani.cubesidestats.api.PlayerWithScore;
import de.iani.cubesidestats.api.PositionAlgorithm;
import de.iani.cubesidestats.api.StatisticKey;
import de.iani.cubesidestats.api.TimeFrame;
import de.iani.cubesideutils.bukkit.commands.CommandRouter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OreFarmStatsPlugin extends JavaPlugin {

    private final HashMap<String, KnownWorldOreLocations> previousLocations = new HashMap<>();
    private final HashMap<String, KnownWorldMultiLocations> previousBuddelLocations = new HashMap<>();
    private final Map<UUID, LinkedList<Location>> veggieLocationsPlayer = new HashMap<>();
    private final Map<UUID, LinkedList<Location>> communityEventveggieLocationsPlayer = new HashMap<>();
    private final HashMap<String, KnownWorldOreLocations> previousLogLocations = new HashMap<>();
    private final HashMap<String, KnownWorldPlayerChunks> schweinereiterChunks = new HashMap<>();
    private final HashMap<String, KnownWorldOreLocations> previousGrasscutLocations = new HashMap<>();
    private final HashMap<UUID, Double> playerBoatTravelAccumDist = new HashMap<>();
    private final Set<DamageCause> fireDamageCauses = EnumSet.of(DamageCause.CAMPFIRE, DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.HOT_FLOOR, DamageCause.LAVA);
    private final Set<DamageType> fireDamageTypes = Set.of(DamageType.CAMPFIRE, DamageType.HOT_FLOOR, DamageType.IN_FIRE, DamageType.LAVA, DamageType.ON_FIRE);
    private final Map<UUID, Double> olympicTorchPartialPoints = new HashMap<>();

    private final HashSet<Material> oreMaterials = new HashSet<>();
    private final HashSet<Material> deepOreMaterials = new HashSet<>();
    private final HashSet<Material> logMaterials = new HashSet<>();
    private final HashSet<Material> buddlerMaterials = new HashSet<>();
    private final HashSet<Material> veggiesMaterials = new HashSet<>();
    private final HashMap<Material, Integer> medalMaterials = new HashMap<>();
    private final HashSet<Material> grassCutterMaterials = new HashSet<>();
    private final HashSet<EntityType> flySwatterMobs = new HashSet<>();
    private HashMap<Material, List<Object>> veggieStatsKeysMap;
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
    private StatisticKey eventMedalminingStatsKey;
    private StatisticKey eventGrasscutterStatsKey;
    private StatisticKey eventFlySwatterStatsKey;
    private StatisticKey eventBoatTravelStatsKey;
    private StatisticKey eventOlympicTorchStatsKey;
    private StatisticKey eventArcadeStatsKey;
    private StatisticKey eventTotalScoreStatsKey;

    private StatisticKey communityEventPlayerMelonStatsKey;
    private StatisticKey communityEventPlayerCarrotStatsKey;
    private StatisticKey communityEventPlayerPotatoStatsKey;
    private StatisticKey communityEventPlayerBeetrootStatsKey;
    private StatisticKey communityEventPlayerPumpkinStatsKey;
    private StatisticKey communityEventPlayerWheatStatsKey;
    private StatisticKey communityEventPlayerCocoaStatsKey;
    private GlobalStatisticKey communityEventMelonStatsKey;
    private GlobalStatisticKey communityEventCarrotStatsKey;
    private GlobalStatisticKey communityEventPotatoStatsKey;
    private GlobalStatisticKey communityEventBeetrootStatsKey;
    private GlobalStatisticKey communityEventPumpkinStatsKey;
    private GlobalStatisticKey communityEventWheatStatsKey;
    private GlobalStatisticKey communityEventCocoaStatsKey;

    private final LinkedHashMap<UUID, List<String>> statsDisplays = new LinkedHashMap<>();
    private final ConcurrentHashMap<UUID, Component> allTimeStats = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        cubesideStatistics = getServer().getServicesManager().load(CubesideStatisticsAPI.class);

        CommandRouter router = new CommandRouter(getCommand("orefarmstats"));
        router.addCommandMapping(new CreateStatsDisplayCommand(this), "statsDisplay", "create");
        router.addCommandMapping(new RemoveStatsDisplayCommand(this), "statsDisplay", "remove");
        router.addCommandMapping(new ListStatsDisplayCommand(this), "statsDisplay", "list");
        router.addCommandMapping(new AddToStatsDisplayCommand(this), "statsDisplay", "addStatTo");
        router.addCommandMapping(new RemoveFromStatsDisplayCommand(this), "statsDisplay", "removeStatFrom");
        router.addCommandMapping(new DrawWinnerCommand(this), "drawWinner");

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

        medalMaterials.put(Material.GOLD_ORE, 5);
        medalMaterials.put(Material.DEEPSLATE_GOLD_ORE, 5);
        medalMaterials.put(Material.NETHER_GOLD_ORE, 5);
        medalMaterials.put(Material.RAW_GOLD_BLOCK, 5);
        medalMaterials.put(Material.IRON_ORE, 3);
        medalMaterials.put(Material.DEEPSLATE_IRON_ORE, 3);
        medalMaterials.put(Material.RAW_IRON_BLOCK, 3);
        medalMaterials.put(Material.COPPER_ORE, 1);
        medalMaterials.put(Material.DEEPSLATE_COPPER_ORE, 1);
        medalMaterials.put(Material.RAW_COPPER_BLOCK, 1);

        grassCutterMaterials.add(Material.PALE_HANGING_MOSS);
        grassCutterMaterials.add(Material.SHORT_GRASS);
        grassCutterMaterials.add(Material.FERN);
        grassCutterMaterials.add(Material.SHORT_DRY_GRASS);
        grassCutterMaterials.add(Material.BUSH);
        grassCutterMaterials.add(Material.FIREFLY_BUSH);
        grassCutterMaterials.add(Material.CRIMSON_ROOTS);
        grassCutterMaterials.add(Material.WARPED_ROOTS);
        grassCutterMaterials.add(Material.NETHER_SPROUTS);
        grassCutterMaterials.add(Material.TALL_GRASS);
        grassCutterMaterials.add(Material.TALL_DRY_GRASS);
        grassCutterMaterials.add(Material.LARGE_FERN);
        grassCutterMaterials.add(Material.HANGING_ROOTS);
        grassCutterMaterials.add(Material.GLOW_LICHEN);
        grassCutterMaterials.add(Material.SEAGRASS);
        grassCutterMaterials.add(Material.KELP);
        grassCutterMaterials.add(Material.DEAD_BUSH);
        grassCutterMaterials.add(Material.BROWN_MUSHROOM);
        grassCutterMaterials.add(Material.RED_MUSHROOM);

        flySwatterMobs.add(EntityType.BEE);
        flySwatterMobs.add(EntityType.BAT);
        flySwatterMobs.add(EntityType.ALLAY);

        getDataFolder().mkdirs();
        saveDefaultConfig();
        HashSet<String> loggedWorldsList = new HashSet<>(getConfig().getStringList("worlds"));
        boolean allWorldsLogged = loggedWorldsList.remove("*");
        loggedWorlds = allWorldsLogged ? null : Set.of(loggedWorldsList.toArray(new String[loggedWorldsList.size()]));

        boolean updateEventStatsSum = getConfig().getBoolean("updateEventStatsSum", false);

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
        c.set(2025, Calendar.SEPTEMBER, 18, 19, 0, 0);
        eventStartMillis = c.getTimeInMillis();
        c.set(2025, Calendar.SEPTEMBER, 21, 19, 0, 0);
        eventEndMillis = c.getTimeInMillis();

        eventSchweinereitenStatsKey = cubesideStatistics.getStatisticKey("sommerspiele.2025.schweinereiten");
        eventSchweinereitenStatsKey.setDisplayName("Schweinereiten");

        eventMedalminingStatsKey = cubesideStatistics.getStatisticKey("sommerspiele.2025.medalmining");
        eventMedalminingStatsKey.setDisplayName("Medaillenmining");

        eventGrasscutterStatsKey = cubesideStatistics.getStatisticKey("sommerspiele.2025.rasenmaehen");
        eventGrasscutterStatsKey.setDisplayName("Rasenmähen");

        eventFlySwatterStatsKey = cubesideStatistics.getStatisticKey("sommerspiele.2025.fliegenklatschen");
        eventFlySwatterStatsKey.setDisplayName("Fliegenklatschen");

        eventBoatTravelStatsKey = cubesideStatistics.getStatisticKey("sommerspiele.2025.bootfahren");
        eventBoatTravelStatsKey.setDisplayName("Bootfahren");

        eventOlympicTorchStatsKey = cubesideStatistics.getStatisticKey("sommerspiele.2025.olympischefackel");
        eventOlympicTorchStatsKey.setDisplayName("Olympische Fackel");

        eventArcadeStatsKey = cubesideStatistics.getStatisticKey("sommerspiele.2025.olympischesArcade");
        eventArcadeStatsKey.setDisplayName("Olympisches Arcade");

        eventTotalScoreStatsKey = cubesideStatistics.getStatisticKey("sommerspiele.2025.total");
        eventTotalScoreStatsKey.setDisplayName("Gesamtpunkte");

        communityEventPlayerMelonStatsKey = cubesideStatistics.getStatisticKey("herbstfest.2025.melonen");
        communityEventPlayerMelonStatsKey.setDisplayName("Melonen gefarmt");
        communityEventMelonStatsKey = cubesideStatistics.getGlobalStatisticKey("herbstfest.2025.melonen");
        communityEventMelonStatsKey.setDisplayName("Melonen gemeinsam gefarmt");

        communityEventPlayerPotatoStatsKey = cubesideStatistics.getStatisticKey("herbstfest.2025.kartoffeln");
        communityEventPlayerPotatoStatsKey.setDisplayName("Kartoffeln gefarmt");
        communityEventPotatoStatsKey = cubesideStatistics.getGlobalStatisticKey("herbstfest.2025.kartoffeln");
        communityEventPotatoStatsKey.setDisplayName("Kartoffeln gemeinsam gefarmt");

        communityEventPlayerPumpkinStatsKey = cubesideStatistics.getStatisticKey("herbstfest.2025.kuerbisse");
        communityEventPlayerPumpkinStatsKey.setDisplayName("Kürbisse gefarmt");
        communityEventPumpkinStatsKey = cubesideStatistics.getGlobalStatisticKey("herbstfest.2025.kuerbisse");
        communityEventPumpkinStatsKey.setDisplayName("Kürbisse gemeinsam gefarmt");

        communityEventPlayerCocoaStatsKey = cubesideStatistics.getStatisticKey("herbstfest.2025.kakaobohnen");
        communityEventPlayerCocoaStatsKey.setDisplayName("Kakaobohnen gefarmt");
        communityEventCocoaStatsKey = cubesideStatistics.getGlobalStatisticKey("herbstfest.2025.kakaobohnen");
        communityEventCocoaStatsKey.setDisplayName("Kakaobohnen gemeinsam gefarmt");

        communityEventPlayerCarrotStatsKey = cubesideStatistics.getStatisticKey("herbstfest.2025.karotten");
        communityEventPlayerCarrotStatsKey.setDisplayName("Karotten gefarmt");
        communityEventCarrotStatsKey = cubesideStatistics.getGlobalStatisticKey("herbstfest.2025.karotten");
        communityEventCarrotStatsKey.setDisplayName("Karotten gemeinsam gefarmt");

        communityEventPlayerBeetrootStatsKey = cubesideStatistics.getStatisticKey("herbstfest.2025.rotebete");
        communityEventPlayerBeetrootStatsKey.setDisplayName("Rote Bete gefarmt");
        communityEventBeetrootStatsKey = cubesideStatistics.getGlobalStatisticKey("herbstfest.2025.rotebete");
        communityEventBeetrootStatsKey.setDisplayName("Rote Bete gemeinsam gefarmt");

        communityEventPlayerWheatStatsKey = cubesideStatistics.getStatisticKey("herbstfest.2025.weizen");
        communityEventPlayerWheatStatsKey.setDisplayName("Weizen gefarmt");
        communityEventWheatStatsKey = cubesideStatistics.getGlobalStatisticKey("herbstfest.2025.weizen");
        communityEventWheatStatsKey.setDisplayName("Weizen gemeinsam gefarmt");

        veggieStatsKeysMap = new HashMap<>();
        veggieStatsKeysMap.put(Material.MELON, List.of(communityEventPlayerMelonStatsKey, communityEventMelonStatsKey));
        veggieStatsKeysMap.put(Material.POTATOES, List.of(communityEventPlayerPotatoStatsKey,
                communityEventPotatoStatsKey));
        veggieStatsKeysMap.put(Material.PUMPKIN, List.of(communityEventPlayerPumpkinStatsKey,
                communityEventPumpkinStatsKey));
        veggieStatsKeysMap.put(Material.COCOA, List.of(communityEventPlayerCocoaStatsKey,
                communityEventCocoaStatsKey));
        veggieStatsKeysMap.put(Material.CARROTS, List.of(communityEventPlayerCarrotStatsKey,
                communityEventCarrotStatsKey));
        veggieStatsKeysMap.put(Material.BEETROOTS, List.of(communityEventPlayerBeetrootStatsKey,
                communityEventBeetrootStatsKey));
        veggieStatsKeysMap.put(Material.WHEAT, List.of(communityEventPlayerWheatStatsKey, communityEventWheatStatsKey));

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        ConfigurationSection section = getConfig().getConfigurationSection("statsDisplays");
        if (section != null) {
            section.getKeys(false).forEach(key -> {
                UUID uuid = UUID.fromString(key);
                statsDisplays.put(uuid, section.getStringList(key));
            });
        }
        statsDisplays.keySet().forEach(this::updateStatsComponent);
        getServer().getScheduler().runTaskTimer(this, () -> {
            updateDisplayEntities();
            statsDisplays.keySet().forEach(this::updateStatsComponent);
        }, 20 * 10, 20 * 10);

        if (updateEventStatsSum) {
            getServer().getScheduler().runTaskTimer(this, this::updateStatsSum, 60 * 20, 60 * 20);
        }
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

        for (KnownWorldOreLocations e : previousGrasscutLocations.values()) {
            e.close();
        }
        previousGrasscutLocations.clear();
    }

    public StatisticKey getEventSchweinereitenStatsKey() {
        return eventSchweinereitenStatsKey;
    }

    public StatisticKey getEventMedalminingStatsKey() {
        return eventMedalminingStatsKey;
    }

    public StatisticKey getEventGrasscutterStatsKey() {
        return eventGrasscutterStatsKey;
    }

    public StatisticKey getEventFlySwatterStatsKey() {
        return eventFlySwatterStatsKey;
    }

    public StatisticKey getEventBoatTravelStatsKey() {
        return eventBoatTravelStatsKey;
    }

    private void updateStatsSum() {
        final int totalScoredPlayers = 50;
        eventTotalScoreStatsKey.getTop(0, 1024 * 1024, Ordering.DESCENDING, TimeFrame.ALL_TIME, PositionAlgorithm.TOTAL_ORDER, listSum -> {
            eventSchweinereitenStatsKey.getTop(0, totalScoredPlayers, Ordering.DESCENDING, TimeFrame.ALL_TIME, PositionAlgorithm.TOTAL_ORDER, list_1 -> {
                eventMedalminingStatsKey.getTop(0, totalScoredPlayers, Ordering.DESCENDING, TimeFrame.ALL_TIME, PositionAlgorithm.TOTAL_ORDER, list_2 -> {
                    eventGrasscutterStatsKey.getTop(0, totalScoredPlayers, Ordering.DESCENDING, TimeFrame.ALL_TIME, PositionAlgorithm.TOTAL_ORDER, list_3 -> {
                        eventFlySwatterStatsKey.getTop(0, totalScoredPlayers, Ordering.DESCENDING, TimeFrame.ALL_TIME, PositionAlgorithm.TOTAL_ORDER, list_4 -> {
                            eventBoatTravelStatsKey.getTop(0, totalScoredPlayers, Ordering.DESCENDING, TimeFrame.ALL_TIME, PositionAlgorithm.TOTAL_ORDER, list_5 -> {
                                eventOlympicTorchStatsKey.getTop(0, totalScoredPlayers, Ordering.DESCENDING, TimeFrame.ALL_TIME, PositionAlgorithm.TOTAL_ORDER, list_6 -> {
                                    eventArcadeStatsKey.getTop(0, totalScoredPlayers, Ordering.DESCENDING, TimeFrame.ALL_TIME, PositionAlgorithm.TOTAL_ORDER, list_7 -> {
                                        HashMap<UUID, Integer> playerScores = new HashMap<>();
                                        for (List<PlayerWithScore> scoreList : List.of(list_1, list_2, list_3, list_4, list_5, list_6, list_7)) {
                                            for (PlayerWithScore player : scoreList) {
                                                int score = totalScoredPlayers + 1 - player.getPosition();
                                                playerScores.merge(player.getPlayer().getOwner(), score, Integer::sum);
                                            }
                                        }
                                        // update existing entries
                                        for (PlayerWithScore totalScorePlayer : listSum) {
                                            Integer expected = playerScores.remove(totalScorePlayer.getPlayer().getOwner());
                                            if (expected == null) {
                                                expected = 0;
                                            }
                                            if (totalScorePlayer.getScore() != expected) {
                                                totalScorePlayer.getPlayer().setScore(eventTotalScoreStatsKey, expected);
                                            }
                                        }
                                        // add missing entries
                                        for (Entry<UUID, Integer> newScores : playerScores.entrySet()) {
                                            cubesideStatistics.getStatistics(newScores.getKey()).setScore(eventTotalScoreStatsKey, newScores.getValue());
                                        }
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    public Collection<? extends GlobalStatisticKey> getGlobalStatsKeys() {
        return cubesideStatistics.getAllGlobalStatisticKeys();
    }

    public boolean existGlobalStatsKey(String key) {
        return cubesideStatistics.hasGlobalStatisticKey(key);
    }

    public CubesideStatisticsAPI getStatistics() {
        return cubesideStatistics;
    }

    public int amountStatsDisplays() {
        return statsDisplays.size();
    }

    public LinkedHashMap<UUID, List<String>> getStatsDisplays() {
        return new LinkedHashMap<>(statsDisplays);
    }

    public void createDisplayEntity(Location location, String statsKey) {
        TextDisplay textDisplay = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY, CreatureSpawnEvent.SpawnReason.CUSTOM);
        textDisplay.setShadowed(true);
        textDisplay.setLineWidth(300);
        textDisplay.setAlignment(TextDisplay.TextAlignment.LEFT);
        UUID id = textDisplay.getUniqueId();

        List<String> newList = new ArrayList<String>();
        newList.add(statsKey);
        statsDisplays.put(id, newList);

        getConfig().set("statsDisplays." + id, newList);
        saveConfig();

        updateStatsComponent(id);
    }

    public boolean addStatToStatsDisplay(String statsKey, UUID id) {
        if (statsDisplays.containsKey(id)) {
            statsDisplays.get(id).add(statsKey);

            getConfig().set("statsDisplays." + id, statsDisplays.get(id));
            saveConfig();

            updateStatsComponent(id);
            return true;
        }
        return false;
    }

    public boolean removeStatFromStatsDisplay(String statsKey, UUID id) {
        if (statsDisplays.containsKey(id)) {
            statsDisplays.get(id).remove(statsKey);

            getConfig().set("statsDisplays." + id, statsDisplays.get(id));
            saveConfig();

            updateStatsComponent(id);
            return true;
        }
        return false;
    }

    public void removeDisplayEntity(UUID id) {
        Entity entity = Bukkit.getEntity(id);
        if (entity instanceof TextDisplay display && display.isValid()) {
            display.remove();
        }

        Map<UUID, List<String>> statsDisplayCopy = new HashMap<>(statsDisplays);
        statsDisplayCopy.forEach((display, statsKeys) -> {
            if (display.equals(id)) {
               statsDisplays.remove(display);
               getConfig().set("statsDisplays." + id, null);
            }
        });
        saveConfig();
    }

    public Component getCombinedText(LinkedList<Component> components, String seperator) {
        Component combinedText = Component.empty();
        boolean first = true;
        for (Component component : components) {
            if (!first) {
                combinedText = combinedText.append(Component.text(seperator));
            }
            first = false;
            combinedText = combinedText.append(component);
        }
        return combinedText;
    }

    public String getCombinedString(LinkedList<String> components, String seperator) {
        String combinedText = new String();
        boolean first = true;
        for (String component : components) {
            if (!first) {
                combinedText += seperator;
            }
            first = false;
            combinedText += component;
        }
        return combinedText;
    }

    public void updateStatsComponent(UUID id) {
        LinkedList<Component> components = new LinkedList<>();
        components.add(Component.text("Insgesamt: ", Style.style(TextColor.color(0xFF1493), TextDecoration.BOLD)));

        List<String> statsKeys = statsDisplays.get(id);
        if (statsKeys.isEmpty()) {
            allTimeStats.put(id, getCombinedText(components, "\n"));
            updateDisplayEntity(id);
            return;
        }
        statsKeys.forEach(statsKey -> {
            GlobalStatisticKey key = cubesideStatistics.getGlobalStatisticKey(statsKey, false);
            if (key != null) {
                cubesideStatistics.getGlobalStatistics().getValue(key, TimeFrame.ALL_TIME, allTime -> {
                    components.add(Component.text(allTime, NamedTextColor.YELLOW).append(Component.text(" " + key.getDisplayName(), NamedTextColor.GOLD)));
                    allTimeStats.put(id, getCombinedText(components, "\n"));
                    updateDisplayEntity(id);
                });
            }
        });
    };

    public void updateDisplayEntities() {
        statsDisplays.keySet().forEach(this::updateDisplayEntity);
    }

    public void updateDisplayEntity(UUID id) {
        TextDisplay textDisplay = null;

        if (statsDisplays.containsKey(id)) {
            Entity entity = Bukkit.getEntity(id);
            if (entity instanceof TextDisplay display && display.isValid()) {
                textDisplay = display;
            }
        }

        if (textDisplay == null) {
            return;
        }

        Component component = allTimeStats.getOrDefault(id, Component.empty());

        textDisplay.text(component);
    }

    public boolean isWorldLogged(World world) {
        return loggedWorlds == null || loggedWorlds.contains(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldOreLocations(World world) {
        return getKnownWorldOreLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldOreLocations(String world) {
        return previousLocations.computeIfAbsent(world, (world2) -> new KnownWorldOreLocations(this, world2));
    }

    public KnownWorldOreLocations getKnownWorldLogLocations(World world) {
        return getKnownWorldLogLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldLogLocations(String world) {
        return previousLogLocations.computeIfAbsent(world, (world2) -> new KnownWorldOreLocations(this, world2, "log"));
    }

    public KnownWorldMultiLocations getKnownWorldBuddelLocations(World world) {
        return previousBuddelLocations.computeIfAbsent(world.getName(), (world2) -> new KnownWorldMultiLocations(this, world2, 10, "buddel"));
    }

    public KnownWorldPlayerChunks getKnownWorldSchweinereiterLocations(World world) {
        return schweinereiterChunks.computeIfAbsent(world.getName(), (world2) -> new KnownWorldPlayerChunks(this, world2, "schweinereiter"));
    }

    public KnownWorldOreLocations getKnownWorldGrasscutLocations(World world) {
        return getKnownWorldGrasscutLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldGrasscutLocations(String world) {
        return previousGrasscutLocations.computeIfAbsent(world, (world2) -> new KnownWorldOreLocations(this, world2, "grass"));
    }

    public boolean isNowInEvent() {
        long now = System.currentTimeMillis();
        return now >= eventStartMillis && now < eventEndMillis;
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

    public boolean isMedal(Material type) {
        return medalMaterials.keySet().contains(type);
    }

    public int getMedalScore(Material type) {
        return medalMaterials.get(type);
    }

    public boolean isGrass(Material type) {
        return grassCutterMaterials.contains(type);
    }

    public boolean isFly(EntityType type) {
        return flySwatterMobs.contains(type);
    }

    public boolean isVeggie(Material material, boolean communityEvent) {
        if (communityEvent) {
            return veggieStatsKeysMap.containsKey(material);
        } else {
            return veggiesMaterials.contains(material);
        }
    }

    public boolean isFireDamage(DamageCause cause) {
        return fireDamageCauses.contains(cause);
    }

    public boolean isFireDamage(DamageType type) {
        return fireDamageTypes.contains(type);
    }

    // public boolean isActive() {
    // long now = System.currentTimeMillis();
    // return now >= startTime && now < endTime;
    // }

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

    public void addSchweinereitenScore(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(eventSchweinereitenStatsKey, 1);
    }

    public void addMedalMined(Player p, int amount) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(eventMedalminingStatsKey, amount);
    }

    public void addGrassCut(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(eventGrasscutterStatsKey, 1);
    }

    public void addFlySwat(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(eventFlySwatterStatsKey, 1);
    }

    public void addBoatTravel(Player p, double distance) {
        UUID playerId = p.getUniqueId();
        double amount = playerBoatTravelAccumDist.getOrDefault(playerId, 0.0) + distance;
        if (amount > 1) {
            final int flooredAmount = (int) Math.floor(amount);
            playerBoatTravelAccumDist.put(playerId, amount - flooredAmount);
            PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
            playerStats.increaseScore(eventBoatTravelStatsKey, flooredAmount);
        } else {
            playerBoatTravelAccumDist.put(playerId, amount);
        }
    }

    public void addOlympicTorchScore(Player p, double score) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        if (score < 0) {
            playerStats.increaseScore(eventOlympicTorchStatsKey, (int) score);
        } else {
            score += olympicTorchPartialPoints.getOrDefault(playerId, 0.0);
            int fullPoints = (int) Math.floor(score);
            playerStats.increaseScore(eventOlympicTorchStatsKey, fullPoints);
            olympicTorchPartialPoints.put(playerId, score - fullPoints);
        }
    }

    public void addHerbstfestScore(Player p, Material type, Location location) {
        List<Object> statKeys = veggieStatsKeysMap.get(type);
        if (statKeys == null) {
            return;
        }

        UUID uuid = p.getUniqueId();
        communityEventveggieLocationsPlayer.putIfAbsent(uuid, new LinkedList<>());
        LinkedList<Location> locations = communityEventveggieLocationsPlayer.get(uuid);
        if (locations.contains(location)) {
            return;
        }
        locations.addFirst(location);

        PlayerStatistics playerStats = cubesideStatistics.getStatistics(uuid);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore((StatisticKey) statKeys.getFirst(), 1);
        globalStatistic.increaseValue((GlobalStatisticKey) statKeys.getLast(), 1);

        if (locations.size() > 50) {
            locations.removeLast();
        }
        communityEventveggieLocationsPlayer.put(uuid, locations);
    }
}
