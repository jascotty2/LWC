package com.griefcraft.modules.lecterns;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class LecternListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLecternClosedEvent(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();

        // inv.getViewers() will include the closing player (not "closed" yet)
        if (inv.getViewers().size() <= 1 && inv.getHolder() instanceof Lectern lectern) {
            // good to reset!
            // query: is the flag set?
            final LWC lwc = LWC.getInstance();
            final Block b = lectern.getBlock();
            final Protection protection = lwc.findProtection(b.getLocation());
            if (protection != null && (protection.hasFlag(Flag.Type.LECTERN_AUTO_REWIND) || protection.hasFlag(Flag.Type.LECTERN_VIEW_ONLY))) {
                // yep!
                lectern.setPage(0);
                lectern.update();
            }
        }
    }
}
