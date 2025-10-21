package de.cubeside.orefarmstats;

import de.cubeside.orefarmstats.util.CompressedLocation;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class KnownWorldMultiChunks {
    private final OreFarmStatsPlugin plugin;
    private final File file;
    private final DataOutputStream outStream;
    private final HashMap<UUID, Long2IntOpenHashMap> knownLocations;
    private final int maxCount;

    public KnownWorldMultiChunks(OreFarmStatsPlugin plugin, String world, int maxCount) {
        this(plugin, world, maxCount, null);
    }

    public KnownWorldMultiChunks(OreFarmStatsPlugin plugin, String world, int maxCount, String filePrefix) {
        if (filePrefix == null) {
            filePrefix = "";
        }
        plugin.getLogger().info("Loading " + filePrefix + " locations for world " + world);
        if (!filePrefix.isEmpty()) {
            filePrefix = filePrefix + ".";
        }
        this.plugin = plugin;
        this.maxCount = maxCount;
        plugin.getDataFolder().mkdirs();
        knownLocations = new HashMap<>();
        file = new File(plugin.getDataFolder(), filePrefix + world + ".dat");
        if (file.isFile()) {
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                while (true) {
                    try {
                        long msb = dis.readLong();
                        long lsb = dis.readLong();
                        UUID uuid = new UUID(msb, lsb);
                        long l = dis.readLong();
                        int count = dis.readInt();
                        Long2IntOpenHashMap set = getPlayerData(uuid);
                        if (count > 0) {
                            set.put(l, count);
                        } else {
                            set.remove(l, count);
                        }
                    } catch (EOFException e) {
                        break;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not open " + file.getName() + " for reading.", e);
            }
        }

        try {
            outStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, true)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not open " + file.getName() + " for writing.", e);
        }
    }

    public Long2IntOpenHashMap getPlayerData(UUID player) {
        return knownLocations.computeIfAbsent(player, uuid2 -> new Long2IntOpenHashMap());
    }

    public boolean add(UUID player, int x, int z) {
        long compressed = CompressedLocation.fromLocation(x, 0, z);
        Long2IntOpenHashMap set = getPlayerData(player);
        int newAmount = set.get(compressed) + 1;
        if (newAmount <= maxCount) {
            set.put(compressed, newAmount);
            try {
                outStream.writeLong(player.getMostSignificantBits());
                outStream.writeLong(player.getLeastSignificantBits());
                outStream.writeLong(compressed);
                outStream.writeInt(newAmount);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not add location to file", e);
            }
            return true;
        }
        return false;
    }

    public boolean remove(UUID player, int x, int z) {
        long compressed = CompressedLocation.fromLocation(x, 0, z);
        Long2IntOpenHashMap set = getPlayerData(player);
        if (set.remove(compressed) > 0) {
            try {
                outStream.writeLong(player.getMostSignificantBits());
                outStream.writeLong(player.getLeastSignificantBits());
                outStream.writeLong(compressed);
                outStream.writeInt(0);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove location to file", e);
            }
            return true;
        }
        return false;
    }

    public boolean contains(UUID player, int x, int z) {
        long compressed = CompressedLocation.fromLocation(x, 0, z);
        Long2IntOpenHashMap set = getPlayerData(player);
        return set.containsKey(compressed);
    }

    public void close() {
        try {
            outStream.flush();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not flush location file", e);
        }
        try {
            outStream.close();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not close location file", e);
        }
    }
}
