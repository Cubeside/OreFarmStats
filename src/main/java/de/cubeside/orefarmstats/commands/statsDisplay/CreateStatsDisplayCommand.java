package de.cubeside.orefarmstats.commands.statsDisplay;

import de.cubeside.orefarmstats.StatsDisplayManager;
import de.iani.cubesidestats.api.GlobalStatisticKey;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateStatsDisplayCommand extends SubCommand {

    private final StatsDisplayManager statsDisplayManager;

    public CreateStatsDisplayCommand(StatsDisplayManager statsDisplayManager) {
        this.statsDisplayManager = statsDisplayManager;
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
        if (!statsDisplayManager.hasGlobalStatsKey(statsKey)) {
            sender.sendMessage(Component.text("Key existiert nicht.").color(NamedTextColor.RED));
            return true;
        }

        statsDisplayManager.createDisplayEntity(player.getLocation(), statsKey);
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
            for (GlobalStatisticKey gsk : statsDisplayManager.getGlobalStatsKeys()) {
                str.add(gsk.getName());
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
        return "<statsKey>";
    }
}
