package de.cubeside.orefarmstats.commands;

import de.cubeside.orefarmstats.OreFarmStatsPlugin;
import de.cubeside.orefarmstats.woodcutter.WoodType;
import de.cubeside.orefarmstats.woodcutter.WoodcutterStatsManager;
import de.cubeside.orefarmstats.woodcutter.WoodcutterStatsSnapshot;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.playerUUIDCache.CachedPlayer;
import de.iani.playerUUIDCache.PlayerUUIDCacheAPI;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WoodcutterStatsCommand extends SubCommand {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN);

    private final OreFarmStatsPlugin plugin;
    private final WoodcutterStatsManager statsManager;

    public WoodcutterStatsCommand(OreFarmStatsPlugin plugin, WoodcutterStatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        String requestedName;
        if (!args.hasNext()) {
            if (sender instanceof Player player) {
                requestedName = player.getName();
            } else {
                sendUsage(sender, commandString);
                return true;
            }
        } else {
            requestedName = args.getNext();
        }
        if (args.hasNext()) {
            sendUsage(sender, commandString);
            return true;
        }

        ResolvedPlayer resolvedPlayer = resolvePlayer(requestedName);
        if (resolvedPlayer == null) {
            sender.sendMessage(Component.text("Der Spieler " + requestedName + " ist unbekannt.", NamedTextColor.RED));
            return true;
        }

        statsManager.getCurrentStats(
                resolvedPlayer.playerId(),
                snapshot -> sendStats(sender, resolvedPlayer.name(), snapshot));
        return true;
    }

    private ResolvedPlayer resolvePlayer(String playerName) {
        PlayerUUIDCacheAPI playerUUIDCache = plugin.getServer().getServicesManager().load(PlayerUUIDCacheAPI.class);
        if (playerUUIDCache != null) {
            CachedPlayer cachedPlayer = playerUUIDCache.getPlayer(playerName);
            if (cachedPlayer != null) {
                return new ResolvedPlayer(cachedPlayer.getUniqueId(), cachedPlayer.getName());
            }
        }

        Player onlinePlayer = plugin.getServer().getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return new ResolvedPlayer(onlinePlayer.getUniqueId(), onlinePlayer.getName());
        }

        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayerIfCached(playerName);
        if (offlinePlayer != null && offlinePlayer.getName() != null) {
            return new ResolvedPlayer(offlinePlayer.getUniqueId(), offlinePlayer.getName());
        }
        return null;
    }

    private void sendStats(CommandSender sender, String playerName, WoodcutterStatsSnapshot snapshot) {
        Component message = Component.empty().append(Component.text(
                "Holzfäller-Statistik für " + playerName + " (" + YearMonth.now().format(MONTH_FORMATTER) + ")",
                Style.style(NamedTextColor.DARK_GREEN, TextDecoration.UNDERLINED)));

        for (WoodType type : WoodType.values()) {
            message = message
                    .append(Component.newline())
                    .append(Component.translatable(type.getDisplayMaterial().translationKey())
                            .color(NamedTextColor.GOLD))
                    .append(Component.text(": ", NamedTextColor.GOLD))
                    .append(Component.text(snapshot.getCount(type), NamedTextColor.YELLOW));
        }

        message = message
                .append(Component.newline())
                .append(Component.text("Punkte: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(snapshot.getPoints(), NamedTextColor.GREEN));
        sender.sendMessage(message);
    }

    private void sendUsage(CommandSender sender, String commandString) {
        sender.sendMessage(Component.text(commandString + getUsage(), NamedTextColor.DARK_RED));
    }

    @Override
    public ArrayList<String> onTabComplete(CommandSender sender, Command command, String alias, ArgsParser args) {
        int argumentCount = 0;
        while (args.hasNext()) {
            args.next();
            argumentCount++;
        }
        if (argumentCount != 1) {
            return new ArrayList<>();
        }

        ArrayList<String> names = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            names.add(player.getName());
        }
        names.sort(Comparator.comparing(String::toLowerCase));
        return names;
    }

    @Override
    public String getRequiredPermission() {
        return "orefarmstats.woodcutterstats";
    }

    @Override
    public String getUsage() {
        return "<spielername>";
    }

    private record ResolvedPlayer(UUID playerId, String name) {}
}
