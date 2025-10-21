package de.cubeside.orefarmstats.commands.evaluation;

import de.cubeside.orefarmstats.OreFarmStatsPlugin;
import de.iani.cubesidestats.api.CubesideStatisticsAPI;
import de.iani.cubesidestats.api.GlobalStatisticKey;
import de.iani.cubesidestats.api.PlayerWithScore;
import de.iani.cubesidestats.api.StatisticKey;
import de.iani.cubesidestats.api.TimeFrame;
import de.iani.cubesideutils.HastebinUtil;
import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.playerUUIDCache.PlayerUUIDCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

public class DrawWinnerCommand extends SubCommand {

    private final OreFarmStatsPlugin plugin;
    private PlayerUUIDCache playerUUIDCache;
    private Map<UUID, Integer> addtionalParticipants;

    public DrawWinnerCommand(OreFarmStatsPlugin plugin) {
        this.plugin = plugin;
        this.playerUUIDCache = (PlayerUUIDCache) Bukkit.getServer().getPluginManager().getPlugin("PlayerUUIDCache");
        this.addtionalParticipants = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        // Zusätzliche Teilnehmer (Stimmenanteil = 1 / alle zusätzlichen Teilnehmer)
        String nextSubcommand = "";
        if (args.hasNext()) {
            nextSubcommand = args.getNext();
        }
        if (nextSubcommand.equals("addParticipants")) {
            if (!args.hasNext()) {
                sender.sendMessage(Component.text(commandString + this.getUsage()).color((NamedTextColor.DARK_RED)));
                return true;
            }
            HashMap<UUID, Integer> playersToAdd = new HashMap<>();
            int lastWeight = 1;
            while (args.hasNext()) {
                String arg = args.getNext();
                try {
                    lastWeight = Integer.parseInt(arg);
                } catch (NumberFormatException e) {
                    if (playerUUIDCache.getPlayer(arg) == null) {
                        sender.sendMessage(Component.text("Spieler " + arg + " ist unbekannt. Spieler wurden nicht hinzugefügt.").color(NamedTextColor.RED));
                        return true;
                    }
                    UUID playerId = playerUUIDCache.getPlayer(arg).getUniqueId();
                    playersToAdd.put(playerId, lastWeight);
                }
            }
            addtionalParticipants.putAll(playersToAdd);
            if (!playersToAdd.isEmpty()) {
                sender.sendMessage(Component.text("Spieler wurden hinzugefügt.").color((NamedTextColor.DARK_GREEN)));
            }
            return true;
        } else if (nextSubcommand.equals("listParticipants")) {
            Map<Integer, List<String>> playersToWeights = new HashMap<>();
            addtionalParticipants.forEach((playerId, weight) -> {
                playersToWeights.putIfAbsent(weight, new ArrayList<>());
                playersToWeights.get(weight).add(playerUUIDCache.getPlayer(playerId).getName());
            });
            sender.sendMessage(Component.text("Hinzugefügte Spieler:", Style.style(NamedTextColor.DARK_GREEN, TextDecoration.UNDERLINED)));
            playersToWeights.forEach((weight, players) ->
                sender.sendMessage(
                        Component.text(String.format("Gewicht %d: ", weight)).color(NamedTextColor.GOLD)
                                .append(Component.text(Arrays.toString(players.toArray())).color(NamedTextColor.WHITE))
                )
            );
            if (playersToWeights.isEmpty()) {
                sender.sendMessage(Component.text("Keine").color(NamedTextColor.RED));
            }
            return true;
        }
        // GlobalStatsKey und StatsKey müssen den selben Namen haben, sonst funktioniert es nicht
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("lottery");
        if (section == null) {
            sender.sendMessage(Component.text("Es existiert nicht die benötige Sektion \"lottery\" in der Config.").color(NamedTextColor.RED));
            return true;
        }

        CubesideStatisticsAPI cubesideStatistics = plugin.getStatsDisplayManager().getStatistics();
        if (cubesideStatistics == null) {
            sender.sendMessage(Component.text("CubesideStatisticsAPI aktuell nicht verfügbar.").color(NamedTextColor.RED));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, task -> {
            Map<String, Map<UUID, Double>> allPlayerScores = new HashMap<>();
            Map<UUID, Double> aP = new HashMap<>();
            double weightSum = addtionalParticipants.values().stream().reduce(0, Integer::sum);
            addtionalParticipants.forEach((playerId,weight) -> {
                aP.put(playerId, weight / weightSum);
            });
            allPlayerScores.put("extra", aP);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            ConfigurationSection keysValuesection = section.getConfigurationSection("applicableStatKeys");
            if (keysValuesection == null) {
                sender.sendMessage(Component.text("Es existiert nicht die benötige Sektion \"applicableStatKeys\" in der Config.").color(NamedTextColor.RED));
                task.cancel();
                return;
            }

            Map<String, Object> statsKeys = keysValuesection.getValues(false).entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().replace("_", "."),
                            Map.Entry::getValue));

            if (statsKeys.isEmpty()) {
                sender.sendMessage(Component.text("Es wurden keine Keys in der Config hinterlegt.").color(NamedTextColor.RED));
                task.cancel();
                return;
            }

            // Anteile der Spieler pro Disziplin berechnen
            for (String key : statsKeys.keySet()) {
                GlobalStatisticKey globalStatsKey = cubesideStatistics.getGlobalStatisticKey(key, false);
                if (globalStatsKey == null) {
                    continue;
                }
                CompletableFuture<Void> cf = new CompletableFuture<>();
                cubesideStatistics.getGlobalStatistics().getValue(globalStatsKey, TimeFrame.ALL_TIME, value -> {
                    StatisticKey statsKey = cubesideStatistics.getStatisticKey(key, false);
                    if (statsKey != null && value > 0) {
                        Map<UUID, Double> playerScores = new HashMap<>();
                        statsKey.getTop(256, TimeFrame.ALL_TIME, playersWithScores -> {
                            for (PlayerWithScore playerWithScore : playersWithScores) {
                                UUID playerId = playerWithScore.getPlayer().getOwner();
                                Object scaleFactor = statsKeys.get(key);
                                playerScores.put(playerId, (((double) playerWithScore.getScore()) / value) / ((scaleFactor instanceof Number n) ? n.doubleValue() : 1d));
                            }
                            allPlayerScores.put(key, playerScores);
                            cf.complete(null);
                        });
                    } else {
                        cf.complete(null);
                    }
                });
                futures.add(cf);
            }

            // Warten bis alle Spielerwerte berechnet wurden
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> Bukkit.getScheduler().runTask(plugin, taskFutures -> {
                Map<UUID, Double> playerScoreSums = new HashMap<>();

                // Spielerwerte der Disziplinen aufsummieren
                allPlayerScores.forEach((statsKey, playerScores) -> {
                    playerScores.forEach((playerId, score) -> {
                        double currentScore = playerScoreSums.getOrDefault(playerId, 0d);
                        playerScoreSums.put(playerId, (currentScore + score));
                    });
                });

                // Gesamtpunkt alle Spieler aufsummieren
                double sumAllPlayerPoints = playerScoreSums.values().stream().reduce(0d, Double::sum);
                if (sumAllPlayerPoints == 0) {
                    sender.sendMessage(Component.text("Kein Spieler hat Punkte zu den angegebenen Keys erzielt.").color(NamedTextColor.RED));
                    taskFutures.cancel();
                    return;
                }

                // Spieler nach Gesamtpunkten absteigend sortieren
                Map<UUID, Double> sortedPlayerScoreSums = playerScoreSums.entrySet().stream().sorted(Map.Entry.<UUID, Double> comparingByValue().reversed())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));

                // Gewinnzahl ziehen
                double winningNumber = (new Random()).nextDouble() * sumAllPlayerPoints;
                UUID winner = null;

                // Gewinner bestimmen
                Iterator<Map.Entry<UUID, Double>> scoreSums = sortedPlayerScoreSums.entrySet().iterator();
                double sumPlayerScores = 0;
                while (scoreSums.hasNext() && winner == null) {
                    Map.Entry<UUID, Double> playerScore = scoreSums.next();
                    sumPlayerScores += playerScore.getValue();
                    if (sumPlayerScores >= winningNumber) {
                        winner = playerScore.getKey();
                    }
                }

                // Falls kein Gewinner ermittelt werden konnte
                if (winner == null) {
                    sender.sendMessage(Component.text("Es konnte kein Gewinner ermittelt werden.").color(NamedTextColor.RED));
                    taskFutures.cancel();
                    return;
                }

                // Gewinnerausgabe
                String winnerName = playerUUIDCache.getPlayer(winner).getName();
                if (winnerName == null) {
                    sender.sendMessage(Component.text("Es konnte kein Name zum Gewinner ermittelt werden.").color(NamedTextColor.RED));
                    taskFutures.cancel();
                    return;
                }
                sender.sendMessage(Component.text("Der Gewinner ist ").color(NamedTextColor.DARK_GREEN).append(Component.text(winnerName).color(NamedTextColor.RED))
                        .append(Component.text(".").color(NamedTextColor.DARK_GREEN)));
                if (!this.addtionalParticipants.isEmpty()) {
                    sender.sendMessage(Component.text("Zusätzliche Teilnehmer wurden entfernt.").color((NamedTextColor.DARK_GREEN)));
                }
                this.addtionalParticipants.clear();

                new BukkitRunnable() {

                    @Override
                    public void run() {
                        // Gewinnwahrscheinlichkeiten jedes Spielers berechnen
                        LinkedList<String> components = new LinkedList<>();
                        sortedPlayerScoreSums.forEach((id, score) -> {
                            String name = playerUUIDCache.getPlayer(id).getName();
                            if (name != null) {
                                components.add(name + " " + ((double) Math.round((score * 10000f) / sumAllPlayerPoints)) / 100 + "%");
                            }
                        });

                        StringBuilder sb = new StringBuilder();
                        sb.append("Gewinnwahrscheinlichkeiten:\n" + plugin.getCombinedString(components, "\n"));
                        HastebinUtil.paste(sb.toString(), new HastebinUtil.PasteCompletedListener() {
                            @Override
                            public void onSuccess(String url) {
                                sender.sendMessage("Die Gewinnchancen wurden gespeichert als " + url);
                            }

                            @Override
                            public void onError(Exception exception) {
                                sender.sendMessage("Die Gewinnchancen konnten nicht gespeichert werden: " + exception.getMessage());
                                plugin.getLogger().log(Level.SEVERE, "Could not paste winning chances report", exception);
                            }
                        });
                    }
                }.runTaskAsynchronously(plugin);
            }));
        });

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
            str.add("addParticipants");
            str.add("listParticipants");
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
        return "(addParticipants <spieler> [<spieler2> ...])";
    }
}
