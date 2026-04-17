package de.cubeside.orefarmstats;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import de.cubeside.orefarmstats.commands.RemoveReceivingEntityCommand;
import de.cubeside.orefarmstats.commands.SetReceivingEntityCommand;
import de.cubeside.orefarmstats.commands.lottery.ClearLotteryStatsKeysCommand;
import de.cubeside.orefarmstats.commands.lottery.DrawWinnerCommand;
import de.cubeside.orefarmstats.commands.lottery.ListLotteryStatsKeysCommand;
import de.cubeside.orefarmstats.commands.lottery.SetLotteryStatsKeysCommand;
import de.cubeside.orefarmstats.commands.statsDisplay.AddToStatsDisplayCommand;
import de.cubeside.orefarmstats.commands.statsDisplay.CreateStatsDisplayCommand;
import de.cubeside.orefarmstats.commands.statsDisplay.ListStatsDisplayCommand;
import de.cubeside.orefarmstats.commands.statsDisplay.RemoveFromStatsDisplayCommand;
import de.cubeside.orefarmstats.commands.statsDisplay.RemoveStatsDisplayCommand;
import de.cubeside.orefarmstats.commands.statsDisplay.SetStatTextOnDisplayCommand;
import de.cubeside.orefarmstats.commands.statsDisplay.SetStatsDisplayHeadlineCommand;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;

public class OreFarmStatsPlugin extends JavaPlugin {

    private StatsDisplayManager statsDisplays;

    private UUID receivingEntity;

    private final HashMap<String, KnownWorldOreLocations> previousLocations = new HashMap<>();
    private final HashMap<String, KnownWorldOreLocations> previousEventLocations = new HashMap<>();
    private final HashMap<String, KnownWorldMultiLocations> previousBuddelLocations = new HashMap<>();
    private final Map<UUID, LinkedList<Location>> veggieLocationsPlayer = new HashMap<>();
    private final Map<UUID, LinkedList<Location>> communityEventveggieLocationsPlayer = new HashMap<>();
    private final HashMap<String, KnownWorldOreLocations> previousLogLocations = new HashMap<>();
    private final HashMap<String, KnownWorldOreLocations> previousIceSnowLocations = new HashMap<>();
    private final HashMap<String, KnownWorldOreLocations> previousEventLogLocations = new HashMap<>();
    private final HashMap<String, KnownWorldMultiChunks> previousMonsterLocations = new HashMap<>();
    private final HashMap<String, KnownWorldPlayerChunks> schweinereiterChunks = new HashMap<>();
    private final HashMap<String, KnownWorldOreLocations> previousGrasscutLocations = new HashMap<>();
    private final HashMap<String, KnownWorldMultiChunks> communityEventMonsterkillingChunks = new HashMap<>();
    private final HashMap<String, KnownWorldPlayerChunks> schreiterReiterChunks = new HashMap<>();
    private final HashMap<String, KnownWorldOreLocations> eatenCakes = new HashMap<>();

    private final HashMap<UUID, Double> playerBoatTravelAccumDist = new HashMap<>();
    private final Set<DamageCause> fireDamageCauses = EnumSet.of(DamageCause.CAMPFIRE, DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.HOT_FLOOR, DamageCause.LAVA);
    private final Set<DamageType> fireDamageTypes = Set.of(DamageType.CAMPFIRE, DamageType.HOT_FLOOR, DamageType.IN_FIRE, DamageType.LAVA, DamageType.ON_FIRE);
    private final Map<UUID, Double> olympicTorchPartialPoints = new HashMap<>();
    private final Map<UUID, Integer> cakePiecesEaten = new HashMap<>();

    private final HashSet<Material> oreMaterials = new HashSet<>();
    private final HashSet<Material> deepOreMaterials = new HashSet<>();
    private final HashSet<Material> logMaterials = new HashSet<>();
    private final HashSet<Material> buddlerMaterials = new HashSet<>();
    private final HashSet<Material> veggiesMaterials = new HashSet<>();
    private final HashSet<Material> iceSnowMaterials = new HashSet<>();
    private final HashSet<EntityType> monsterMobs = new HashSet<>();
    private final HashSet<EntityType> illagerMobs = new HashSet<>();
    private final HashMap<Material, Integer> medalMaterials = new HashMap<>();
    private final HashSet<Material> grassCutterMaterials = new HashSet<>();
    private final HashSet<EntityType> flySwatterMobs = new HashSet<>();
    private HashMap<Material, List<Object>> veggieStatsKeysMap;
    private HashMap<EntityType, List<Object>> halloweenMonsterKillingStatsKeysMap;
    private HashMap<Chicken.Variant, List<Object>> birthdayMobBreedingStatsKeysMap;

