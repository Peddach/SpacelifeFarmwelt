package de.petropia.farmworld;

import de.petropia.turtleServer.api.worlds.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;

public class FarmworldManager {

    private static final String WORLD_DATA_FILE = "FarmFile";
    private static final String WORLD_NAME = "world";
    private static final String NETHER_NAME = "world_nether";
    private static final int BLOCKS_TO_GENERATE = Farmworld.getInstance().getConfig().getInt("BlockToGenerate");

    /**
     * Checks for all worlds if they are too old and deletes them if neccessary. Also, it pregenerates all worlds and set world boarders.
     */
    public void checkWorld(){
        File worldFolder = Bukkit.getWorldContainer();
        File farmFile = new File(worldFolder, WORLD_NAME + File.separator + WORLD_DATA_FILE);
        if (farmFile.exists()) {
            try {
                String line = Files.readAllLines(farmFile.toPath()).get(0);
                long parsedLongTimestamp = Long.parseLong(line);
                Instant deleteTimestamp = Instant.ofEpochSecond(parsedLongTimestamp);
                if (Instant.now().isAfter(deleteTimestamp)) {
                    deleteRecursive(new File(worldFolder, WORLD_NAME));
                    deleteRecursive(new File(worldFolder, NETHER_NAME));
                    recreateVanillaDirectories(NETHER_NAME);
                    recreateVanillaDirectories(WORLD_NAME);
                    createNewFarmFile(farmFile);
                    pregenWorld();
                    Bukkit.getLogger().info("Farmworld is expired and was deleted");
                    Farmworld.setAvailable(false);
                } else {
                    Farmworld.getInstance().getLogger().info("Farmworld is not expired.");
                    Farmworld.setAvailable(true);
                    Farmworld.setNextDelete(deleteTimestamp);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            createNewFarmFile(farmFile);
            pregenWorld();
        }
        setWorldBorders();
        applyGamerules(WORLD_NAME);
        applyGamerules(NETHER_NAME);
    }

    private void applyGamerules(String worldname) {
        Bukkit.getScheduler().runTask(Farmworld.getInstance(), () -> {
            World world = Bukkit.getWorld(worldname);
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(GameRule.DO_FIRE_TICK, false);
            world.setGameRule(GameRule.MAX_ENTITY_CRAMMING, 12);
            world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        });
    }

    private void setWorldBorders(){
        Bukkit.getScheduler().runTaskLater(Farmworld.getInstance(), () -> {
            World world = Bukkit.getWorld(WORLD_NAME);
            WorldBorder worldBorder = world.getWorldBorder();
            worldBorder.setSize(BLOCKS_TO_GENERATE * 2);
            World nether = Bukkit.getWorld(NETHER_NAME);
            WorldBorder netherBorder = nether.getWorldBorder();
            netherBorder.setSize(BLOCKS_TO_GENERATE * 2);
        }, 2);
    }

    private void pregenWorld() {
        Farmworld.getInstance().getLogger().info("Starting pregeneration!");
        Farmworld.setAvailable(false);
        Bukkit.getScheduler().runTaskLater(Farmworld.getInstance(), () -> WorldManager.generate(Bukkit.getWorld(WORLD_NAME), BLOCKS_TO_GENERATE, false).thenAccept(success -> {
            if(!success){
                Farmworld.getInstance().getLogger().info("Failed to generate world");
            } else {
                Farmworld.getInstance().getLogger().info("Successfully generated world");
            }
            Farmworld.setAvailable(true);
        }), 20);
        Bukkit.getScheduler().runTaskLater(Farmworld.getInstance(), () -> WorldManager.generate(Bukkit.getWorld(WORLD_NAME), BLOCKS_TO_GENERATE, false).thenAccept(success -> {
            if(!success){
                Farmworld.getInstance().getLogger().info("Failed to generate nether");
            } else {
                Farmworld.getInstance().getLogger().info("Successfully generated nether");
            }
        }), 20*20);

    }

    private void createNewFarmFile(File file){
        Bukkit.getScheduler().runTask(Farmworld.getInstance(), () -> {
            try {
                Instant nextDeleteTimestamp = getNextDelete();
                Files.write(file.toPath(), Collections.singletonList(String.valueOf(nextDeleteTimestamp.getEpochSecond())));
                Farmworld.setNextDelete(nextDeleteTimestamp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void recreateVanillaDirectories(String worldName){
        Bukkit.getScheduler().runTask(Farmworld.getInstance(), () -> {
           File worldContainer = Bukkit.getWorldContainer();
           File playerData = new File(worldContainer, worldName + File.separator + "playerdata");
           if(!playerData.mkdirs()){
               Farmworld.getInstance().getLogger().info("Failed to recreate playerdata directory");
           }
        });
    }

    private Instant getNextDelete(){
        int dayOfWeek = Farmworld.getInstance().getConfig().getInt("dayToReset");
        Instant startingInstant = Instant.now();
        // Convert the dayOfWeek value to DayOfWeek enum
        DayOfWeek targetDay = DayOfWeek.of(dayOfWeek);
        // Get the LocalDateTime equivalent of the startingInstant in the Germany time zone
        LocalDateTime startingDateTime = startingInstant.atZone(ZoneId.of("Europe/Berlin")).toLocalDateTime();
        // Find the next occurrence of the target day in the week
        LocalDateTime nextDayDateTime = startingDateTime.with(TemporalAdjusters.next(targetDay));
        // Set the time to 15:00 (3:00 PM)
        nextDayDateTime = nextDayDateTime.withHour(15).withMinute(0).withSecond(0).withNano(0);
        // Convert LocalDateTime back to Instant in the Germany time zone
        return nextDayDateTime.atZone(ZoneId.of("Europe/Berlin")).toInstant();
    }

    /**
     * Delete all content in a directory and the directory itself
     *
     * @param directory The directory to delete
     */
    private void deleteRecursive(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {  //Can be null when dir is empty or IO error
            directory.delete();
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteRecursive(file);  //delete subdir recursive
            } else {
                file.delete();
            }
        }
        directory.delete(); //delete root dir -> now it is empty
    }

}
