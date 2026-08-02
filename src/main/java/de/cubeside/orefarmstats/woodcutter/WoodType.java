package de.cubeside.orefarmstats.woodcutter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public enum WoodType {
    OAK("oak", "Eiche", "Eichenstämme gefällt", Material.OAK_LOG, Material.STRIPPED_OAK_LOG),
    SPRUCE("spruce", "Fichte", "Fichtenstämme gefällt", Material.SPRUCE_LOG, Material.STRIPPED_SPRUCE_LOG),
    BIRCH("birch", "Birke", "Birkenstämme gefällt", Material.BIRCH_LOG, Material.STRIPPED_BIRCH_LOG),
    JUNGLE("jungle", "Tropenholz", "Tropenbaumstämme gefällt", Material.JUNGLE_LOG, Material.STRIPPED_JUNGLE_LOG),
    ACACIA("acacia", "Akazie", "Akazienstämme gefällt", Material.ACACIA_LOG, Material.STRIPPED_ACACIA_LOG),
    DARK_OAK("dark_oak", "Schwarzeiche", "Schwarzeichenstämme gefällt", Material.DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_LOG),
    MANGROVE("mangrove", "Mangrove", "Mangrovenstämme gefällt", Material.MANGROVE_LOG, Material.STRIPPED_MANGROVE_LOG),
    CHERRY("cherry", "Kirsche", "Kirschstämme gefällt", Material.CHERRY_LOG, Material.STRIPPED_CHERRY_LOG),
    PALE_OAK("pale_oak", "Blasseiche", "Blasseichenstämme gefällt", Material.PALE_OAK_LOG, Material.STRIPPED_PALE_OAK_LOG),
    CRIMSON("crimson", "Karmesin", "Karmesinstämme gefällt", Material.CRIMSON_STEM, Material.STRIPPED_CRIMSON_STEM),
    WARPED("warped", "Wirr", "Wirrstämme gefällt", Material.WARPED_STEM, Material.STRIPPED_WARPED_STEM);

    private static final Map<Material, WoodType> TYPES_BY_MATERIAL;

    static {
        EnumMap<Material, WoodType> typesByMaterial = new EnumMap<>(Material.class);
        for (WoodType type : values()) {
            for (Material material : type.materials) {
                WoodType previous = typesByMaterial.put(material, type);
                if (previous != null) {
                    throw new IllegalStateException(material + " is assigned to multiple wood types");
                }
            }
        }
        TYPES_BY_MATERIAL = Collections.unmodifiableMap(typesByMaterial);
    }

    private final String keySuffix;
    private final String displayName;
    private final String statisticDisplayName;
    private final Set<Material> materials;

    WoodType(String keySuffix, String displayName, String statisticDisplayName, Material... materials) {
        this.keySuffix = keySuffix;
        this.displayName = displayName;
        this.statisticDisplayName = statisticDisplayName;
        this.materials = Collections.unmodifiableSet(EnumSet.of(materials[0], materials));
    }

    public String getKeySuffix() {
        return keySuffix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStatisticDisplayName() {
        return statisticDisplayName;
    }

    public Set<Material> getMaterials() {
        return materials;
    }

    public static @Nullable WoodType fromMaterial(Material material) {
        return TYPES_BY_MATERIAL.get(material);
    }
}
