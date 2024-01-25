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
import java.util.logging.Level;
import org.bukkit.Location;

public class KnownWorldOreLocations {
    private final OreFarmStatsPlugin plugin;
    private final File file;
    private final DataOutputStream outStream;
    private final LongOpenHashSet knownLocations;

    public KnownWorldOreLocations(OreFarmStatsPlugin plugin, String world) {
        plugin.getLogger().info("Loading locations for world " + world);
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        knownLocations = new LongOpenHashSet();
        file = new File(plugin.getDataFolder(), world + ".dat");
        if (file.isFile()) {
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                while (true) {
                    try {
                        long l = dis.readLong();
                        knownLocations.add(l);
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

    public boolean add(Location loc) {
        long compressed = CompressedLocation.fromLocation(loc);
        if (knownLocations.add(compressed)) {
            try {
                outStream.writeLong(compressed);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not add location to file", e);
            }
            return true;
        }
        return false;
    }

    public boolean contains(Location loc) {
        long compressed = CompressedLocation.fromLocation(loc);
        return knownLocations.contains(compressed);
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
