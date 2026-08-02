package de.cubeside.orefarmstats.woodcutter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.iani.cubesidestats.api.Callback;
import de.iani.cubesidestats.api.CubesideStatisticsAPI;
import de.iani.cubesidestats.api.PlayerStatistics;
import de.iani.cubesidestats.api.PlayerStatisticsQueryKey;
import de.iani.cubesidestats.api.StatisticKey;
import de.iani.cubesidestats.api.StatisticsQueryKey;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class WoodcutterStatsManagerTest {
    @Test
    void coalescesRapidUpdatesWithoutAwardingPointsTwice() {
        FakeStatistics fakeStatistics = new FakeStatistics();
        WoodcutterStatsManager manager = new WoodcutterStatsManager(fakeStatistics.api());
        UUID playerId = UUID.randomUUID();

        for (int i = 0; i < 4; i++) {
            assertTrue(manager.addLogFarmed(playerId, Material.OAK_LOG));
        }

        assertEquals(1, fakeStatistics.pendingQueries());
        fakeStatistics.completeNextQuery();
        assertEquals(1, fakeStatistics.score(playerId, WoodcutterStatsManager.POINTS_KEY_NAME));
        assertEquals(1, fakeStatistics.pendingQueries());

        fakeStatistics.completeNextQuery();
        assertEquals(4, fakeStatistics.score(playerId, WoodcutterStatsManager.KEY_PREFIX + "oak"));
        assertEquals(2, fakeStatistics.score(playerId, WoodcutterStatsManager.POINTS_KEY_NAME));
        assertEquals(0, fakeStatistics.pendingQueries());
    }

    @Test
    void recalculatesUsingAllWoodTypesAfterRapidMixedUpdates() {
        FakeStatistics fakeStatistics = new FakeStatistics();
        WoodcutterStatsManager manager = new WoodcutterStatsManager(fakeStatistics.api());
        UUID playerId = UUID.randomUUID();

        manager.addLogFarmed(playerId, Material.OAK_LOG);
        manager.addLogFarmed(playerId, Material.OAK_LOG);
        manager.addLogFarmed(playerId, Material.BIRCH_LOG);
        manager.addLogFarmed(playerId, Material.STRIPPED_BIRCH_LOG);
        manager.addLogFarmed(playerId, Material.BIRCH_LOG);

        fakeStatistics.completeNextQuery();
        fakeStatistics.completeNextQuery();

        assertEquals(2, fakeStatistics.score(playerId, WoodcutterStatsManager.KEY_PREFIX + "oak"));
        assertEquals(3, fakeStatistics.score(playerId, WoodcutterStatsManager.KEY_PREFIX + "birch"));
        assertEquals(3, fakeStatistics.score(playerId, WoodcutterStatsManager.POINTS_KEY_NAME));
    }

    @Test
    void excludedWoodDoesNotCreateStatsOrPointQueries() {
        FakeStatistics fakeStatistics = new FakeStatistics();
        WoodcutterStatsManager manager = new WoodcutterStatsManager(fakeStatistics.api());
        UUID playerId = UUID.randomUUID();

        assertFalse(manager.addLogFarmed(playerId, Material.OAK_WOOD));

        assertEquals(0, fakeStatistics.pendingQueries());
        assertEquals(0, fakeStatistics.score(playerId, WoodcutterStatsManager.KEY_PREFIX + "oak"));
    }

    private static final class FakeStatistics {
        private final Map<String, StatisticKey> keysByName = new LinkedHashMap<>();
        private final Map<UUID, PlayerStatistics> playersById = new HashMap<>();
        private final Map<UUID, Map<StatisticKey, Integer>> scoresByPlayer = new HashMap<>();
        private final Queue<Runnable> pendingQueries = new ArrayDeque<>();
        private final CubesideStatisticsAPI api;

        private FakeStatistics() {
            api = proxy(CubesideStatisticsAPI.class, this::invokeApi);
        }

        private CubesideStatisticsAPI api() {
            return api;
        }

        private Object invokeApi(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "getStatisticKey" -> key((String) args[0]);
                case "getStatistics" -> player((UUID) args[0]);
                case "queryStats" -> {
                    if (args.length != 2) {
                        throw new UnsupportedOperationException(method.toString());
                    }
                    queueQuery(
                            castQueries(args[0]),
                            castCallback(args[1]));
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            };
        }

        private StatisticKey key(String name) {
            return keysByName.computeIfAbsent(name, keyName ->
                    proxy(StatisticKey.class, (proxy, method, args) -> switch (method.getName()) {
                        case "getName", "getDisplayName" -> keyName;
                        case "isMonthlyStats" -> true;
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> keyName;
                        default -> defaultValue(method.getReturnType());
                    }));
        }

        private PlayerStatistics player(UUID playerId) {
            return playersById.computeIfAbsent(playerId, id ->
                    proxy(PlayerStatistics.class, (proxy, method, args) -> switch (method.getName()) {
                        case "getOwner" -> id;
                        case "increaseScore" -> {
                            scores(id).merge((StatisticKey) args[0], (Integer) args[1], Integer::sum);
                            yield null;
                        }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> id.hashCode();
                        case "toString" -> id.toString();
                        default -> defaultValue(method.getReturnType());
                    }));
        }

        private Map<StatisticKey, Integer> scores(UUID playerId) {
            return scoresByPlayer.computeIfAbsent(playerId, id -> new HashMap<>());
        }

        private void queueQuery(
                Collection<StatisticsQueryKey> queries,
                Callback<Map<StatisticsQueryKey, Integer>> callback) {
            Map<StatisticsQueryKey, Integer> result = new HashMap<>();
            for (StatisticsQueryKey query : queries) {
                PlayerStatisticsQueryKey playerQuery = (PlayerStatisticsQueryKey) query;
                Integer score = scores(playerQuery.getPlayer().getOwner()).get(playerQuery.getKey());
                if (score != null) {
                    result.put(query, score);
                }
            }
            pendingQueries.add(() -> callback.call(result));
        }

        private int score(UUID playerId, String keyName) {
            StatisticKey key = keysByName.get(keyName);
            if (key == null) {
                return 0;
            }
            return scores(playerId).getOrDefault(key, 0);
        }

        private int pendingQueries() {
            return pendingQueries.size();
        }

        private void completeNextQuery() {
            pendingQueries.remove().run();
        }

        @SuppressWarnings("unchecked")
        private static Collection<StatisticsQueryKey> castQueries(Object queries) {
            return (Collection<StatisticsQueryKey>) queries;
        }

        @SuppressWarnings("unchecked")
        private static Callback<Map<StatisticsQueryKey, Integer>> castCallback(Object callback) {
            return (Callback<Map<StatisticsQueryKey, Integer>>) callback;
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[] {type},
                    handler);
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive() || type == void.class) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0F;
            }
            if (type == double.class) {
                return 0D;
            }
            if (type == char.class) {
                return '\0';
            }
            throw new AssertionError(type);
        }
    }
}
