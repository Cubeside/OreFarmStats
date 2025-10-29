package de.cubeside.orefarmstats.commands.lottery;

import de.cubeside.orefarmstats.OreFarmStatsPlugin;
import de.iani.cubesidestats.api.GlobalStatisticKey;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;

public class SetLotteryStatsKeysCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;

    public SetLotteryStatsKeysCommand(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("lottery.applicableStatKeys");
        if (section == null)
            section = plugin.getConfig().createSection("lottery.applicableStatKeys");

        String keysToAdd = args.getAll("");

        if (keysToAdd.isEmpty()) {
            sender.sendMessage(Component.text(commandString + this.getUsage()).color((NamedTextColor.DARK_RED)));
            return true;
        }

        String[] splitKeys = keysToAdd.split(" ");

        for (int i = 0; i < splitKeys.length; i++) {
            String[] keyAndScale = splitKeys[i].split(":");
            if (keyAndScale.length > 2 || keyAndScale.length == 0)
                continue;
            int scale = 1;
            try {
                scale = Integer.parseInt(keyAndScale[1]);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException ignored) {

            }
            section.set(keyAndScale[0].replace(".", "_"), scale == 0 ? null : scale);
        }
        plugin.saveConfig();
        sender.sendMessage(Component.text("Key für Lotterie gesetzt.").color(NamedTextColor.DARK_GREEN));

        return true;
    }

    @Override
    public ArrayList<String> onTabComplete(CommandSender sender, Command command, String alias, ArgsParser args) {
        int i = 0;
        while (args.hasNext()) {
            i++;
            args.next();
        }
        if (i > 0) {
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
        return "orefarmstats.lottery";
    }

    @Override
    public String getUsage() {
        return "<statsKey>:<scale> [<statsKey2>:<scale2> ...]";
    }
}
