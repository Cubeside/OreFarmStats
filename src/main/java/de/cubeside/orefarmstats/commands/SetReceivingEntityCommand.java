package de.cubeside.orefarmstats.commands;

import de.cubeside.orefarmstats.OreFarmStatsPlugin;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class SetReceivingEntityCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;

    public SetReceivingEntityCommand(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean requiresPlayer() {
        return true;
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Du bist keiner Spieler!", NamedTextColor.DARK_RED));
            return true;
        }

        Entity target = ((Player) sender).getTargetEntity(5);
        if (target == null) {
            sender.sendMessage(Component.text("Du musst ein Entity angucken, welches zur Itemabgabe dienen soll!", NamedTextColor.DARK_RED));
            return true;
        }
        plugin.setReceivingEntity(target);
        sender.sendMessage(Component.text("Du hast das das Entity zur Itemabgabe geändert!", NamedTextColor.DARK_GREEN));
        return true;
    }
}
