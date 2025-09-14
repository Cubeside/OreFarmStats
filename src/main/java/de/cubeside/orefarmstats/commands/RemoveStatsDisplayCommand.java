package de.cubeside.orefarmstats.commands;

import de.cubeside.orefarmstats.OreFarmStatsPlugin;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class RemoveStatsDisplayCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;

    public RemoveStatsDisplayCommand(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        if (!args.hasNext()) {
            sender.sendMessage(Component.text(commandString + this.getUsage()).color((NamedTextColor.DARK_RED)));
            return true;
        }

        int nr = args.getNext(-1) - 1;
        if (nr < 0) {
            sender.sendMessage(Component.text("Dieses Display existiert nicht.").color(NamedTextColor.RED));
            return true;
        }
        LinkedHashMap<UUID, List<String>> keysByStatDisplays = plugin.getStatsDisplays();
        List<UUID> keys = new ArrayList<>(keysByStatDisplays.keySet());
        if (!keys.isEmpty() && nr < keys.size()) {
            plugin.removeDisplayEntity(keys.get(nr));
            sender.sendMessage(Component.text("Display entfernt.").color(NamedTextColor.DARK_GREEN));
        } else {
            sender.sendMessage(Component.text("Dieses Display existiert nicht.").color(NamedTextColor.RED));
            return true;
        }

        return true;
    }

    @Override
    public ArrayList<String> onTabComplete(CommandSender sender, Command command, String alias, ArgsParser args) {
        int i = 0;
        while (args.hasNext()) {
            i++;
            args.next();
        }
        if (i == 1) {
            ArrayList<String> str = new ArrayList<>();
            int amount = plugin.getStatsDisplays().size();
            for (int id = 1; id <= amount; id++) {
                str.add(String.valueOf(id));
            }
            return str;
        }
        return new ArrayList<>();
    }

    @Override
    public String getRequiredPermission() {
        return "orefarmstats.display";
    }

    @Override
    public String getUsage() {
        return "<id>";
    }
}
