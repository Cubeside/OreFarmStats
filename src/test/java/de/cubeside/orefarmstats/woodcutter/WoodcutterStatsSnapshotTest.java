package de.cubeside.orefarmstats.woodcutter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WoodcutterStatsSnapshotTest {
    @Test
    void fillsMissingWoodTypesWithZeroAndCalculatesPoints() {
        WoodcutterStatsSnapshot snapshot =
                new WoodcutterStatsSnapshot(Map.of(WoodType.OAK, 9));

        assertEquals(9, snapshot.getCount(WoodType.OAK));
        assertEquals(0, snapshot.getCount(WoodType.WARPED));
        assertEquals(3, snapshot.getPoints());
        assertEquals(11, snapshot.getCounts().size());
    }

    @Test
    void clampsNegativeStoredCountsToZero() {
        EnumMap<WoodType, Integer> counts = new EnumMap<>(WoodType.class);
        counts.put(WoodType.OAK, -10);
        counts.put(WoodType.CRIMSON, 4);

        WoodcutterStatsSnapshot snapshot = new WoodcutterStatsSnapshot(counts);

        assertEquals(0, snapshot.getCount(WoodType.OAK));
        assertEquals(2, snapshot.getPoints());
    }
}
