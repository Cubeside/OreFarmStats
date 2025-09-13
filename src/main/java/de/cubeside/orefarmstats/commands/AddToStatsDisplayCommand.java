package de.cubeside.orefarmstats.commands;

import de.cubeside.orefarmstats.OreFarmStatsPlugin;
import de.iani.cubesidestats.api.GlobalStatisticKey;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public class AddToStatsDisplayCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;

    public AddToStatsDisplayCommand(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Du musst ein Spieler sein, um diesen Command auszuführen.").color(NamedTextColor.RED));
            return true;
        }

        if (!args.hasNext()) {
            sender.sendMessage(Component.text(commandString + this.getUsage()).color((NamedTextColor.DARK_RED)));
            return true;
        }

        String key = args.getNext();
        if (!plugin.existGlobalStatsKey(key)) {
            sender.sendMessage(Component.text("Key existiert nicht.").color(NamedTextColor.RED));
            return true;
        }

        int nr = args.getNext(-1) - 1;
        if (nr < 0) {
            sender.sendMessage(Component.text("Dieses Display existiert nicht.").color(NamedTextColor.RED));
            return true;
        }
        TreeMap<UUID, List<String>> keysByStatDisplays = plugin.getKeysByStatDisplays();
        List<UUID> keys = new ArrayList<>(keysByStatDisplays.keySet());
        if (!keys.isEmpty() && nr < keys.size()) {
            plugin.removeDisplayEntity(keys.get(nr));
            sender.sendMessage(Component.text("Display entfernt.").color(NamedTextColor.DARK_GREEN));
        }

        plugin.createDisplayEntity(player.getLocation(), key);
        sender.sendMessage(Component.text("Stats-Display wurde gespawnt.").color(NamedTextColor.DARK_GREEN));
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
            Set<String> inUse = plugin.getStatsKeysInUse();
            for (GlobalStatisticKey gsk : plugin.getGlobalStatsKeys()) {
                if (!inUse.contains(gsk.getName())) {
                    str.add(gsk.getName());
                }
            }
            return str;
        } else if (i == 2) {
            ArrayList<String> str = new ArrayList<>();
            int amount = plugin.amountStatsDisplays();
            for (int id = 1; id <= amount; id++) {
                str.add(String.valueOf(id));
            }
            return str;
        }
        return new ArrayList<>();
    }

    @Override
    public String getRequiredPermission() {
        return "orefarmstats.display.admin";
    }

    @Override
    public String getUsage() {
        return "<statsKey>";
    }
}