    private final HashSet<Material> halloweenMiningMaterials = new HashSet<>();
    private final HashSet<Material> halloweenNetherbaumMaterials = new HashSet<>();
    // private long startTime;
    // private long endTime;
    private @Nullable CubesideStatisticsAPI cubesideStatistics;
    private StatisticKey oreStatsKey;
    private StatisticKey deepOreStatsKey;
    private StatisticKey logStatsKey;
    private StatisticKey breedStatsKey;
    private StatisticKey veggieStatsKey;
    private StatisticKey buddelStatsKey;
    private StatisticKey iceSnowStatsKey;
    private StatisticKey monsterSlayerStatsKey;
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

    private StatisticKey halloween2025PlayerCreeperKillingStatsKey;
    private StatisticKey halloween2025PlayerPhantomKillingStatsKey;
    private StatisticKey halloween2025PlayerMiningStatsKey;
    private StatisticKey halloween2025PlayerSchreiterreiterStatsKey;
    private StatisticKey halloween2025PlayerNetherbaumStatsKey;
    private StatisticKey halloween2025PlayerNetherwarzenStatsKey;
    private GlobalStatisticKey halloween2025CommunityCreeperKillingStatsKey;
    private GlobalStatisticKey halloween2025CommunityPhantomKillingStatsKey;
    private GlobalStatisticKey halloween2025CommunityMiningStatsKey;
    private GlobalStatisticKey halloween2025CommunitySchreiterreiterStatsKey;
    private GlobalStatisticKey halloween2025CommunityNetherbaumStatsKey;
    private GlobalStatisticKey halloween2025CommunityNetherwarzenStatsKey;

    private StatisticKey birthday2026PlayerCowStatsKey;
    private StatisticKey birthday2026PlayerTemperateChickenStatsKey;
    private StatisticKey birthday2026PlayerColdChickenStatsKey;
    private StatisticKey birthday2026PlayerWarmChickenStatsKey;
    private StatisticKey birthday2026PlayerCakeEatenStatsKey;
    private StatisticKey birthday2026PlayerLumaFedStatsKey;
    private StatisticKey birthday2026PlayerWheatStatsKey;
    // private StatisticKey birthday2026PlayerSugarCaneStatsKey;
    private StatisticKey birthday2026PlayerIllagerKilledStatsKey;
    private GlobalStatisticKey birthday2026CommunityCowStatsKey;
    private GlobalStatisticKey birthday2026CommunityTemperateChickenStatsKey;
    private GlobalStatisticKey birthday2026CommunityColdChickenStatsKey;
    private GlobalStatisticKey birthday2026CommunityWarmChickenStatsKey;
    private GlobalStatisticKey birthday2026CommunityCakeEatenStatsKey;
    private GlobalStatisticKey birthday2026CommunityLumaFedStatsKey;
    private GlobalStatisticKey birthday2026CommunityWheatStatsKey;
    // private GlobalStatisticKey birthday2026CommunitySugarCaneStatsKey;
    private GlobalStatisticKey birthday2026CommunityIllagerKilledStatsKey;

