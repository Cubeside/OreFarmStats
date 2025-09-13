package de.cubeside.orefarmstats.commands;

import de.cubeside.orefarmstats.OreFarmStatsPlugin;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.UUID;

public class ListStatsDisplayCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;

    public ListStatsDisplayCommand(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        TreeMap<UUID, List<String>> keysByStatDisplays = plugin.getKeysByStatDisplays();
        ListIterator<UUID> keys = new ArrayList<>(keysByStatDisplays.keySet()).listIterator();
        sender.sendMessage(Component.text("Stats-Displays in Verwendung:", Style.style(NamedTextColor.DARK_GREEN, TextDecoration.UNDERLINED)));
        while (keys.hasNext()) {
            int index = keys.nextIndex()+1;
            UUID key = keys.next();
            sender.sendMessage(Component.text("Nr. ").color(NamedTextColor.GOLD).append(Component.text(index).color(NamedTextColor.YELLOW)).append(Component.text(" - " + key).color(NamedTextColor.GOLD)));
            sender.sendMessage(Component.text(String.join(" ", keysByStatDisplays.get(key))));
        }
        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "orefarmstats.display.admin";
    }

    @Override
    public String getUsage() {
        return "";
    }
}
