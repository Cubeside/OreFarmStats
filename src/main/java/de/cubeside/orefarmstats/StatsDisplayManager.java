package de.cubeside.orefarmstats;

import de.iani.cubesidestats.api.CubesideStatisticsAPI;
import de.iani.cubesidestats.api.GlobalStatisticKey;
import de.iani.cubesidestats.api.TimeFrame;
import de.iani.cubesideutils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsDisplayManager {
    private final JavaPlugin plugin;
    private final CubesideStatisticsAPI cubesideStatistics;
    private final LinkedHashMap<UUID, List<String>> statsDisplays;
    private final HashMap<UUID, String> displayHeadlines;
    private final HashMap<Pair<UUID, String>, String> statsMessages;
    private final ConcurrentHashMap<UUID, Component> timeFrameStats;
    private final String configPrefix;

    public StatsDisplayManager(JavaPlugin plugin, CubesideStatisticsAPI cubesideStatistics) {
        this(plugin, cubesideStatistics, "statsDisplays");
    }

    public StatsDisplayManager(JavaPlugin plugin, CubesideStatisticsAPI cubesideStatistics, String configPrefix) {
        this.plugin = plugin;
        this.cubesideStatistics = cubesideStatistics;
        statsDisplays = new LinkedHashMap<>();
        statsMessages = new HashMap<>();
        displayHeadlines = new HashMap<>();
        timeFrameStats = new ConcurrentHashMap<>();
        this.configPrefix = configPrefix;

        loadFromConfig();
    }

    public Collection<? extends GlobalStatisticKey> getGlobalStatsKeys() {
        return cubesideStatistics.getAllGlobalStatisticKeys();
    }

    public boolean hasGlobalStatsKey(String key) {
        return cubesideStatistics.hasGlobalStatisticKey(key);
    }

    public CubesideStatisticsAPI getStatistics() {
        return cubesideStatistics;
    }

    public int amountStatsDisplays() {
        return statsDisplays.size();
    }

    public LinkedHashMap<UUID, List<String>> getStatsDisplaysCopy() {
        return new LinkedHashMap<>(statsDisplays);
    }

    public HashMap<UUID, String> getDisplayHeadlinesCopy() {
        return new HashMap<>(displayHeadlines);
    }

    public HashMap<Pair<UUID, String>, String> getStatsMessagesCopy() {
        return new HashMap<>(statsMessages);
    }

    private void loadFromConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(configPrefix);
        if (section != null) {
            section.getKeys(false).forEach(id -> {
                UUID uuid = UUID.fromString(id);
                Set<String> escapedStatsKeys = section.getConfigurationSection(id + ".statsKeys").getKeys(false);
                List<String> statsKeys = new ArrayList<>();
                escapedStatsKeys.forEach(key -> {
                    String statsKey = key.replace("_", ".");
                    statsKeys.add(statsKey);
                    statsMessages.put(new Pair<>(uuid, statsKey), section.getString(id + ".statsKeys." + key));
                });
                statsDisplays.put(uuid, statsKeys);
                displayHeadlines.put(uuid, section.getString(id + ".headline"));
            });
        }
        statsDisplays.keySet().forEach(this::updateStatsComponent);
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            updateDisplayEntities();
            statsDisplays.keySet().forEach(this::updateStatsComponent);
        }, 20 * 10, 20 * 10);
    }

    public void createDisplayEntity(Location location, String statsKey) {
        TextDisplay textDisplay = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY, CreatureSpawnEvent.SpawnReason.CUSTOM);
        textDisplay.setShadowed(true);
        textDisplay.setLineWidth(300);
        textDisplay.setAlignment(TextDisplay.TextAlignment.LEFT);

        UUID id = textDisplay.getUniqueId();
        String headline = "&#FF1493&lInsgesamt: ";
        String message = "&e%d &6" + cubesideStatistics.getGlobalStatisticKey(statsKey, false).getDisplayName();

        List<String> newList = new ArrayList<>();
        newList.add(statsKey);
        statsDisplays.put(id, newList);
        statsMessages.put(new Pair<>(id, statsKey), message);
        displayHeadlines.put(id, headline);

        String escapedKey = statsKey.replace(".", "_");
        String section = configPrefix + "." + id;
        ConfigurationSection configSectionIDs = plugin.getConfig().createSection(section);
        configSectionIDs.set("headline", headline);
        ConfigurationSection configSectionKeys = configSectionIDs.createSection("statsKeys");
        configSectionKeys.set(escapedKey, message);
        plugin.saveConfig();

        updateStatsComponent(id);
    }

    public boolean addStatToStatsDisplay(String statsKey, UUID id) {
        if (statsDisplays.containsKey(id)) {
            statsDisplays.get(id).add(statsKey);
            String escapedKey = statsKey.replace(".", "_");
            String message = "&e%d &6" + cubesideStatistics.getGlobalStatisticKey(statsKey, false).getDisplayName();
            statsMessages.put(new Pair<>(id, statsKey), message);

            plugin.getConfig().set(String.format("%s.%s.statsKeys.%s", configPrefix, id, escapedKey), message);
            plugin.saveConfig();

            updateStatsComponent(id);
            return true;
        }
        return false;
    }

    public boolean removeStatFromStatsDisplay(String statsKey, UUID id) {
        if (statsDisplays.containsKey(id)) {
            statsDisplays.get(id).remove(statsKey);
            String escapedKey = statsKey.replace(".", "_");
            statsMessages.remove(new Pair<>(id, statsKey));

            plugin.getConfig().set(String.format("%s.%s.statsKeys.%s", configPrefix, id, escapedKey), null);
            plugin.saveConfig();

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
                statsKeys.forEach(statsKey -> statsMessages.remove(new Pair<>(id, statsKey)));
                displayHeadlines.remove(id);
                plugin.getConfig().set(String.format("%s.%s", configPrefix, id), null);
            }
        });
        plugin.saveConfig();
    }

    public void setHeadline(UUID id, String headline) {
        if (statsDisplays.containsKey(id)) {
            headline = headline.replaceAll("&x", "&#");
            displayHeadlines.put(id, headline);
            plugin.getConfig().set(String.format("%s.%s.headline", configPrefix, id), headline);
            plugin.saveConfig();

            updateStatsComponent(id);
        }
    }

    public boolean setMessage(UUID id, String statsKey, String message) {
        if (statsDisplays.containsKey(id) && message.contains("%d")) {
            message = message.replaceAll("&x", "&#");
            statsMessages.put(new Pair<>(id, statsKey), message);
            String escapedKey = statsKey.replace(".", "_");
            plugin.getConfig().set(String.format("%s.%s.statsKeys.%s", configPrefix, id, escapedKey), message);
            plugin.saveConfig();

            updateStatsComponent(id);
            return true;
        }
        return false;
    }

    public Component getCombinedText(LinkedList<Component> components, String separator) {
        Component combinedText = Component.empty();
        boolean first = true;
        for (Component component : components) {
            if (!first) {
                combinedText = combinedText.append(Component.text(separator));
            }
            first = false;
            combinedText = combinedText.append(component);
        }
        return combinedText;
    }

    public void updateStatsComponent(UUID id) {
        LinkedList<Component> components = new LinkedList<>();
        components.add(getTextComponent(displayHeadlines.get(id)));

        List<String> statsKeys = statsDisplays.get(id);
        if (statsKeys.isEmpty()) {
            timeFrameStats.put(id, getCombinedText(components, "\n"));
            updateDisplayEntity(id);
            return;
        }
        statsKeys.forEach(statsKey -> {
            GlobalStatisticKey key = cubesideStatistics.getGlobalStatisticKey(statsKey, false);
            if (key != null) {
                cubesideStatistics.getGlobalStatistics().getValue(key, TimeFrame.ALL_TIME, allTime -> {
                    String text = statsMessages.get(new Pair<>(id, statsKey));
                    text = text.replaceAll("%", "%%").replaceFirst("%%d", "%d");
                    text = String.format(text, allTime);
                    components.add(getTextComponent(text));
                    timeFrameStats.put(id, getCombinedText(components, "\n"));
                    updateDisplayEntity(id);
                });
            }
        });
    }

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

        Component component = timeFrameStats.getOrDefault(id, Component.empty());

        textDisplay.text(component);
    }

    private String getLegacyString(TextComponent text) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(text);
    }

    private TextComponent getTextComponent(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
