package de.cubeside.orefarmstats;

import de.cubeside.orefarmstats.util.CompressedLocation;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
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

public class KnownWorldPlayerChunks {
    private final OreFarmStatsPlugin plugin;
    private final File file;
    private final DataOutputStream outStream;
    private final HashMap<UUID, LongOpenHashSet> knownLocations;

    public KnownWorldPlayerChunks(OreFarmStatsPlugin plugin, String world) {
        this(plugin, world, null);
    }

    public KnownWorldPlayerChunks(OreFarmStatsPlugin plugin, String world, String filePrefix) {
        if (filePrefix == null) {
            filePrefix = "";
        }
        plugin.getLogger().info("Loading " + filePrefix + " locations for world " + world);
        if (!filePrefix.isEmpty()) {
            filePrefix = filePrefix + ".";
        }
        this.plugin = plugin;
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
                        LongOpenHashSet set = getPlayerData(uuid);
                        if (!set.add(l)) {
                            set.remove(l);
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

    public LongOpenHashSet getPlayerData(UUID player) {
        return knownLocations.computeIfAbsent(player, uuid2 -> new LongOpenHashSet());
    }

    public boolean add(UUID player, int x, int z) {
        long compressed = CompressedLocation.fromLocation(x, 0, z);
        LongOpenHashSet set = getPlayerData(player);
        if (set.add(compressed)) {
            try {
                outStream.writeLong(player.getMostSignificantBits());
                outStream.writeLong(player.getLeastSignificantBits());
                outStream.writeLong(compressed);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not add location to file", e);
            }
            return true;
        }
        return false;
    }

    public boolean remove(UUID player, int x, int z) {
        long compressed = CompressedLocation.fromLocation(x, 0, z);
        LongOpenHashSet set = getPlayerData(player);
        if (set.remove(compressed)) {
            try {
                outStream.writeLong(player.getMostSignificantBits());
                outStream.writeLong(player.getLeastSignificantBits());
                outStream.writeLong(compressed);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not add location to file", e);
            }
            return true;
        }
        return false;
    }

    public boolean contains(UUID player, int x, int z) {
        long compressed = CompressedLocation.fromLocation(x, 0, z);
        LongOpenHashSet set = getPlayerData(player);
        return set.contains(compressed);
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
