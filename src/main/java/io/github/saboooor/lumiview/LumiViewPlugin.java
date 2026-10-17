package io.github.saboooor.lumiview;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Shelf;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Shelf items exist only in outgoing block-entity packets, never in world storage. */
public final class LumiViewPlugin extends JavaPlugin implements Listener {
    private final Set<Location> shelves = new HashSet<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getWorlds().forEach(world -> {
            for (Chunk chunk : world.getLoadedChunks()) scan(chunk);
        });
        Bukkit.getScheduler().runTaskTimer(this, this::refresh, 1L, 10L);
    }

    @Override
    public void onDisable() {
        // Restore real (empty) shelf inventories in clients when disabling/reloading.
        for (Location location : shelves) {
            if (location.isWorldLoaded() && location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
                    && location.getBlock().getState() instanceof Shelf shelf) send(shelf);
        }
        shelves.clear();
    }

    private void scan(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Shelf) shelves.add(state.getLocation());
        }
    }

    @EventHandler
    public void onLoad(ChunkLoadEvent event) { scan(event.getChunk()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        shelves.removeIf(loc -> loc.getWorld().equals(chunk.getWorld())
                && loc.getBlockX() >> 4 == chunk.getX() && loc.getBlockZ() >> 4 == chunk.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        shelves.removeIf(loc -> loc.getWorld().equals(event.getWorld()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getState() instanceof Shelf) shelves.add(event.getBlockPlaced().getLocation());
    }

    private Chest chestBehind(Shelf shelf) {
        if (!(shelf.getBlockData() instanceof org.bukkit.block.data.type.Shelf data)) return null;
        Block behind = shelf.getBlock().getRelative(data.getFacing().getOppositeFace());
        // Do not load adjacent chunks just to show a preview.
        if (!behind.getWorld().isChunkLoaded(behind.getX() >> 4, behind.getZ() >> 4)) return null;
        if (!(behind.getState() instanceof Chest chest)) return null;
        if (!(chest.getBlockData() instanceof org.bukkit.block.data.type.Chest chestData)
                || chestData.getFacing() != data.getFacing()) return null;
        return chest;
    }

    private boolean empty(Shelf shelf) { return shelf.getInventory().isEmpty(); }

    private void refresh() {
        Iterator<Location> iterator = shelves.iterator();
        while (iterator.hasNext()) {
            Location location = iterator.next();
            if (!location.isWorldLoaded() || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                iterator.remove();
                continue;
            }
            if (!(location.getBlock().getState() instanceof Shelf shelf)) {
                iterator.remove();
                continue;
            }
            Chest chest = chestBehind(shelf);
            if (chest != null && empty(shelf) && safeToPreview(chest)) {
                List<ItemStack> preview = PreviewItems.select(chest.getInventory().getStorageContents(), previewOffset(chest));
                for (int slot = 0; slot < preview.size(); slot++) {
                    shelf.getSnapshotInventory().setItem(slot, preview.get(slot));
                }
            }
            // Sending the actual snapshot also clears a previous preview after unlinking.
            send(shelf);
        }
    }

    private int previewOffset(Chest chest) {
        if (!(chest.getInventory().getHolder() instanceof DoubleChest pair)
                || !(pair.getLeftSide() instanceof Chest left)
                || !(pair.getRightSide() instanceof Chest right)) return 0;
        boolean isLeft = chest.getLocation().equals(left.getLocation());
        Chest partner = isLeft ? right : left;
        var data = (org.bukkit.block.data.type.Chest) partner.getBlockData();
        Block partnerBlock = partner.getBlock().getRelative(data.getFacing());
        if (!partnerBlock.getWorld().isChunkLoaded(partnerBlock.getX() >> 4, partnerBlock.getZ() >> 4)
                || !(partnerBlock.getState() instanceof Shelf partnerShelf)
                || !empty(partnerShelf)) return 0;
        Chest partnerBehind = chestBehind(partnerShelf);
        if (partnerBehind == null || !partnerBehind.getLocation().equals(partner.getLocation())) return 0;
        // Bukkit's double-chest half order is stable, regardless of which shelf refreshes first.
        return isLeft ? 0 : 3;
    }

    private boolean safeToPreview(Chest chest) {
        if (chest.isLocked() || chest.getLootTable() != null) return false;
        if (chest.getInventory().getHolder() instanceof DoubleChest pair) {
            return pair.getLeftSide() instanceof Chest left && pair.getRightSide() instanceof Chest right
                    && !left.isLocked() && !right.isLocked()
                    && left.getLootTable() == null && right.getLootTable() == null;
        }
        return true;
    }

    private boolean blocked(Chest chest) {
        if (chest.isBlocked()) return true;
        if (chest.getInventory().getHolder() instanceof DoubleChest pair) {
            return pair.getLeftSide() instanceof Chest left && left.isBlocked()
                    || pair.getRightSide() instanceof Chest right && right.isBlocked();
        }
        return false;
    }

    private void send(Shelf shelf) {
        Location location = shelf.getLocation();
        for (Player player : location.getWorld().getPlayers()) {
            if (player.isChunkSent(Chunk.getChunkKey(location.getBlockX() >> 4, location.getBlockZ() >> 4))) {
                player.sendBlockUpdate(location, shelf);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || !(event.getClickedBlock().getState() instanceof Shelf shelf)) return;
        Chest chest = chestBehind(shelf);
        if (chest == null || !empty(shelf)) return;
        boolean denied = event.useInteractedBlock() == Event.Result.DENY;
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (denied || event.getHand() != EquipmentSlot.HAND) return;

        // Give protection plugins the same chest interaction to authorize before opening.
        PlayerInteractEvent chestEvent = new PlayerInteractEvent(event.getPlayer(), Action.RIGHT_CLICK_BLOCK,
                event.getItem(), chest.getBlock(), event.getBlockFace(), EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(chestEvent);
        if (chestEvent.useInteractedBlock() == Event.Result.DENY || blocked(chest)) return;
        // Fail closed for locked/unopened loot chests; direct inventory opening skips vanilla checks.
        if (!safeToPreview(chest)) return;
        Inventory inventory = chest.getInventory();
        event.getPlayer().openInventory(inventory);
    }
}
