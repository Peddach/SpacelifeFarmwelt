package de.petropia.farmworld;

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
