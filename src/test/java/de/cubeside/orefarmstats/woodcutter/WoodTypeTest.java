package de.cubeside.orefarmstats.woodcutter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class WoodTypeTest {
    @Test
    void allElevenWoodTypesHaveNormalAndStrippedTrunks() {
        assertEquals(11, WoodType.values().length);

        Set<Material> mappedMaterials = EnumSet.noneOf(Material.class);
        for (WoodType type : WoodType.values()) {
            assertEquals(2, type.getMaterials().size());
            for (Material material : type.getMaterials()) {
                assertTrue(
                        material.name().endsWith("_LOG") || material.name().endsWith("_STEM"),
                        material + " is not a log or stem");
                assertTrue(mappedMaterials.add(material), material + " is mapped more than once");
                assertSame(type, WoodType.fromMaterial(material));
            }
        }
        assertEquals(22, mappedMaterials.size());
    }

    @Test
    void woodHyphaeAndBambooAreExcluded() {
        for (Material material : Set.of(
                Material.OAK_WOOD,
                Material.STRIPPED_OAK_WOOD,
                Material.CRIMSON_HYPHAE,
                Material.STRIPPED_CRIMSON_HYPHAE,
                Material.WARPED_HYPHAE,
                Material.STRIPPED_WARPED_HYPHAE,
                Material.BAMBOO,
                Material.BAMBOO_BLOCK,
                Material.STRIPPED_BAMBOO_BLOCK)) {
            assertNull(WoodType.fromMaterial(material), material + " must not be counted");
        }
    }
}
