package de.cubeside.orefarmstats.commands.statsDisplay;

import de.cubeside.orefarmstats.StatsDisplayManager;
import de.iani.cubesidestats.api.GlobalStatisticKey;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class RemoveFromStatsDisplayCommand extends SubCommand {

    private final StatsDisplayManager statsDisplayManager;

    public RemoveFromStatsDisplayCommand(StatsDisplayManager statsDisplayManager) {
        this.statsDisplayManager = statsDisplayManager;
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

        if (!args.hasNext()) {
            sender.sendMessage(Component.text(commandString + this.getUsage()).color((NamedTextColor.DARK_RED)));
            return true;
        }
        String statsKey = args.getNext();
        if (!statsDisplayManager.hasGlobalStatsKey(statsKey)) {
            sender.sendMessage(Component.text("Key existiert nicht.").color(NamedTextColor.RED));
            return true;
        }

        LinkedHashMap<UUID, List<String>> statsDisplays = statsDisplayManager.getStatsDisplaysCopy();
        List<UUID> ids = new ArrayList<>(statsDisplays.keySet());
        if (!ids.isEmpty() && nr < ids.size()) {
            UUID id = ids.get(nr);
            if (!statsDisplays.get(id).contains(statsKey)) {
                sender.sendMessage(Component.text("Dieser Key ist nicht im Display vorhanden.").color(NamedTextColor.RED));
                return true;
            }
            if (statsDisplayManager.removeStatFromStatsDisplay(statsKey, id)) {
                sender.sendMessage(Component.text("Key vom Display entfernt.").color(NamedTextColor.DARK_GREEN));
            }
        } else {
            sender.sendMessage(Component.text("Dieses Display existiert nicht.").color(NamedTextColor.RED));
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
        if (i == 2) {
            ArrayList<String> str = new ArrayList<>();
            for (GlobalStatisticKey gsk : statsDisplayManager.getGlobalStatsKeys()) {
                str.add(gsk.getName());
            }
            return str;
        } else if (i == 1) {
            ArrayList<String> str = new ArrayList<>();
            int amount = statsDisplayManager.amountStatsDisplays();
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
        return "<id> <statsKey>";
    }
}
