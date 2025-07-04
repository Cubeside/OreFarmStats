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
import java.util.logging.Level;
import org.bukkit.Location;

public class KnownWorldMultiLocations {
    private final OreFarmStatsPlugin plugin;
    private final File file;
    private final DataOutputStream outStream;
    private final Long2IntOpenHashMap knownLocations;
    private final int maxCount;

    public KnownWorldMultiLocations(OreFarmStatsPlugin plugin, String world, int maxCount) {
        this(plugin, world, maxCount, null);
    }

    public KnownWorldMultiLocations(OreFarmStatsPlugin plugin, String world, int maxCount, String filePrefix) {
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
        knownLocations = new Long2IntOpenHashMap();
        file = new File(plugin.getDataFolder(), filePrefix + world + ".dat");
        if (file.isFile()) {
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                while (true) {
                    try {
                        long l = dis.readLong();
                        int count = dis.readInt();
                        if (count > 0) {
                            knownLocations.put(l, count);
                        } else {
                            knownLocations.remove(l);
                        }
                    } catch (EOFException e) {
                        break;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not open " + file.getName() + " for reading.", e);
            }
        }

        try

        {
            outStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, true)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not open " + file.getName() + " for writing.", e);
        }
    }

    public boolean add(Location loc) {
        long compressed = CompressedLocation.fromLocation(loc);
        int newAmount = knownLocations.get(compressed) + 1;
        if (newAmount <= maxCount) {
            knownLocations.put(compressed, newAmount);
            try {
                outStream.writeLong(compressed);
                outStream.writeInt(newAmount);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not add location to file", e);
            }
            return true;
        }
        return false;
    }

    public boolean remove(Location loc) {
        long compressed = CompressedLocation.fromLocation(loc);
        if (knownLocations.remove(compressed) > 0) {
            try {
                outStream.writeLong(compressed);
                outStream.writeInt(0);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not add location to file", e);
            }
            return true;
        }
        return false;
    }

    public boolean contains(Location loc) {
        long compressed = CompressedLocation.fromLocation(loc);
        return knownLocations.containsKey(compressed);
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
