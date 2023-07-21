package de.petropia.farmworld;

import de.petropia.turtleServer.api.worlds.WorldManager;
import de.petropia.turtleServer.server.TurtleServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

public class FarmworldManager {

    private static final String WORLD_DATA_FILE = "FarmFile";

    public void checkFarmworlds(){
        Farmworld.getInstance().getLogger().info("Checking world: world");
        checkWorld("world", true);
        Farmworld.getInstance().getLogger().info("Checking world: world_nether");
        checkWorld("world_nether", false);
        setWorldBorders();
    }

    private void setWorldBorders(){
        int distance = Farmworld.getInstance().getConfig().getInt("BlockToGenerate");
        World world = Bukkit.getWorld("world");
        world.getWorldBorder().setCenter(new Location(world, 0, 0, 0));
        world.getWorldBorder().setWarningDistance(30);
        world.getWorldBorder().setSize(distance - 50);
        World nether = Bukkit.getWorld("world_nether");
        nether.getWorldBorder().setCenter(new Location(nether, 0, 0, 0));
        nether.getWorldBorder().setWarningDistance(30);
        nether.getWorldBorder().setSize(distance - 50);
    }

    private void checkWorld(String name, boolean mainWorld){
        File worldcontainer = Bukkit.getServer().getWorldContainer();
        File worlddata;
        try {
            if(new File(worldcontainer, name).exists()){
                Farmworld.getInstance().getLogger().info("Found world directory for " + name);
                File worldDir = new File(worldcontainer, name);
                worlddata = new File(worldDir, WORLD_DATA_FILE);
                if(!worlddata.exists()){
                    Farmworld.getInstance().getLogger().info("Worlddata file does not exist for existing world '" + name+ "'! Creating new!");
                    generateNewWorldDataFile(worlddata);
                    return;
                }
                Farmworld.getInstance().getLogger().info("Worlddata file found! Reading...");
                BufferedReader reader = new BufferedReader(new FileReader(worlddata));
                long timeStamp = Long.parseLong(reader.readLine());
                reader.close();
                if(timeStamp <= Instant.now().getEpochSecond()){ //World is too old
                    Farmworld.getInstance().getLogger().info("World to old! Deleting and schedule new delete");
                    delete(worldDir);
                    Bukkit.getScheduler().runTask(Farmworld.getInstance(), () -> {
                        if(mainWorld) {
                            Farmworld.setAvailable(false);
                        }
                        generateNewWorldDataFile(worlddata);
                        Farmworld.getInstance().getLogger().info("Starting world pregen for " + name);
                        WorldManager.generate(Bukkit.getWorld(name), Farmworld.getInstance().getConfig().getInt("BlockToGenerate"), false).thenAccept(bool -> {
                            if(bool) Farmworld.getInstance().getLogger().warning("Pregen for " + name + " failed!");
                            Farmworld.getInstance().getLogger().info("Pregen done for" + name);
                            if(mainWorld) {
                                Farmworld.setAvailable(true);
                            }
                        });
                    });
                } else {
                    if(!mainWorld){
                        return;
                    }
                    Farmworld.getInstance().getLogger().info("Schedule server restart in " + timeStamp + "s");
                    Bukkit.getScheduler().runTaskLater(Farmworld.getInstance(), () -> TurtleServer.getInstance().shutdownServer(), (timeStamp - Instant.now().getEpochSecond()) * 20);
                    Farmworld.setNextDelete(Instant.ofEpochSecond(timeStamp));
                }
                return;
            }
            Farmworld.getInstance().getLogger().info("World " + name + " is not contained in world container. Assuming creation later and schedule creation of new worlddata file");
            File worldDir = new File(worldcontainer, name);
            worlddata = new File(worldDir,WORLD_DATA_FILE);
            generateNewWorldDataFile(worlddata);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
    }

    /**
     * Create a new world data file in world directory as soon as the world is generated and sets a new date to delete
     * @param worlddata Worldata as file
     */
    private void generateNewWorldDataFile(File worlddata){
        Farmworld.getInstance().getLogger().info("Creating new worddata file");
        if(!worlddata.exists()){
            try {
                worlddata.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Instant nextDelete = getNextDelete();
        Farmworld.setNextDelete(nextDelete);
        Bukkit.getScheduler().runTask(Farmworld.getInstance(), () -> {
            try {
                Files.writeString(worlddata.toPath(), String.valueOf(nextDelete.getEpochSecond()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Farmworld.getInstance().getLogger().info("Success! Next delete on " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date.from(nextDelete)));
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

}
