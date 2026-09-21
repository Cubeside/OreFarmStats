package de.cubeside.orefarmstats.woodcutter;

import java.util.Map;

public final class WoodcutterScoreCalculator {
    private WoodcutterScoreCalculator() {
    }

    public static int calculate(Map<WoodType, Integer> counts) {
        double sum = 0;
        for (WoodType type : WoodType.values()) {
            Integer count = counts.get(type);
            if (count != null && count > 0) {
                sum += calculateForSingleType(count);
            }
        }
        return (int) Math.floor(sum);
    }

    public static double calculateForSingleType(int count) {
        return Math.sqrt(count);
    }
}
