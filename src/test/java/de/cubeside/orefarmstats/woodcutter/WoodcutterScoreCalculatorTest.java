package de.cubeside.orefarmstats.woodcutter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WoodcutterScoreCalculatorTest {
    @Test
    void noWoodProducesNoPoints() {
        assertEquals(0, WoodcutterScoreCalculator.calculate(Map.of()));
    }

    @Test
    void perfectSquareProducesItsRoot() {
        EnumMap<WoodType, Integer> counts = new EnumMap<>(WoodType.class);
        counts.put(WoodType.OAK, 16);

        assertEquals(4, WoodcutterScoreCalculator.calculate(counts));
    }

    @Test
    void nonPerfectSquareIsRoundedDown() {
        EnumMap<WoodType, Integer> counts = new EnumMap<>(WoodType.class);
        counts.put(WoodType.OAK, 8);

        assertEquals(2, WoodcutterScoreCalculator.calculate(counts));
    }

    @Test
    void roundsOnlyAfterAddingAllRoots() {
        EnumMap<WoodType, Integer> counts = new EnumMap<>(WoodType.class);
        counts.put(WoodType.OAK, 2);
        counts.put(WoodType.BIRCH, 3);

        assertEquals(3, WoodcutterScoreCalculator.calculate(counts));
    }

    @Test
    void nonPositiveCountsDoNotContributePoints() {
        EnumMap<WoodType, Integer> counts = new EnumMap<>(WoodType.class);
        counts.put(WoodType.OAK, -5);
        counts.put(WoodType.BIRCH, 0);
        counts.put(WoodType.SPRUCE, 1);

        assertEquals(1, WoodcutterScoreCalculator.calculate(counts));
    }
}
