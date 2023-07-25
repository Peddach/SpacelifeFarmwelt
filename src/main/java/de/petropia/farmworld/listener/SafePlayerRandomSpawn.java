package de.petropia.farmworld.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.petropia.farmworld.Farmworld;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkPopulateEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class SafePlayerRandomSpawn implements Listener {

    private static final HashMap<UUID, Location> PLAYER_SPAWNPOINT_CACHE = new HashMap<>();
    private static final List<Location> SAFE_LOCATIONS_CACHE = new ArrayList<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        teleportPlayerToSpawn(event.getPlayer());
        Farmworld.getInstance().getMessageUtil().sendMessage(event.getPlayer(),
                Component.text("Nächster Reset der Farmwelt: ", NamedTextColor.GRAY)
                        .append(Component.text(Farmworld.convertUnixTimestamp(Farmworld.getNextDelete().getEpochSecond()), NamedTextColor.RED).decorate(TextDecoration.ITALIC)));
        event.getPlayer().playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER ,1.2F, 0.8F));
    }

    private void teleportPlayerToSpawn(Player player){
        if(SAFE_LOCATIONS_CACHE.size() == 0){
            Farmworld.getInstance().getLogger().info("No Spawns available for " + player.getName());
            return;
        }
        Location spawn = PLAYER_SPAWNPOINT_CACHE.computeIfAbsent(player.getUniqueId(), uuid -> SAFE_LOCATIONS_CACHE.get(ThreadLocalRandom.current().nextInt(0, SAFE_LOCATIONS_CACHE.size())));
        if(!isSafeLocation(spawn)){
            SAFE_LOCATIONS_CACHE.remove(spawn);
            PLAYER_SPAWNPOINT_CACHE.remove(player.getUniqueId());
            Farmworld.getInstance().getLogger().info("Spawn not safe anymore. Remaining: " + SAFE_LOCATIONS_CACHE.size());
            teleportPlayerToSpawn(player);
            return;
        }
        Farmworld.getInstance().getLogger().info("Found safe spawn for " + player.getName() + ". Remaining: " + SAFE_LOCATIONS_CACHE.size());
        player.teleport(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventListener
    public void onChunkGenerate(ChunkPopulateEvent event){
        if(!event.getWorld().getName().equals("world")){
            return;
        }
        Chunk chunk = event.getChunk();
        searchLocationInChunk(chunk);
    }

    public void onLoad(){
        Farmworld.getInstance().getLogger().info("Start caching Spawn locations");
        int radius = Farmworld.getInstance().getConfig().getInt("BlockToGenerate");
        final World world = Bukkit.getWorld("world");
        final int CHUNK_BUFFFER_SIZE = 6;
        new Thread(() -> {
            List<Location> chunkLocations = new ArrayList<>();
            for(int chunkX = -radius; chunkX < radius; chunkX += 16){
                for(int chunkZ = -radius; chunkZ < radius; chunkZ += 16){
                    chunkLocations.add(new Location(world, chunkX, 100, chunkZ));
                }
            }
            List<CompletableFuture<Void>> chunkBuffer = new ArrayList<>();
            for (Location location : chunkLocations) {
                chunkBuffer.add(world.getChunkAtAsync(location.getBlock()).thenAccept(chunk -> Bukkit.getScheduler().runTask(Farmworld.getInstance(), () -> searchLocationInChunk(chunk))));
                if (chunkBuffer.size() >= CHUNK_BUFFFER_SIZE) {
                    CompletableFuture.allOf(chunkBuffer.toArray(new CompletableFuture[0])).join();
                    chunkBuffer.clear();
                }
            }
            Farmworld.getInstance().getLogger().info("Done Caching Spawn locations");
        }).start();

    }

    private void searchLocationInChunk(Chunk chunk){
        for(int chunkBlockX = 0; chunkBlockX < 16; chunkBlockX++){
            for(int chunkBlockZ = 0; chunkBlockZ < 16; chunkBlockZ++){
                int chunkX = chunk.getX();
                int chunkZ = chunk.getZ();
                int y = chunk.getChunkSnapshot().getHighestBlockYAt(chunkBlockX, chunkBlockZ) + 1;
                int[] globalCoords = chunkToGlobal(chunkX, chunkZ, chunkBlockX, chunkBlockZ);
                int x = globalCoords[0];
                int z = globalCoords[1];
                Location location = new Location(chunk.getWorld(), x + 0.5, y, z + 0.5);
                if(isSafeLocation(location)){
                    SAFE_LOCATIONS_CACHE.add(location);
                    return;
                }
            }
        }
    }

    public int[] chunkToGlobal(int chunkX, int chunkZ, int blockX, int blockZ) {
        int[] globalCoords = new int[2];
        int globalX = chunkX * 16;
        int globalZ = chunkZ * 16;
        globalX += blockX;
        globalZ += blockZ;
        globalCoords[0] = globalX;
        globalCoords[1] = globalZ;
        return globalCoords;
    }

    private boolean isSafeLocation(Location location){
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if(!world.getWorldBorder().isInside(location)){
            return false;
        }
        Material mainBlock = new Location(world, x, y, z).getBlock().getType();
        if(!isNotSolidOrLiquid(mainBlock)){
            return false;
        }
        Material topBlock = new Location(world, x, y + 1, z).getBlock().getType();
        if(!isNotSolidOrLiquid(topBlock)){
            return false;
        }
        Material floorBlock = new Location(world, x, y - 1, z).getBlock().getType();
        if(!floorBlock.isSolid()){
            return false;
        }
        return true;
    }

    private boolean isNotSolidOrLiquid(Material material){
        return !material.isSolid() && material != Material.WATER && material != Material.LAVA;
    }
}
