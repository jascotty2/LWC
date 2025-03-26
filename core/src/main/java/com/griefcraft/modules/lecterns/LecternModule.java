package com.griefcraft.modules.lecterns;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCProtectionInteractEvent;
import com.griefcraft.util.config.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LecternModule extends JavaModule {

    private final Configuration configuration = Configuration.load("lecterns.yml");
    /**
     * If this module is enabled
     */
    private boolean enabled = false;

    @Override
    public void load(LWC lwc) {
        enabled = configuration.getBoolean("lecterns.enabled", false);
        if (enabled) {
            if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                lwc.log("ProtocolLib not found - disabling behavior for flag \"" + Flag.Type.LECTERN_VIEW_ONLY.name().toLowerCase().replace('_', '-') + "\"");
                enabled = false;
            } else {
                // register listener
                ProtoLecternListener.register();
            }
            Bukkit.getPluginManager().registerEvents(new LecternListener(), LWC.getInstance().getPlugin());
        }
    }

    @Override
    public void unload() {
        if (enabled) {
            ProtoLecternListener.unregister();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onProtectionInteract(LWCProtectionInteractEvent event) {
        if (!enabled || event.getResult() != DEFAULT) {
            return;
        }

        Protection protection = event.getProtection();

        if (protection.hasFlag(Flag.Type.LECTERN_VIEW_ONLY)) {

            // if we can admin, passthrough - otherwise, show a fake lectern:
            if (event.canAdmin()) {
                return;
            }

            //LWC lwc = event.getLWC();
            Player player = event.getPlayer();

            event.getEvent().setCancelled(true);
            openFakeLectern(player, protection.getBlock());

            event.setResult(Result.ALLOW);
        }
    }

    private void openFakeLectern(Player player, Block lecternBlock) {
        if (lecternBlock.getType() != Material.LECTERN) {
            return;
        }

        Lectern lectern = (Lectern) lecternBlock.getState();

        ItemStack book = lectern.getInventory().getItem(0);

        if (book == null || (book.getType() != Material.WRITTEN_BOOK && book.getType() != Material.WRITABLE_BOOK)) {
            return;
        }

        ProtoLecternListener.openLectern(player, book);

    }

}
