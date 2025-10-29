package de.cubeside.orefarmstats.commands.lottery;

import de.cubeside.orefarmstats.OreFarmStatsPlugin;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ClearLotteryStatsKeysCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;

    public ClearLotteryStatsKeysCommand(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {

        plugin.getConfig().createSection("lottery.applicableStatKeys");
        plugin.saveConfig();
        sender.sendMessage(Component.text("Alle Keys für die Lotterie entfernt.").color(NamedTextColor.DARK_GREEN));

        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "orefarmstats.lottery";
    }

    @Override
    public String getUsage() {
        return "";
    }
}
