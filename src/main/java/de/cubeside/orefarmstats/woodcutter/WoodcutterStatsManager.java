package de.cubeside.orefarmstats.woodcutter;

import de.iani.cubesidestats.api.CubesideStatisticsAPI;
import de.iani.cubesidestats.api.PlayerStatistics;
import de.iani.cubesidestats.api.PlayerStatisticsQueryKey;
import de.iani.cubesidestats.api.PlayerStatisticsQueryKey.QueryType;
import de.iani.cubesidestats.api.StatisticKey;
import de.iani.cubesidestats.api.StatisticsQueryKey;
import de.iani.cubesidestats.api.TimeFrame;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Material;

public final class WoodcutterStatsManager {
    public static final String KEY_PREFIX = "farmstats.woodcutter.";
    public static final String POINTS_KEY_NAME = KEY_PREFIX + "points";

    private final CubesideStatisticsAPI statistics;
    private final EnumMap<WoodType, StatisticKey> countKeys;
    private final StatisticKey pointsKey;
    private final Map<UUID, RecalculationState> recalculationStates;

    public WoodcutterStatsManager(CubesideStatisticsAPI statistics) {
        this.statistics = statistics;
        this.countKeys = new EnumMap<>(WoodType.class);
        this.recalculationStates = new HashMap<>();

        for (WoodType type : WoodType.values()) {
            StatisticKey key = statistics.getStatisticKey(KEY_PREFIX + type.getKeySuffix());
            key.setDisplayName(type.getStatisticDisplayName());
            key.setIsMonthlyStats(true);
            countKeys.put(type, key);
        }

        pointsKey = statistics.getStatisticKey(POINTS_KEY_NAME);
        pointsKey.setDisplayName("Holzfällerpunkte");
        pointsKey.setIsMonthlyStats(true);
    }

    public boolean addLogFarmed(UUID playerId, Material material) {
        WoodType type = WoodType.fromMaterial(material);
        if (type == null) {
            return false;
        }

        PlayerStatistics playerStatistics = statistics.getStatistics(playerId);
        playerStatistics.increaseScore(countKeys.get(type), 1);
        requestPointsRecalculation(playerId);
        return true;
    }

    public void getCurrentStats(UUID playerId, Consumer<WoodcutterStatsSnapshot> callback) {
        QueryBatch queryBatch = createQueryBatch(playerId, false);
        statistics.queryStats(queryBatch.queries(), result ->
                callback.accept(createSnapshot(queryBatch, result)));
    }

    private void requestPointsRecalculation(UUID playerId) {
        RecalculationState state = recalculationStates.computeIfAbsent(playerId, id -> new RecalculationState());
        if (state.running) {
            state.dirty = true;
            return;
        }

        state.running = true;
        reconcilePoints(playerId, state);
    }

    private void reconcilePoints(UUID playerId, RecalculationState state) {
        QueryBatch queryBatch = createQueryBatch(playerId, true);
        statistics.queryStats(queryBatch.queries(), result -> {
            WoodcutterStatsSnapshot snapshot = createSnapshot(queryBatch, result);
            int storedPoints = result.getOrDefault(queryBatch.pointsQuery(), 0);
            int difference = snapshot.getPoints() - storedPoints;
            if (difference != 0) {
                statistics.getStatistics(playerId).increaseScore(pointsKey, difference);
            }

            if (state.dirty) {
                state.dirty = false;
                reconcilePoints(playerId, state);
            } else {
                state.running = false;
                recalculationStates.remove(playerId, state);
            }
        });
    }

    private QueryBatch createQueryBatch(UUID playerId, boolean includePoints) {
        PlayerStatistics playerStatistics = statistics.getStatistics(playerId);
        EnumMap<WoodType, PlayerStatisticsQueryKey> countQueries = new EnumMap<>(WoodType.class);
        List<StatisticsQueryKey> queries = new ArrayList<>(WoodType.values().length + (includePoints ? 1 : 0));

        for (WoodType type : WoodType.values()) {
            PlayerStatisticsQueryKey query = new PlayerStatisticsQueryKey(
                    playerStatistics,
                    countKeys.get(type),
                    QueryType.SCORE,
                    TimeFrame.MONTH);
            countQueries.put(type, query);
            queries.add(query);
        }

        PlayerStatisticsQueryKey pointsQuery = null;
        if (includePoints) {
            pointsQuery = new PlayerStatisticsQueryKey(
                    playerStatistics,
                    pointsKey,
                    QueryType.SCORE,
                    TimeFrame.MONTH);
            queries.add(pointsQuery);
        }

        return new QueryBatch(countQueries, queries, pointsQuery);
    }

    private WoodcutterStatsSnapshot createSnapshot(
            QueryBatch queryBatch,
            Map<StatisticsQueryKey, Integer> result) {
        EnumMap<WoodType, Integer> counts = new EnumMap<>(WoodType.class);
        for (WoodType type : WoodType.values()) {
            counts.put(type, result.getOrDefault(queryBatch.countQueries().get(type), 0));
        }
        return new WoodcutterStatsSnapshot(counts);
    }

    private record QueryBatch(
            Map<WoodType, PlayerStatisticsQueryKey> countQueries,
            List<StatisticsQueryKey> queries,
            PlayerStatisticsQueryKey pointsQuery) {
    }

    private static final class RecalculationState {
        private boolean running;
        private boolean dirty;
    }
}
