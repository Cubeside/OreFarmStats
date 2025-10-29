package de.cubeside.orefarmstats.commands.lottery;

import de.cubeside.orefarmstats.OreFarmStatsPlugin;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedList;
import java.util.Map;

public class ListLotteryStatsKeysCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;

    public ListLotteryStatsKeysCommand(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("lottery.applicableStatKeys");
        if (section == null)
            section = plugin.getConfig().createSection("lottery.applicableStatKeys");

        LinkedList<Component> components = new LinkedList<>();
        components.add(Component.text("Stats-Keys für Lotterie:", Style.style(NamedTextColor.DARK_GREEN, TextDecoration.UNDERLINED)));

       Map<String, Object> keysValues = section.getValues(false);
        if (keysValues.isEmpty()) {
            components.add(Component.text("Keine").color(NamedTextColor.RED));
        } else {
            keysValues.forEach((key, value) -> {
                components.add(Component.text(key.replace("_", ".")).color(NamedTextColor.WHITE).append(Component.text(":").color(NamedTextColor.YELLOW).append(Component.text(value.toString()).color(NamedTextColor.GOLD))));
            });

        }
        sender.sendMessage(getCombinedText(components, "\n"));

        return true;
    }

    public Component getCombinedText(LinkedList<Component> components, String separator) {
        Component combinedText = Component.empty();
        boolean first = true;
        for (Component component : components) {
            if (!first) {
                combinedText = combinedText.append(Component.text(separator));
            }
            first = false;
            combinedText = combinedText.append(component);
        }
        return combinedText;
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
