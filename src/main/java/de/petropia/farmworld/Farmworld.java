package de.petropia.farmworld;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.petropia.farmworld.listener.FarmworldCLMessageListener;
import de.petropia.farmworld.listener.PreventPVPListener;
import de.petropia.farmworld.listener.SafePlayerRandomSpawn;
import de.petropia.spacelifeCore.player.SpacelifeDatabase;
import de.petropia.spacelifeCore.player.SpacelifePlayer;
import de.petropia.spacelifeCore.scoreboard.ScoreboardElementRegistry;
import de.petropia.spacelifeCore.teleport.StaticTeleportPoints;
import de.petropia.turtleServer.api.PetropiaPlugin;
import de.petropia.turtleServer.server.TurtleServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.time.Instant;

public class Farmworld extends PetropiaPlugin {

    private static Farmworld instance;

    private static Instant nextDelete;
    private static boolean available = true;

    @Override
    public void onEnable(){ //load: Preworld
        instance = this;
        saveDefaultConfig();
        saveConfig();
        reloadConfig();
        new FarmworldManager().checkWorld();
        CloudNetDriver.getInstance().getEventManager().registerListener(new FarmworldCLMessageListener());
        this.getServer().getPluginManager().registerEvents(new PreventPVPListener(), this);
        SafePlayerRandomSpawn playerRandomSpawnListener = new SafePlayerRandomSpawn();
        this.getServer().getPluginManager().registerEvents(playerRandomSpawnListener, this);
        if(available){
            getServer().getScheduler().runTask(this, playerRandomSpawnListener::onLoad);
        }
        ScoreboardElementRegistry.registerElement(new WorldDeleteScoreboardElement());
        if(nextDelete != null){
            scheduleShutdown();
        }
    }

    public void scheduleShutdown(){
        Bukkit.getScheduler().runTaskLater(this, () -> TurtleServer.getInstance().shutdownServer(), (nextDelete.getEpochSecond() - Instant.now().getEpochSecond()) * 20);
        Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getOnlinePlayers().forEach(player -> {
            available = false;
            getMessageUtil().sendMessage(player, Component.text("Die Farmwelt wird jetzt resettet!", NamedTextColor.RED));
            SpacelifePlayer spacelifePlayer = SpacelifeDatabase.getInstance().getCachedPlayer(player.getUniqueId());
            spacelifePlayer.teleportCrossServer(StaticTeleportPoints.SPAWN);
        }), ((nextDelete.getEpochSecond() - Instant.now().getEpochSecond()) - 2*20) * 20);
    }

    public static Farmworld getInstance() {
        return instance;
    }

    public static Instant getNextDelete() {
        return nextDelete;
    }

    public static void setNextDelete(Instant nextDelete) {
        Farmworld.nextDelete = nextDelete;
    }

    public static boolean isAvailable() {
        return available;
    }

    public static String convertUnixTimestamp(long timestamp) {
        long currentTimestamp = Instant.now().getEpochSecond();
        long secondsRemaining = timestamp - currentTimestamp;
        long months = secondsRemaining / 2592000;
        if (months > 0) {
            return months + (months == 1 ? " Monat" : " Monate");
        }
        long weeks = secondsRemaining / 604800;
        if (weeks > 0) {
            return weeks + (weeks == 1 ? " Woche" : " Wochen");
        }
        long days = secondsRemaining / 86400;
        if (days > 0) {
            return days + (days == 1 ? " Tag" : " Tage");
        }
        long hours = secondsRemaining / 3600;
        if (hours > 0) {
            return hours + (hours == 1 ? " Stunde" : " Stunden");
        }
        long minutes = secondsRemaining / 60;
        if (minutes > 0) {
            return minutes + (minutes == 1 ? " Minute" : " Minuten");
        }
        return secondsRemaining + (secondsRemaining == 1 ? " Sekunde" : " Sekunden");
    }
    public static void setAvailable(boolean isAvailable) {
        Farmworld.available = isAvailable;
    }
}
