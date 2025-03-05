package com.griefcraft.modules.admin;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.LWCPlayer;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCCommandEvent;
import com.griefcraft.util.UUIDRegistry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class AdminOwnerAll extends JavaModule {

    @Override
    public void onCommand(LWCCommandEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!event.hasFlag("a", "admin")) {
            return;
        }

        LWC lwc = event.getLWC();
        CommandSender sender = event.getSender();
        String[] args = event.getArgs();

        if (!args[0].equals("forceownerall")) {
            return;
        }

        if (args.length < 3) {
            lwc.sendSimpleUsage(sender,
                    "/lwc admin forceownerall <OldPlayer> <NewPlayer>");
            return;
        }

        UUID oldOwner = UUIDRegistry.getUUID(args[2]);

        if (!(sender instanceof Player)) {
            lwc.sendLocale(sender, "protection.admin.noconsole");
            return;
        }

        String owner;

        if (oldOwner != null) {
            owner = oldOwner.toString();
        } else {
            owner = UUIDRegistry.getName(oldOwner);
        }

        UUID uuid = UUIDRegistry.getUUID(args[1]);
        List<Protection> protection;
        if (uuid != null) {
            protection = lwc.getPhysicalDatabase().loadProtectionsByPlayer(
                    uuid.toString());
        } else {
            protection = lwc.getPhysicalDatabase().loadProtectionsByPlayer(
                    args[1]);
        }

        LWCPlayer player = lwc.wrapPlayer(sender);
        for (Protection prot : protection) {
            prot.setOwner(owner);
            lwc.getPhysicalDatabase().saveProtection(prot);
            lwc.removeModes(player);
            lwc.log(prot.getOwner() + " Changed");
            lwc.sendLocale(player, "protection.interact.forceowner.finalize",
                    "player", prot.getFormattedOwnerPlayerName());
        }
    }

}
