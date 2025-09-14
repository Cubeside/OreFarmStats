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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

public class ListStatsDisplayCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;

    public ListStatsDisplayCommand(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        LinkedHashMap<UUID, List<String>> statsDisplays = plugin.getStatsDisplays();
        LinkedList<Component> components = new LinkedList<>();
        components.add(Component.text("Stats-Displays in Verwendung:", Style.style(NamedTextColor.DARK_GREEN, TextDecoration.UNDERLINED)));
        if (statsDisplays.isEmpty()) {
            components.add(Component.text("Keine").color(NamedTextColor.RED));
        } else {
            ListIterator<UUID> ids = new ArrayList<>(statsDisplays.keySet()).listIterator();
            while (ids.hasNext()) {
                int index = ids.nextIndex()+1;
                UUID id = ids.next();
                components.add(Component.text("Nr. ").color(NamedTextColor.GOLD).append(Component.text(index).color(NamedTextColor.YELLOW)).append(Component.text(" - " + id).color(NamedTextColor.GOLD)));
                components.add(Component.text(String.join("\n", statsDisplays.get(id))));
            }
        }
        sender.sendMessage(plugin.getCombinedText(components, "\n"));

        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "orefarmstats.display";
    }

    @Override
    public String getUsage() {
        return "";
    }
}
