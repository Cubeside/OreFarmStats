package de.cubeside.orefarmstats.commands;

import de.cubeside.orefarmstats.OreFarmStatsPlugin;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class RemoveReceivingEntityCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;

    public RemoveReceivingEntityCommand(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean requiresPlayer() {
        return false;
    }

    @Override
    public String getRequiredPermission() {
        return "orefarmstats.admin";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        plugin.setReceivingEntity(null);
        sender.sendMessage(Component.text("Du hast das das Entity zur Itemabgabe entfernt!", NamedTextColor.DARK_GREEN));
        return true;
    }
}