    @Override
    public void onEnable() {
        cubesideStatistics = getServer().getServicesManager().load(CubesideStatisticsAPI.class);
        statsDisplays = new StatsDisplayManager(this, cubesideStatistics);

        CommandRouter router = new CommandRouter(getCommand("orefarmstats"));
        router.addCommandMapping(new CreateStatsDisplayCommand(statsDisplays), "statsDisplay", "create");
        router.addCommandMapping(new RemoveStatsDisplayCommand(statsDisplays), "statsDisplay", "remove");
        router.addCommandMapping(new ListStatsDisplayCommand(statsDisplays), "statsDisplay", "list");
        router.addCommandMapping(new AddToStatsDisplayCommand(statsDisplays), "statsDisplay", "addStatTo");
        router.addCommandMapping(new RemoveFromStatsDisplayCommand(statsDisplays), "statsDisplay", "removeStatFrom");
        router.addCommandMapping(new SetStatsDisplayHeadlineCommand(statsDisplays), "statsDisplay", "setHeadline");
        router.addCommandMapping(new SetStatTextOnDisplayCommand(statsDisplays), "statsDisplay", "setStatTextOn");
        router.addCommandMapping(new DrawWinnerCommand(this), "lottery");
        router.addCommandMapping(new SetLotteryStatsKeysCommand(this), "lottery", "setKeys");
        router.addCommandMapping(new ClearLotteryStatsKeysCommand(this), "lottery", "clearKeys");
        router.addCommandMapping(new ListLotteryStatsKeysCommand(this), "lottery", "listKeys");
        router.addCommandMapping(new SetReceivingEntityCommand(this), "setReceiveEntity");
        router.addCommandMapping(new RemoveReceivingEntityCommand(this), "removeReceiveEntity");

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

        iceSnowMaterials.add(Material.ICE);
        iceSnowMaterials.add(Material.BLUE_ICE);
        iceSnowMaterials.add(Material.PACKED_ICE);
        iceSnowMaterials.add(Material.SNOW);
        iceSnowMaterials.add(Material.SNOW_BLOCK);
        iceSnowMaterials.add(Material.POWDER_SNOW);

        monsterMobs.addAll(Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .filter(EntityType::isSpawnable)
                .filter(type -> type.getEntityClass() != null &&
                        org.bukkit.entity.Enemy.class.isAssignableFrom(type.getEntityClass()))
                .collect(Collectors.toSet()));

        illagerMobs.addAll(Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .filter(EntityType::isSpawnable)
                .filter(type -> type.getEntityClass() != null &&
                        org.bukkit.entity.Illager.class.isAssignableFrom(type.getEntityClass()))
                .collect(Collectors.toSet()));

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

        halloweenMiningMaterials.add(Material.GILDED_BLACKSTONE);
        halloweenMiningMaterials.add(Material.ANCIENT_DEBRIS);

        halloweenNetherbaumMaterials.add(Material.SHROOMLIGHT);
        halloweenNetherbaumMaterials.add(Material.WARPED_STEM);
        halloweenNetherbaumMaterials.add(Material.WARPED_WART_BLOCK);
        halloweenNetherbaumMaterials.add(Material.CRIMSON_STEM);
        halloweenNetherbaumMaterials.add(Material.NETHER_WART_BLOCK);

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

        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.set(2026, Calendar.APRIL, 17, 0, 0, 0);
        eventStartMillis = c.getTimeInMillis();
        c.set(2026, Calendar.APRIL, 26, 19, 0, 0);
        eventEndMillis = c.getTimeInMillis();

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

        iceSnowStatsKey = cubesideStatistics.getStatisticKey("farmstats.icesnow");
        iceSnowStatsKey.setDisplayName("Eis und Schnee gemint");
        iceSnowStatsKey.setIsMonthlyStats(true);

        monsterSlayerStatsKey = cubesideStatistics.getStatisticKey("farmstats.monster");
        monsterSlayerStatsKey.setDisplayName("Monster gekillt");
        monsterSlayerStatsKey.setIsMonthlyStats(true);

        breedStatsKey = cubesideStatistics.getStatisticKey("farmstats.breeding");
        breedStatsKey.setDisplayName("Tiere vermehrt");
        breedStatsKey.setIsMonthlyStats(true);

        veggieStatsKey = cubesideStatistics.getStatisticKey("farmstats.plants");
        veggieStatsKey.setDisplayName("Gemüse gefarmt");
        veggieStatsKey.setIsMonthlyStats(true);

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

        halloween2025PlayerMiningStatsKey = cubesideStatistics.getStatisticKey("halloween.2025.mining");
        halloween2025PlayerMiningStatsKey.setDisplayName("Golddurchzogenen Schwarzstein und Antiken Schrott gemint");
        halloween2025CommunityMiningStatsKey = cubesideStatistics.getGlobalStatisticKey("halloween.2025.mining");
        halloween2025CommunityMiningStatsKey.setDisplayName("Golddurchzogenen Schwarzstein und Antiken Schrott gemint");
        halloween2025PlayerCreeperKillingStatsKey = cubesideStatistics.getStatisticKey("halloween.2025.creeper");
        halloween2025PlayerCreeperKillingStatsKey.setDisplayName("Creeper besiegt");
        halloween2025CommunityCreeperKillingStatsKey = cubesideStatistics.getGlobalStatisticKey("halloween.2025.creeper");
        halloween2025CommunityCreeperKillingStatsKey.setDisplayName("Creeper besiegt");
        halloween2025PlayerPhantomKillingStatsKey = cubesideStatistics.getStatisticKey("halloween.2025.phantome");
        halloween2025PlayerPhantomKillingStatsKey.setDisplayName("Phantome besiegt");
        halloween2025CommunityPhantomKillingStatsKey = cubesideStatistics.getGlobalStatisticKey("halloween.2025.phantome");
        halloween2025CommunityPhantomKillingStatsKey.setDisplayName("Creeper besiegt");
        halloween2025PlayerSchreiterreiterStatsKey = cubesideStatistics.getStatisticKey("halloween.2025.schreitereiter");
        halloween2025PlayerSchreiterreiterStatsKey.setDisplayName("Chunks mit Stridern bereist");
        halloween2025CommunitySchreiterreiterStatsKey = cubesideStatistics.getGlobalStatisticKey("halloween.2025.schreitereiter");
        halloween2025CommunitySchreiterreiterStatsKey.setDisplayName("Chunks mit Stridern gemeinsam bereist");
        halloween2025PlayerNetherbaumStatsKey = cubesideStatistics.getStatisticKey("halloween.2025.netherbaeume");
        halloween2025PlayerNetherbaumStatsKey.setDisplayName("Netherbäume abgeholzt");
        halloween2025CommunityNetherbaumStatsKey = cubesideStatistics.getGlobalStatisticKey("halloween.2025.netherbaeume");
        halloween2025CommunityNetherbaumStatsKey.setDisplayName("Netherbäume gemeinsam abgeholzt");
        halloween2025PlayerNetherwarzenStatsKey = cubesideStatistics.getStatisticKey("halloween.2025.netherwarzen");
        halloween2025PlayerNetherwarzenStatsKey.setDisplayName("Netherwarzen gefarmt");
        halloween2025CommunityNetherwarzenStatsKey = cubesideStatistics.getGlobalStatisticKey("halloween.2025.netherwarzen");
        halloween2025CommunityNetherwarzenStatsKey.setDisplayName("Netherwarzen gemeinsam gefarmt");

        birthday2026PlayerCowStatsKey = cubesideStatistics.getStatisticKey("birthday.2026.cow");
        birthday2026PlayerCowStatsKey.setDisplayName("Kühe getötet");
        birthday2026PlayerTemperateChickenStatsKey = cubesideStatistics.getStatisticKey("birthday.2026.chicken.temperate");
        birthday2026PlayerTemperateChickenStatsKey.setDisplayName("Gemäßigte Hühner getötet");
        birthday2026PlayerColdChickenStatsKey = cubesideStatistics.getStatisticKey("birthday.2026.chicken.cold");
        birthday2026PlayerColdChickenStatsKey.setDisplayName("Kalt-Hühner getötet");
        birthday2026PlayerWarmChickenStatsKey = cubesideStatistics.getStatisticKey("birthday.2026.chicken.warm");
        birthday2026PlayerWarmChickenStatsKey.setDisplayName("Warm-Hühner getötet");
        birthday2026PlayerCakeEatenStatsKey = cubesideStatistics.getStatisticKey("birthday.2026.cake");
        birthday2026PlayerCakeEatenStatsKey.setDisplayName("Kuchen gefuttert");
        birthday2026PlayerLumaFedStatsKey = cubesideStatistics.getStatisticKey("birthday.2026.luma");
        birthday2026PlayerLumaFedStatsKey.setDisplayName("Kuchen an Willi Warden verfüttert");
        birthday2026PlayerWheatStatsKey = cubesideStatistics.getStatisticKey("birthday.2026.wheat");
        birthday2026PlayerWheatStatsKey.setDisplayName("Weizen gefarmt");
        // birthday2026PlayerSugarCaneStatsKey = cubesideStatistics.getStatisticKey("birthday.2026.sugarcane");
        // birthday2026PlayerSugarCaneStatsKey.setDisplayName("Zuckerrohr gefarmt");
        birthday2026PlayerIllagerKilledStatsKey = cubesideStatistics.getStatisticKey("birthday.2026.illager");
        birthday2026PlayerIllagerKilledStatsKey.setDisplayName("Illager besiegt");
        birthday2026CommunityCowStatsKey = cubesideStatistics.getGlobalStatisticKey("birthday.2026.cow");
        birthday2026CommunityCowStatsKey.setDisplayName("Kühe gemeinsam getötet");
        birthday2026CommunityTemperateChickenStatsKey = cubesideStatistics.getGlobalStatisticKey("birthday.2026.chicken.temperate");
        birthday2026CommunityTemperateChickenStatsKey.setDisplayName("Gemäßigte Hühner gemeinsam getötet");
        birthday2026CommunityColdChickenStatsKey = cubesideStatistics.getGlobalStatisticKey("birthday.2026.chicken.cold");
        birthday2026CommunityColdChickenStatsKey.setDisplayName("Kalt-Hühner gemeinsam getötet");
        birthday2026CommunityWarmChickenStatsKey = cubesideStatistics.getGlobalStatisticKey("birthday.2026.chicken.warm");
        birthday2026CommunityWarmChickenStatsKey.setDisplayName("Warm-Hühner gemeinsam getötet");
        birthday2026CommunityCakeEatenStatsKey = cubesideStatistics.getGlobalStatisticKey("birthday.2026.cake");
        birthday2026CommunityCakeEatenStatsKey.setDisplayName("Kuchen gemeinsam gefuttert");
        birthday2026CommunityLumaFedStatsKey = cubesideStatistics.getGlobalStatisticKey("birthday.2026.luma");
        birthday2026CommunityLumaFedStatsKey.setDisplayName("Kuchen an Luma gemeinsam verfüttert");
        birthday2026CommunityWheatStatsKey = cubesideStatistics.getGlobalStatisticKey("birthday.2026.wheat");
        birthday2026CommunityWheatStatsKey.setDisplayName("Weizen gemeinsam gefarmt");
        // birthday2026CommunitySugarCaneStatsKey = cubesideStatistics.getGlobalStatisticKey("birthday.2026.sugarcane");
        // birthday2026CommunitySugarCaneStatsKey.setDisplayName("Zuckerrohr gemeinsam gefarmt");
        birthday2026CommunityIllagerKilledStatsKey = cubesideStatistics.getGlobalStatisticKey("birthday.2026.illager");
        birthday2026CommunityIllagerKilledStatsKey.setDisplayName("Illager gemeinsam besiegt");

        veggieStatsKeysMap = new HashMap<>();
        // veggieStatsKeysMap.put(Material.SUGAR_CANE, List.of(birthday2026PlayerSugarCaneStatsKey, birthday2026CommunitySugarCaneStatsKey));
        // veggieStatsKeysMap.put(Material.WHEAT, List.of(birthday2026PlayerWheatStatsKey, birthday2026CommunityWheatStatsKey));

        halloweenMonsterKillingStatsKeysMap = new HashMap<>();
        halloweenMonsterKillingStatsKeysMap.put(EntityType.CREEPER, List.of(halloween2025PlayerCreeperKillingStatsKey, halloween2025CommunityCreeperKillingStatsKey));
        halloweenMonsterKillingStatsKeysMap.put(EntityType.PHANTOM, List.of(halloween2025PlayerPhantomKillingStatsKey, halloween2025CommunityPhantomKillingStatsKey));

        birthdayMobBreedingStatsKeysMap = new HashMap<>();
        birthdayMobBreedingStatsKeysMap.put(Chicken.Variant.TEMPERATE, List.of(birthday2026PlayerTemperateChickenStatsKey, birthday2026CommunityTemperateChickenStatsKey));
        birthdayMobBreedingStatsKeysMap.put(Chicken.Variant.COLD, List.of(birthday2026PlayerColdChickenStatsKey, birthday2026CommunityColdChickenStatsKey));
        birthdayMobBreedingStatsKeysMap.put(Chicken.Variant.WARM, List.of(birthday2026PlayerWarmChickenStatsKey, birthday2026CommunityWarmChickenStatsKey));

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        if (updateEventStatsSum) {
            getServer().getScheduler().runTaskTimer(this, this::updateStatsSum, 60 * 20, 60 * 20);
        }

        receivingEntity = getConfig().contains("receivingEntity") ? UUID.fromString(getConfig().getString("receivingEntity")) : null;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isNowInEvent()) {
                    return;
                }
                for (World world : getServer().getWorlds()) {
                    com.sk89q.worldguard.protection.managers.RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                    if (rm == null || !rm.hasRegion("kuchenschmaus")) {
                        continue;
                    }
                    Set<BlockVector> locations = getKnownWorldEatenCakeLocations(world).getLocations();
                    for (BlockVector location : locations) {
                        Location loc = new Location(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
                        if (getKnownWorldEatenCakeLocations(world).remove(loc) &&
                                rm.getRegion("kuchenschmaus").contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) &&
                                loc.getBlock().getType() == Material.AIR) {
                            loc.getBlock().setType(Material.CAKE);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 6000L, 6000L);

        /*
         * // alte Monats .dat Dateien löschen
         * SimpleDateFormat formatter = new SimpleDateFormat("MM-yyyy");
         * for (File file : getDataFolder().listFiles()) {
         * if (file.isFile() && file.getName().indexOf("+MY") > -1) {
         * String fileName = file.getName();
         * fileName = fileName.substring(fileName.indexOf("+MY")+3);
         * fileName = fileName.substring(0, fileName.indexOf('.'));
         * try {
         * Date date = formatter.parse(fileName);
         * c.setTime(date);
         * long fileDate = c.getTimeInMillis();
         * c = Calendar.getInstance();
         * long currentDate = c.getTimeInMillis();
         * getLogger().log(Level.WARNING, "file: " + fileDate + ", current: " + currentDate);
         * if (currentDate - fileDate > 7889400000L) { // 3 Monate alte löschen
         * file.delete();
         * }
         * } catch (ParseException ex) {
         *
         * }
         * }
         * }
         */
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

        for (KnownWorldOreLocations e : previousIceSnowLocations.values()) {
            e.close();
        }
        previousIceSnowLocations.clear();

        for (KnownWorldMultiChunks e : previousMonsterLocations.values()) {
            e.close();
        }
        previousMonsterLocations.clear();

        for (KnownWorldPlayerChunks e : schweinereiterChunks.values()) {
            e.close();
        }
        schweinereiterChunks.clear();

        for (KnownWorldPlayerChunks e : schreiterReiterChunks.values()) {
            e.close();
        }
        schreiterReiterChunks.clear();

        for (KnownWorldOreLocations e : previousGrasscutLocations.values()) {
            e.close();
        }
        previousGrasscutLocations.clear();

        for (KnownWorldMultiChunks e : communityEventMonsterkillingChunks.values()) {
            e.close();
        }
        communityEventMonsterkillingChunks.clear();

        for (KnownWorldOreLocations e : previousEventLocations.values()) {
            e.close();
        }
        previousEventLocations.clear();

        for (KnownWorldOreLocations e : previousEventLogLocations.values()) {
            e.close();
        }
        previousEventLogLocations.clear();

        for (KnownWorldOreLocations e : eatenCakes.values()) {
            e.close();
        }
        eatenCakes.clear();
    }

    public StatsDisplayManager getStatsDisplayManager() {
        return statsDisplays;
    }

    public UUID getReceivingEntity() {
        return this.receivingEntity;
    }

    public void setReceivingEntity(Entity entity) {
        this.receivingEntity = entity == null ? null : entity.getUniqueId();
        getConfig().set("receivingEntity", receivingEntity != null ? receivingEntity.toString() : null);
        saveConfig();
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

    public String getCombinedString(LinkedList<String> components, String separator) {
        StringBuilder combinedText = new StringBuilder();
        boolean first = true;
        for (String component : components) {
            if (!first) {
                combinedText.append(separator);
            }
            first = false;
            combinedText.append(component);
        }
        return combinedText.toString();
    }

    public boolean isWorldLogged(World world) {
        return loggedWorlds == null || loggedWorlds.contains(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldOreLocations(World world) {
        return getKnownWorldOreLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldOreLocations(String world) {
        return previousLocations.computeIfAbsent(world, world2 -> new KnownWorldOreLocations(this, world2));
    }

    public KnownWorldOreLocations getKnownWorldLogLocations(World world) {
        return getKnownWorldLogLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldLogLocations(String world) {
        return previousLogLocations.computeIfAbsent(world, world2 -> new KnownWorldOreLocations(this, world2, "log"));
    }

    public KnownWorldMultiLocations getKnownWorldBuddelLocations(World world) {
        return previousBuddelLocations.computeIfAbsent(world.getName(), world2 -> new KnownWorldMultiLocations(this, world2, 10, "buddel"));
    }

    public KnownWorldOreLocations getKnownWorldIceSnowLocations(World world) {
        return getKnownWorldIceSnowLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldIceSnowLocations(String world) {
        return previousIceSnowLocations.computeIfAbsent(world, world2 -> new KnownWorldOreLocations(this, world2));
    }

    public KnownWorldMultiChunks getKnownWorldMonsterLocations(World world) {
        return getKnownWorldMonsterLocations(world.getName());
    }

    public KnownWorldMultiChunks getKnownWorldMonsterLocations(String world) {
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH) + 1;
        return previousMonsterLocations.computeIfAbsent(world, world2 -> new KnownWorldMultiChunks(this, world2, 30, ("monster+MY" + (month < 10 ? String.format("0%d", month) : month) + "-" + (c.get(Calendar.YEAR)))));
    }

    public KnownWorldPlayerChunks getKnownWorldSchweinereiterLocations(World world) {
        return schweinereiterChunks.computeIfAbsent(world.getName(), world2 -> new KnownWorldPlayerChunks(this, world2, "schweinereiter"));
    }

    public KnownWorldPlayerChunks getKnownWorldschreiterReiterLocations(World world) {
        return schreiterReiterChunks.computeIfAbsent(world.getName(), world2 -> new KnownWorldPlayerChunks(this, world2, "schreiterreiter"));
    }

    public KnownWorldMultiChunks getKnownWorldEventMonsterkillingLocations(World world) {
        return communityEventMonsterkillingChunks.computeIfAbsent(world.getName(), (world2) -> new KnownWorldMultiChunks(this, world2, 30, "birthday_2026_monsterkilling"));
    }

    public KnownWorldOreLocations getKnownWorldGrasscutLocations(World world) {
        return getKnownWorldGrasscutLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldGrasscutLocations(String world) {
        return previousGrasscutLocations.computeIfAbsent(world, world2 -> new KnownWorldOreLocations(this, world2, "grass"));
    }

    public KnownWorldOreLocations getKnownWorldEventOreLocations(World world) {
        return getKnownWorldEventOreLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldEventOreLocations(String world) {
        return previousEventLocations.computeIfAbsent(world, world2 -> new KnownWorldOreLocations(this, world2, "halloween_2025_ore"));
    }

    public KnownWorldOreLocations getKnownWorldEventLogLocations(World world) {
        return getKnownWorldEventLogLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldEventLogLocations(String world) {
        return previousEventLogLocations.computeIfAbsent(world, world2 -> new KnownWorldOreLocations(this, world2, "halloween_2025_log"));
    }

    public KnownWorldOreLocations getKnownWorldEatenCakeLocations(World world) {
        return getKnownWorldEatenCakeLocations(world.getName());
    }

    public KnownWorldOreLocations getKnownWorldEatenCakeLocations(String world) {
        return eatenCakes.computeIfAbsent(world, world2 -> new KnownWorldOreLocations(this, world2, "birthday_2026_cake"));
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

    public boolean isIceSnow(Material type) {
        return iceSnowMaterials.contains(type);
    }

    public boolean isMonster(EntityType type) {
        return monsterMobs.contains(type);
    }

    public boolean isIllager(EntityType type) {
        return illagerMobs.contains(type);
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

    public boolean isHalloweenMonster(EntityType type) {
        return halloweenMonsterKillingStatsKeysMap.containsKey(type);
    }

    public boolean isHalloweenMiningMaterial(Material type) {
        return halloweenMiningMaterials.contains(type);
    }

    public boolean isHalloweenNetherbaumMaterial(Material type) {
        return halloweenNetherbaumMaterials.contains(type);
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

    public void addIceSnowMined(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(iceSnowStatsKey, 1);
    }

    public void addMonsterSlayen(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        playerStats.increaseScore(monsterSlayerStatsKey, 1);
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

    public void addHalloweenMiningScore(Player p) {
        UUID uuid = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(uuid);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore(halloween2025PlayerMiningStatsKey, 1);
        globalStatistic.increaseValue(halloween2025CommunityMiningStatsKey, 1);
    }

    public void addHalloweenKillingScore(Player p, EntityType entityType) {
        List<Object> statKeys = halloweenMonsterKillingStatsKeysMap.get(entityType);

        UUID uuid = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(uuid);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore((StatisticKey) statKeys.getFirst(), 1);
        globalStatistic.increaseValue((GlobalStatisticKey) statKeys.getLast(), 1);
    }

    public void addHalloweenSchreiterreiterScore(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore(halloween2025PlayerSchreiterreiterStatsKey, 1);
        globalStatistic.increaseValue(halloween2025CommunitySchreiterreiterStatsKey, 1);
    }

    public void addHalloweenNetherbaumScore(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore(halloween2025PlayerNetherbaumStatsKey, 1);
        globalStatistic.increaseValue(halloween2025CommunityNetherbaumStatsKey, 1);
    }

    public void addHalloweenNetherwarzenScore(Player p, Location location) {
        UUID playerId = p.getUniqueId();

        communityEventveggieLocationsPlayer.putIfAbsent(playerId, new LinkedList<>());
        LinkedList<Location> locations = communityEventveggieLocationsPlayer.get(playerId);
        if (locations.contains(location)) {
            return;
        }
        locations.addFirst(location);
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore(halloween2025PlayerNetherwarzenStatsKey, 1);
        globalStatistic.increaseValue(halloween2025CommunityNetherwarzenStatsKey, 1);

        if (locations.size() > 50) {
            locations.removeLast();
        }
        communityEventveggieLocationsPlayer.put(playerId, locations);
    }

    public void addBirthdayChickenScore(Player p, Chicken.Variant variant) {
        List<Object> statKeys = birthdayMobBreedingStatsKeysMap.get(variant);
        if (statKeys == null) {
            return;
        }

        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore((StatisticKey) statKeys.getFirst(), 1);
        globalStatistic.increaseValue((GlobalStatisticKey) statKeys.getLast(), 1);
    }

    public void addBirthdayCowScore(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore(birthday2026PlayerCowStatsKey, 1);
        globalStatistic.increaseValue(birthday2026CommunityCowStatsKey, 1);
    }

    public void addBirthdayCakeScore(Player p) {
        UUID playerId = p.getUniqueId();

        int amount = cakePiecesEaten.getOrDefault(playerId, 0) + 1;
        if (amount > 6) {
            cakePiecesEaten.put(playerId, 0);

            PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
            GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();
            playerStats.increaseScore(birthday2026PlayerCakeEatenStatsKey, 1);
            globalStatistic.increaseValue(birthday2026CommunityCakeEatenStatsKey, 1);
        } else {
            cakePiecesEaten.put(playerId, amount);
        }
    }

    public void addBirthdayLumaScore(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore(birthday2026PlayerLumaFedStatsKey, 1);
        globalStatistic.increaseValue(birthday2026CommunityLumaFedStatsKey, 1);
    }

    public void addBirthdayWheatScore(Player p, Location location) {
        UUID uuid = p.getUniqueId();
        communityEventveggieLocationsPlayer.putIfAbsent(uuid, new LinkedList<>());
        LinkedList<Location> locations = communityEventveggieLocationsPlayer.get(uuid);
        if (locations.contains(location)) {
            return;
        }
        locations.addFirst(location);

        PlayerStatistics playerStats = cubesideStatistics.getStatistics(uuid);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore(birthday2026PlayerWheatStatsKey, 1);
        globalStatistic.increaseValue(birthday2026CommunityWheatStatsKey, 1);

        if (locations.size() > 50) {
            locations.removeLast();
        }
        communityEventveggieLocationsPlayer.put(uuid, locations);
    }

    public void addBirthdayIllagerScore(Player p) {
        UUID playerId = p.getUniqueId();
        PlayerStatistics playerStats = cubesideStatistics.getStatistics(playerId);
        GlobalStatistics globalStatistic = cubesideStatistics.getGlobalStatistics();

        playerStats.increaseScore(birthday2026PlayerIllagerKilledStatsKey, 1);
        globalStatistic.increaseValue(birthday2026CommunityIllagerKilledStatsKey, 1);
    }
}
