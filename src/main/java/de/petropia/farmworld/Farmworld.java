package de.petropia.farmworld;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.petropia.farmworld.listener.FarmworldCLMessageListener;
import de.petropia.farmworld.listener.PreventPVPListener;
import de.petropia.turtleServer.api.PetropiaPlugin;

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

    public static void setAvailable(boolean isAvailable) {
        Farmworld.available = isAvailable;
    }
}
