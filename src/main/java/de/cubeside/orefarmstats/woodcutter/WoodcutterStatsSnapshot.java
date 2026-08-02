package de.cubeside.orefarmstats.woodcutter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class WoodcutterStatsSnapshot {
    private final Map<WoodType, Integer> counts;
    private final int points;

    public WoodcutterStatsSnapshot(Map<WoodType, Integer> counts) {
        EnumMap<WoodType, Integer> completeCounts = new EnumMap<>(WoodType.class);
        for (WoodType type : WoodType.values()) {
            completeCounts.put(type, Math.max(0, counts.getOrDefault(type, 0)));
        }
        this.counts = Collections.unmodifiableMap(completeCounts);
        this.points = WoodcutterScoreCalculator.calculate(completeCounts);
    }

    public Map<WoodType, Integer> getCounts() {
        return counts;
    }

    public int getCount(WoodType type) {
        return counts.get(type);
    }

    public int getPoints() {
        return points;
    }
}
