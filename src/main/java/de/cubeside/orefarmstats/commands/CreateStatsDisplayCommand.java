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

public class CreateStatsDisplayCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;

    public CreateStatsDisplayCommand(OreFarmStatsPlugin plugin) {
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

        String statsKey = args.getNext();
        if (!plugin.existGlobalStatsKey(statsKey)) {
            sender.sendMessage(Component.text("Key existiert nicht.").color(NamedTextColor.RED));
            return true;
        }

        plugin.createDisplayEntity(player.getLocation(), statsKey);
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
            for (GlobalStatisticKey gsk : plugin.getGlobalStatsKeys()) {
                    str.add(gsk.getName());
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
