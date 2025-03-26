package com.griefcraft.modules.lecterns;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.griefcraft.lwc.LWC;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class ProtoLecternListener {

    static HashMap<Player, Inventory> openPlayers = new HashMap();

    public static void register() {

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                LWC.getInstance().getPlugin(),
                ListenerPriority.NORMAL,
                PacketType.Play.Client.CLOSE_WINDOW
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                openPlayers.remove(event.getPlayer());
            }
        });

        // lecterns are supposed to update their block state, then notify the player if it updated
        // (eg, other players can change the current page)
        // for our fake lectern, this means we need to send a packet to let the player turn the page
        // (this is unfortunately not something that spigot or paper supports right now)
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                LWC.getInstance().getPlugin(),
                ListenerPriority.NORMAL,
                PacketType.Play.Client.ENCHANT_ITEM // fake lectern page events use enchant packets for some reason
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                final Player player = event.getPlayer();
                final Inventory inv = openPlayers.get(player);
                if (inv != null) {
                    LecternInventoryHolder invHolder = (LecternInventoryHolder) inv.getHolder();
                    PacketContainer pc = event.getPacket();
                    int invID = pc.getIntegers().getValues().getFirst();
                    int pageType = pc.getIntegers().getValues().getLast();
                    switch (pageType) {
                        case 1 -> {
                            // prev page
                            if (invHolder.page > 0) {
                                // send a packet to tell the client to go back a page
                                ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle().connection.sendPacket(
                                        new net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket(
                                                invID, 0, --invHolder.page
                                        )
                                );
                                player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1, 1f);
                            }
                        }
                        case 2 -> {
                            // next page
                            if (invHolder.page < invHolder.pages) {
                                // send a packet to tell the client to go forward a page
                                ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle().connection.sendPacket(
                                        new net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket(
                                                invID, 0, ++invHolder.page
                                        )
                                );
                                player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1, 1f);
                            }
                        }
                        case 3 -> // 3 == take book (ignore)
                            player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 1, 1.1f);
                        default -> {
                            // todo: BUTTON_PAGE_JUMP_RANGE_START = 100;
                            // jumps to page (id - 100) (not sure if the client actually implements this)
                        }
                    }
                }
            }
        });
    }

    public static void unregister() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(LWC.getInstance().getPlugin());
        for (Player p : openPlayers.keySet()) {
            p.closeInventory();
        }
        openPlayers.clear();
    }

    public static void openLectern(Player player, ItemStack book) {
        Inventory inv = Bukkit.createInventory(new LecternInventoryHolder(player, ((BookMeta) book.getItemMeta()).getPageCount()), InventoryType.LECTERN);
        inv.addItem(book.clone());

        Bukkit.getScheduler().runTaskLater(LWC.getInstance().getPlugin(), () -> {
            player.openInventory(inv);
            openPlayers.put(player, inv);
        }, 1);
    }

    static class LecternInventoryHolder implements InventoryHolder {

        //final Inventory inventory;
        final Player player;
        final int pages;
        int page = 0;

        public LecternInventoryHolder(Player player, int pages) {
            this.player = player;
            this.pages = pages;
        }

        public Player getPlayer() {
            return player;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }

    }

}
