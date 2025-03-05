/*
 * Copyright 2011 Tyler Blair. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */

package com.griefcraft.listeners;

import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.integration.IPermissions;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Flag;
import com.griefcraft.model.LWCPlayer;
import com.griefcraft.model.Permission;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.Module;
import com.griefcraft.scripting.event.LWCBlockInteractEvent;
import com.griefcraft.scripting.event.LWCDropItemEvent;
import com.griefcraft.scripting.event.LWCEntityInteractEvent;
import com.griefcraft.scripting.event.LWCProtectionDestroyEvent;
import com.griefcraft.scripting.event.LWCProtectionInteractEvent;
import com.griefcraft.util.UUIDRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Hopper;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public class LWCPlayerListener implements Listener {

    /**
     * The plugin instance
     */
    private final LWCPlugin plugin;
    private static final Material CHISELED_BOOKSHELF = Material.getMaterial("CHISELED_BOOKSHELF");

    public LWCPlayerListener(LWCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUIDRegistry.updateCache(player.getUniqueId(), player.getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMoveItem(InventoryMoveItemEvent event) {
        if (plugin.getLWC().useAlternativeHopperProtection()
                && !(event.getSource().getHolder() instanceof HopperMinecart || event.getDestination().getHolder() instanceof HopperMinecart))
            return;

        if (plugin.getLWC().useFastHopperProtection() && event.getSource().getHolder() instanceof Hopper)
            return;

        boolean result;

        // if the initiator is the same as the source it is a dropper i.e. depositing items
        if (event.getInitiator() == event.getSource()) {
            result = handleMoveItemEvent(event.getInitiator(), event.getDestination());
        } else {
            result = handleMoveItemEvent(event.getInitiator(), event.getSource());
        }

        if (result) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle the item move event
     *
     * @param inventory
     */
    private boolean handleMoveItemEvent(Inventory initiator, Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        Location location;
        InventoryHolder holder;
        Location hopperLocation = null;
        InventoryHolder hopperHolder;

        try {
            holder = inventory.getHolder();
            hopperHolder = initiator.getHolder();
        } catch (AbstractMethodError e) {
            return false;
        }
		
		Block protectionSource;

        try {
            if (holder instanceof BlockState h) {
                location = h.getLocation();
                protectionSource = location.getBlock();
            } else if (holder instanceof DoubleChest h) {
                location = h.getLocation();
                protectionSource = location.getBlock();
            } else if (holder instanceof Minecart h) {
                int A = EntityBlock.calcHash(h.getUniqueId().hashCode());
                location = new Location(h.getWorld(), A, A, A);
                protectionSource = new EntityBlock(h);
            } else {
                return false;
            }

            if (hopperHolder instanceof Hopper h) {
                hopperLocation = h.getLocation();
            } else if (hopperHolder instanceof HopperMinecart h) {
                hopperLocation = h.getLocation();
            }
        } catch (Exception e) {
            return false;
        }

        LWC lwc = LWC.getInstance();

        // High-intensity zone: increase protection cache if it's full, otherwise
        // the database will be getting rammed
        lwc.getProtectionCache().increaseIfNecessary();

        // Attempt to load the protection at that location
        Protection protection = lwc.findProtection(location);

        // If no protection was found we can safely ignore it
        if (protection == null) {
            return false;
        }

        if (hopperLocation != null && Boolean.parseBoolean(lwc.resolveProtectionConfiguration(Material.HOPPER, "enabled"))) {
            Protection hopperProtection = lwc.findProtection(hopperLocation);

            if (hopperProtection != null) {
                // if they're owned by the same person then we can allow the move
                if (protection.getOwner().equals(hopperProtection.getOwner())) {
                    return false;
                }
            }
        }

        boolean denyHoppers = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(protectionSource, "denyHoppers"));

        // xor = (a && !b) || (!a && b)
        return denyHoppers ^ protection.hasFlag(Flag.Type.HOPPER);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled() || !LWC.ENABLED) {
            return;
        }

        Player player = event.getPlayer();

        LWCDropItemEvent evt = new LWCDropItemEvent(player, event);
        plugin.getLWC().getModuleLoader().dispatchEvent(evt);

        if (evt.isCancelled()) {
            event.setCancelled(true);
        }
    }

/*
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        if (event.isCancelled() || !LWC.ENABLED) {
            return;
        }

        LWC lwc = plugin.getLWC();
        if (!lwc.getConfiguration().getBoolean("core.filterunlock", true)) {
            return;
        }

        // We want to block messages starting with cunlock incase someone screws up /cunlock password.
        String message = event.getMessage();

        if (message.startsWith("cunlock") || message.startsWith("lcunlock") || message.startsWith(".cunlock")) {
            event.setCancelled(true);
            lwc.sendLocale(event.getPlayer(), "lwc.blockedmessage");
        }
    }
*/

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Player player = event.getPlayer();
        LWCPlayer lwcPlayer = lwc.wrapPlayer(player);

        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // This event can sometimes be thrown twice (for the main hand and offhand), and while we need to check both
        // for protection access, we want to avoid doing duplicate work (throwing events, printing messages, etc).
        boolean usingMainHand = event.getHand() == EquipmentSlot.HAND;

        Block block = event.getClickedBlock();
        BlockState state;

        try {
            state = block.getState();
        } catch (NullPointerException e) {
            //
            lwc.log("Invalid Tile Entity detected at " + block.getLocation());
            lwc.log("This is either an issue with your world or a bug in Bukkit");
            return;
        }

        // Prevent players with lwc.deny from interacting with blocks that have an inventory
        if (state instanceof InventoryHolder && lwc.isProtectable(block)) {
            if (!lwc.hasPermission(player, "lwc.protect") && lwc.hasPermission(player, "lwc.deny") && !lwc.isAdmin(player) && !lwc.isMod(player)) {
                if (usingMainHand) {
					lwc.sendLocale(player, "protection.interact.error.blocked");
				}
                event.setCancelled(true);
                return;
            }
        }

        try {
            Set<String> actions = lwcPlayer.getActionNames();
            Protection protection = lwc.findProtection(block.getLocation());

            if (protection != null && protection.getBlock().getType() != protection.getBlockType()) {
                // this block is no longer the block that's supposed to be protected
                protection.remove();
                return;
            }

            Module.Result result;
            boolean canAccess = lwc.canAccessProtection(player, protection);
            
			if(!usingMainHand) {
				result = Module.Result.DEFAULT;
			} else {
				if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
					boolean ignoreLeftClick = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(block, "ignoreLeftClick"));

					if (ignoreLeftClick) {
						return;
					}
				} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					boolean ignoreRightClick = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(block, "ignoreRightClick"));

					if (ignoreRightClick) {
						return;
					}
				}

				// Calculate if the player has a pending action (i.e any action besides 'interacted')
				int actionCount = actions.size();
				boolean hasInteracted = actions.contains("interacted");
				boolean hasPendingAction = (hasInteracted && actionCount > 1) || (!hasInteracted && actionCount > 0);

				// If the event was cancelled and they have an action, warn them
				if (event.isCancelled()) {
					// only send it if a non-"interacted" action is set which is always set on the player
					if (hasPendingAction) {
						lwc.sendLocale(player, "lwc.pendingaction");
					}

					// it's cancelled, do not continue !
					return;
				}

				// register in an action what protection they interacted with (if applicable.)
				if (protection != null) {
					com.griefcraft.model.Action action = new com.griefcraft.model.Action();
					action.setName("interacted");
					action.setPlayer(lwcPlayer);
					action.setProtection(protection);

					lwcPlayer.addAction(action);
				}

				// events are only used when they already have an action pending
				boolean canAdmin = lwc.canAdminProtection(player, protection);

				// allow changing the protection type
				if (protection == null || (actions.contains("create") && protection.isOwner(player))) {
					LWCBlockInteractEvent evt = new LWCBlockInteractEvent(event, block, actions);
					lwc.getModuleLoader().dispatchEvent(evt);

					result = evt.getResult();
				} else {
					LWCProtectionInteractEvent evt = new LWCProtectionInteractEvent(event, protection, actions, canAccess, canAdmin);
					lwc.getModuleLoader().dispatchEvent(evt);

					result = evt.getResult();
				}

				if (result == Module.Result.ALLOW) {
					return;
				}

				// optional.onlyProtectIfOwnerIsOnline
				if (protection != null && !canAccess && lwc.getConfiguration().getBoolean("optional.onlyProtectWhenOwnerIsOnline", false)) {
					Player owner = protection.getBukkitOwner();

					// If they aren't online, allow them in :P
					if (owner == null || !owner.isOnline()) {
						return;
					}
				}

				// optional.onlyProtectIfOwnerIsOffline
				if (protection != null && !canAccess && lwc.getConfiguration().getBoolean("optional.onlyProtectWhenOwnerIsOffline", false)) {
					Player owner = protection.getBukkitOwner();

					// If they aren't online, allow them in :P
					if (owner != null && owner.isOnline()) {
						return;
					}
				}
			}

            if (result == Module.Result.DEFAULT) {
                canAccess = lwc.enforceAccess(player, protection, block, canAccess, usingMainHand);
            }

            if (!canAccess || result == Module.Result.CANCEL) {
                event.setCancelled(true);
                event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            }
        } catch (Exception e) {
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            lwc.sendLocale(player, "protection.internalerror", "id", "PLAYER_INTERACT");
            e.printStackTrace();
        }
    }
	
    @EventHandler(ignoreCancelled = true)
    public void onTakeLecternBook(PlayerTakeLecternBookEvent event) {
		if (!LWC.ENABLED) {
            return;
        }

        final LWC lwc = plugin.getLWC();

		final Block b = event.getLectern().getBlock();
		final Protection protection = lwc.findProtection(b.getLocation());
		if(protection != null) {
			final Player player = event.getPlayer();

			// Can they admin it? (remove items/etc)
			if (!lwc.canAdminProtection(player, protection)) {
				event.setCancelled(true);
			}
		}
	}

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        // remove the place from the player cache and reset anything they can access
        LWCPlayer.removePlayer(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getSlot() < 0) {
            return;
        }

        LWC lwc = LWC.getInstance();

        // Player interacting with the inventory
        Player player = (Player) event.getWhoClicked();
        
        // Location of the container
        Location location;
        InventoryHolder holder;
		boolean holderIsEntity = false;

        try {
            holder = event.getInventory().getHolder();
        } catch (AbstractMethodError e) {
            lwc.log("Caught issue with Bukkit's Inventory.getHolder() method! This is occuring NEAR the player: " + player.getName());
            lwc.log("This player is located at: " + player.getLocation().toString());
            lwc.log("This should be reported to the Bukkit developers.");
            e.printStackTrace();
            return;
        }

        try {
            if (holder instanceof BlockState) {
                location = ((BlockState) holder).getLocation();
            } else if (holder instanceof DoubleChest) {
                location = ((DoubleChest) holder).getLocation();
            } else if (holder instanceof Minecart) {
				holderIsEntity = true;
                Minecart m = (Minecart) holder;
                int A = EntityBlock.calcHash(m.getUniqueId().hashCode());
                location = new Location(m.getWorld(), A, A, A);
            } else {
                return;
            }
        } catch (Exception e) {
            Location ploc = player.getLocation();
            String holderName = holder != null ? holder.getClass().getSimpleName() : "Unknown Block";
            lwc.log("Exception with getting the location of a " + holderName + " has occurred NEAR the player: " + player.getName() + " [" + ploc.getBlockX() + " " + ploc.getBlockY() + " " + ploc.getBlockZ() + "]");
            lwc.log("The exact location of the block is not possible to obtain. This is caused by a Minecraft or Bukkit exception normally.");
            e.printStackTrace();
            return;
        }

		// Attempt to load the protection at that location
		Protection protection = holderIsEntity
				? lwc.getPhysicalDatabase().loadProtection(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ())
				: lwc.findProtection(location);

        // If no protection was found we can safely ignore it
        if (protection == null) {
            return;
        }

        // If it's not a donation, display or supply chest, ignore it
        if (protection.getType() != Protection.Type.DONATION
                && protection.getType() != Protection.Type.DISPLAY
                && protection.getType() != Protection.Type.SUPPLY) {
            return;
        }
        
        if (protection.getType() == Protection.Type.DONATION && event.getAction() != InventoryAction.COLLECT_TO_CURSOR) {
            // If it's not a container, we don't want it
            if (event.getSlotType() != InventoryType.SlotType.CONTAINER) {
                return;
            }

            // Nifty trick: these will different IFF they are interacting with the player's inventory or hotbar instead of the block's inventory
            if (event.getSlot() != event.getRawSlot()) {
                return;
            }

            // The item they are taking/swapping with
            ItemStack item;

            try {
                item = event.getCurrentItem();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }

            // Item their cursor has
            ItemStack cursor = event.getCursor();

            if (item == null || item.getType() == null || item.getType() == Material.AIR) {
                return;
            }

            // if it's not a right click or a shift click it should be a left click (no shift)
            // this is for when players are INSERTing items (i.e. item in hand and left clicking)
            if (player.getItemInHand() == null && (!event.isRightClick() && !event.isShiftClick())) {
                return;
            }

            // Are they inserting a stack?
            if (cursor != null && item.getType() == cursor.getType()) {
                boolean enchantmentsEqual = areEnchantmentsEqual(item, cursor);

                // If they are clicking an item of the stack type, they are inserting it into the inventory,
                // not switching it
                // As long as the item isn't a degradable item, we can explicitly allow it if they have the same durability
                if (item.getDurability() == cursor.getDurability() && item.getAmount() == cursor.getAmount() && enchantmentsEqual) {
                    return;
                }
            }
        } else if (protection.getType() == Protection.Type.SUPPLY) {
            // We check furnace fuel slot because furnace should supply only its result.
            if (event.getSlotType() != InventoryType.SlotType.FUEL) {
                // Not furnace.
                // We ignore those slot type:
                // - outside of inventory ... nothing to check.
                // - craft table result and furnace result... behaviour of these slots are like supply chest originally.
                // - armor slot ... these slots should not appear when we see container inventory.
                if (event.getSlotType() != InventoryType.SlotType.CONTAINER && event.getSlotType() != InventoryType.SlotType.QUICKBAR) {
                    return;
                }
                boolean clickedTopInventory = isRawSlotInTopInventory(event.getView(), event.getRawSlot());
                switch (event.getAction()) {
                    // add or swap item in the slot.
                    case PLACE_ALL:
                    case PLACE_SOME:
                    case PLACE_ONE:
                    case SWAP_WITH_CURSOR:
                        // check if a clicked slot is in top inventory.
                        if (clickedTopInventory) {
                            break;
                        }
                        return;
                    // these actions can also swap items in supply chest.
                    case HOTBAR_MOVE_AND_READD:
                    case HOTBAR_SWAP:
                        // check if a clicked slot is in top inventory and a hotbar item is empty.
                        if (clickedTopInventory && event.getView().getBottomInventory().getItem(event.getHotbarButton()) != null) {
                            break;
                        }
                        return;
                    case MOVE_TO_OTHER_INVENTORY:
                        // check if a clicked slot is in bottom inventory.
                        if (!clickedTopInventory) {
                            break;
                        }
                        return;
                    // COLLECT_TO_CURSOR, PICKUP_ALL,
                    // PICKUP_SOME, PICKUP_HALF,        -> players can pick up items from supply chest. ignore it.
                    // PICKUP_ONE
                    // DROP_ALL_SLOT, DROP_ONE_SLOT     -> players can drop items from supply chest. ignore it.
                    // DROP_ALL_CURSOR, DROP_ONE_CURSOR -> these actions will not come here
                    //                                     because OUTSIDE slot type is ignored above.
                    // CLONE_STACK                      -> this action will not come here
                    //                                     because CLONE_STACK is only for creative inventory.
                    // NOTHING, UNKNOWN                 -> nothing to check.
                    default:
                        return;
                }
            }
        }

        // Can they admin it? (remove items/etc)
        boolean canAdmin = lwc.canAdminProtection(player, protection);

        // allow ACL users to open read-only chests
        if (!canAdmin) {
            canAdmin = protection.getAccess(player.getUniqueId().toString(), Permission.Type.PLAYER) == Permission.Access.PLAYER;
        }
        // extend ACL to include groups
        if (!canAdmin) {
            IPermissions permissions = lwc.getPermissions();
            if (permissions != null) {
                for (String groupName : permissions.getGroups(player)) {
                    if (protection.getAccess(groupName, Permission.Type.GROUP) == Permission.Access.PLAYER) {
                        canAdmin = true;
                        break;
                    }
                }
            }
        }

        // nope.avi
        if (!canAdmin) {
            event.setCancelled(true);
        }
    }

    // Mostly a copy of the inventory click event, but intended to disable dragging in display chests
    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        LWC lwc = LWC.getInstance();

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // Player interacting with the inventory
        Player player = (Player) event.getWhoClicked();

        // Location of the container
        Location location;
        InventoryHolder holder = null;

        try {
            holder = event.getInventory().getHolder();
        } catch (AbstractMethodError e) {
            e.printStackTrace();
            return;
        }

        try {
            if (holder instanceof BlockState) {
                location = ((BlockState) holder).getLocation();
            } else if (holder instanceof DoubleChest) {
                location = ((DoubleChest) holder).getLocation();
            } else {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Attempt to load the protection at that location
        Protection protection = lwc.findProtection(location);

        // If no protection was found we can safely ignore it
        if (protection == null) {
            return;
        }

        // If it's a display chest, check permission.
        if (protection.getType() == Protection.Type.DISPLAY) {
            // Can they admin it? (remove items/etc)
            boolean canMod = lwc.isMod(player);
            boolean canAdmin = lwc.canAdminProtection(player, protection);

            // nope.avi
            if (!canMod && !canAdmin) {
                event.setCancelled(true);
            }

            // If it's a supply chest and player drag items on its slot, check permission.
        } else if (protection.getType() == Protection.Type.SUPPLY
                && event.getRawSlots().stream().anyMatch(slot -> isRawSlotInTopInventory(event.getView(), slot))) {
            // Can they admin it? (remove items/etc)
            boolean canMod = lwc.isMod(player);
            boolean canAdmin = lwc.canAdminProtection(player, protection);

            // nope.avi
            if (!canMod && !canAdmin) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void hangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
        onDamage(event.getEntity(), event.getRemover(), event, true);
    }

    @EventHandler
    public void onMinecartDamage(VehicleDamageEvent event) {
        onDamage(event.getVehicle(), event.getAttacker(), event, false);
    }

    @EventHandler
    public void onEntityDamageByExplosion(EntityDamageEvent event) {
        if(event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
            onDamage(event.getEntity(), null, event, false);
        }
    }

    private void onDamage(Entity entity, Entity damager, Cancellable event, boolean onBreak) {
        if (!LWC.ENABLED) {
            return;
        }
        int hash = EntityBlock.calcHash(entity.getUniqueId().hashCode());
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity.getType())) {
            return; // don't check not protectable entities to improve performance of damage events
        }
        Protection protection = lwc.getPhysicalDatabase().loadProtection(entity.getWorld().getName(), hash, hash, hash);
        if (protection != null && protection.getWorld() == null) {
            System.out.println("Correcting malformed protection: " + protection + " (fixing world)");
            protection.setWorld(entity.getWorld().getName());
        }

        // check if we can update this protection's id
        if (protection != null && protection.getBlockTypeString().equals(EntityBlock.UNKNOWN_ENTITY_TYPE)) {
            final String nid = EntityBlock.calcTypeString(entity);
            if (!nid.equals(EntityBlock.UNKNOWN_ENTITY_TYPE)) {
                protection.setBlockType(nid);
                protection.save();
            }
        }

        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                damager = (Player) shooter;
            } else if (protection != null) {
                event.setCancelled(true);
                return;
            }
        }

        if (damager instanceof Player) {
            Player p = (Player) damager;
            if (onPlayerEntityInteract(p, entity, false, event.isCancelled())) {
                event.setCancelled(true);
            } else if (onBreak && protection != null) {
                try {
                    boolean canAccess = lwc.canAccessProtection(p, protection);
                    boolean canAdmin = lwc.canAdminProtection(p, protection);

                    LWCProtectionDestroyEvent evt = new LWCProtectionDestroyEvent(
                            p, protection,
                            LWCProtectionDestroyEvent.Method.BLOCK_DESTRUCTION,
                            canAccess, canAdmin);
                    lwc.getModuleLoader().dispatchEvent(evt);

                    if (evt.isCancelled() || !canAccess) {
                        event.setCancelled(true);
                    }
                } catch (Exception e) {
                    event.setCancelled(true);
                    lwc.sendLocale(p, "protection.internalerror", "id", "BLOCK_BREAK");
                    e.printStackTrace();
                }
            } else if (protection != null) {
                event.setCancelled(true);
            }
        } else if (protection != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMinecartBreak(VehicleDestroyEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        Entity entity = event.getVehicle();
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity.getType())) {
            return; // don't check not protectable entities to improve performance of damage events
        }

        int A = EntityBlock.calcHash(entity.getUniqueId().hashCode());
        Protection protection = lwc.getPhysicalDatabase().loadProtection(entity.getWorld().getName(), A, A, A);

        Entity breaksource = event.getAttacker();
        if (breaksource instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) breaksource).getShooter();
            if (shooter instanceof Player) {
                breaksource = (Player) shooter;
            } else if (protection != null) {
                event.setCancelled(true);
                return;
            }
        }

        if (breaksource instanceof Player) {
            Player p = (Player) breaksource;
            if (onPlayerEntityInteract(p, entity, false, event.isCancelled())) {
                event.setCancelled(true);
            } else if (protection != null) {
                try {
                    boolean canAccess = lwc.canAccessProtection(p, protection);
                    boolean canAdmin = lwc.canAdminProtection(p, protection);

                    LWCProtectionDestroyEvent evt = new LWCProtectionDestroyEvent(
                            p, protection,
                            LWCProtectionDestroyEvent.Method.BLOCK_DESTRUCTION,
                            canAccess, canAdmin);
                    lwc.getModuleLoader().dispatchEvent(evt);

                    if (evt.isCancelled() || !canAccess) {
                        event.setCancelled(true);
                    }
                } catch (Exception e) {
                    event.setCancelled(true);
                    lwc.sendLocale(p, "protection.internalerror", "id", "BLOCK_BREAK");
                    e.printStackTrace();
                }
            } else if (protection != null) {
                event.setCancelled(true);
            }
        } else if (protection != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void hangingBreak(HangingBreakEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        Entity entity = event.getEntity();
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity.getType())) {
            return; // don't check not protectable entities to improve performance of damage events
        }

        int A = EntityBlock.calcHash(entity.getUniqueId().hashCode());
        Protection protection = lwc.getPhysicalDatabase().loadProtection(entity.getWorld().getName(), A, A, A);
        if (protection != null) {
            if (event.getCause() == RemoveCause.PHYSICS
                    || event.getCause() == RemoveCause.EXPLOSION
                    || event.getCause() == RemoveCause.OBSTRUCTION) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (!LWC.ENABLED) {
            return;
        }

        Entity entity = e.getRightClicked();
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity.getType())) {
            return; // don't check not protectable entities to improve performance of damage events
        }

        int A = EntityBlock.calcHash(entity.getUniqueId().hashCode());
        Protection protection = lwc.getPhysicalDatabase().loadProtection(entity.getWorld().getName(), A, A, A);
        
        // check if we can update this protection's id
        if(protection != null && protection.getBlockTypeString().equals(EntityBlock.UNKNOWN_ENTITY_TYPE)) {
            final String nid = EntityBlock.calcTypeString(entity);
            if(nid.equals(EntityBlock.UNKNOWN_ENTITY_TYPE)) {
                protection.setBlockType(nid);
                protection.save();
            }
        }
        
        Player p = e.getPlayer();
        boolean canAccess = lwc.canAccessProtection(p, protection);
        if (onPlayerEntityInteract(p, entity, true, e.isCancelled())) {
            e.setCancelled(true);
        }
        if (protection != null) {
            if (canAccess) {
                return;
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (!LWC.ENABLED) {
            return;
        }

        if (e instanceof EntityDamageByEntityEvent
                && !(e.getCause() == DamageCause.BLOCK_EXPLOSION || e.getCause() == DamageCause.ENTITY_EXPLOSION)) {
            return; // handle this separately
        }
        Entity entity = e.getEntity();
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity.getType())) {
            return; // don't check not protectable entities to improve performance of damage events
        }

        int A = EntityBlock.calcHash(entity.getUniqueId().hashCode());
        Protection protection = lwc.getPhysicalDatabase().loadProtection(entity.getWorld().getName(), A, A, A);
        if (protection != null) {
            if (e.getCause() != DamageCause.CONTACT) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void itemFrameItemRemoval(EntityDamageByEntityEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity.getType())) {
            return; // don't check not protectable entities to improve performance of damage events
        }
        Entity damager = event.getDamager();
        int A = EntityBlock.calcHash(entity.getUniqueId().hashCode());
        Protection protection = lwc.getPhysicalDatabase().loadProtection(entity.getWorld().getName(), A, A, A);
        if (protection != null && damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                damager = (Player) shooter;
            } else {
                event.setCancelled(true);
                return;
            }
        }
        if (!(damager instanceof Player)) {
            if (protection != null) {
                event.setCancelled(true);
            }
            return;
        }
        
        // check if we can update this protection's id
        if(protection != null && protection.getBlockTypeString().equals(EntityBlock.UNKNOWN_ENTITY_TYPE)) {
            final String nid = EntityBlock.calcTypeString(entity);
            if(nid.equals(EntityBlock.UNKNOWN_ENTITY_TYPE)) {
                protection.setBlockType(nid);
                protection.save();
            }
        }

        Player player = (Player) damager;

        if (((entity instanceof ItemFrame) || (entity instanceof InventoryHolder)) && lwc.isProtectable(entity.getType())) {
            // Prevent players with lwc.deny from interacting with blocks that have an inventory
            if (!lwc.hasPermission(player, "lwc.protect") && lwc.hasPermission(player, "lwc.deny") && !lwc.isAdmin(player) && !lwc.isMod(player)) {
                lwc.sendLocale(player, "protection.interact.error.blocked");
                event.setCancelled(true);
                return;
            }
        }

        if (onPlayerEntityInteract(player, entity, false, event.isCancelled())) {
            event.setCancelled(true);
        } else {
            playerDamagedEntities.put(player, entity);
        }
    }

    protected final HashMap<Player, Entity> playerDamagedEntities = new HashMap<>();

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        // if you've got this far, it's already destroyed
        Entity entity = event.getEntity();
        if (entity instanceof ArmorStand) {
            int A = EntityBlock.calcHash(entity.getUniqueId().hashCode());
            LWC lwc = LWC.getInstance();
            Protection protection = lwc.getPhysicalDatabase().loadProtection(entity.getWorld().getName(), A, A, A);
            if (protection != null) {
                Player player = event.getEntity().getKiller();
                if (player == null) {
                    // workaround, because mc apparently doesn't bother to fill this field in
                    final int hash = entity.hashCode();
                    for (Map.Entry<Player, Entity> e : playerDamagedEntities.entrySet()) {
                        if (e.getValue().hashCode() == hash) {
                            player = e.getKey();
                            playerDamagedEntities.remove(e.getKey());
                            break;
                        }
                    }
                }
                if (player != null) {
                    try {
                        boolean canAccess = lwc.canAccessProtection(player, protection);
                        boolean canAdmin = lwc.canAdminProtection(player, protection);
                        LWCProtectionDestroyEvent evt = new LWCProtectionDestroyEvent(
                                player, protection,
                                LWCProtectionDestroyEvent.Method.ENTITY_DESTRUCTION,
                                canAccess, canAdmin);
                        lwc.getModuleLoader().dispatchEvent(evt);
                    } catch (Exception ex) {
                        lwc.sendLocale(player, "protection.internalerror", "id", "BLOCK_BREAK");
                        ex.printStackTrace();
                    }
                }
                protection.remove();
            }
        }
    }
/*
    @EventHandler
    public void onEntityAtInteract(PlayerInteractAtEntityEvent event) {
    }*/

    /**
     * Restrict access to protected armor stands
     *
     * @param event
     */
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        Entity entity = event.getRightClicked();
        if (entity instanceof Player) {
            return;
        }

        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity.getType())) {
            return; // don't check not protectable entities to improve performance of damage events
        }

        Player player = event.getPlayer();
        if (((entity instanceof ItemFrame) || (entity instanceof ArmorStand)) && lwc.isProtectable(entity.getType())) {
            // Prevent players with lwc.deny from interacting with blocks that have an inventory
            if (!lwc.hasPermission(player, "lwc.protect") && lwc.hasPermission(player, "lwc.deny") && !lwc.isAdmin(player) && !lwc.isMod(player)) {
                lwc.sendLocale(player, "protection.interact.error.blocked");
                event.setCancelled(true);
                return;
            }
        }

        if (onPlayerEntityInteract(player, entity, false, event.isCancelled())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void storageMinecraftInventoryOpen(InventoryOpenEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Minecart)) {
            return;
		}

        if (onPlayerEntityInteract((Player) event.getPlayer(), (Entity) holder, true, event.isCancelled())) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if interaction is protected
     *
     * @param player
     * @param entity
     * @param rightClick if this event is triggered with a right click
     * @param cancelled
     * @return true if interaction is not allowed
     */
    private boolean onPlayerEntityInteract(Player player, Entity entity, boolean rightClick, boolean cancelled) {
        LWC lwc = LWC.getInstance();
        if (!lwc.isProtectable(entity.getType())) {
            return false; // don't check not protectable entities to improve performance of damage events
        }
        LWCPlayer lwcPlayer = lwc.wrapPlayer(player);
        int A = EntityBlock.calcHash(entity.getUniqueId().hashCode());
        Protection protection = lwc.getPhysicalDatabase().loadProtection(entity.getWorld().getName(), A, A, A);
        try {
            Set<String> actions = lwcPlayer.getActionNames();

            boolean canAccess = lwc.canAccessProtection(player, protection);

            // Calculate if the player has a pending action (i.e any action besides 'interacted')
            int actionCount = actions.size();
            boolean hasInteracted = actions.contains("interacted");
            boolean hasPendingAction = ((hasInteracted) && (actionCount > 1)) || ((!hasInteracted) && (actionCount > 0));

            // If the event was cancelled and they have an action, warn them
            if (cancelled) {
                // only send it if a non-"interacted" action is set which is
                // always set on the player
                if (hasPendingAction) {
                    lwc.sendLocale(player, "lwc.pendingaction", new Object[0]);
                }

                // it's cancelled, do not continue !
                return false;
            }

            // register in an action what protection they interacted with (if applicable.)
            if (protection != null) {
                com.griefcraft.model.Action action = new com.griefcraft.model.Action();
                action.setName("interacted");
                action.setPlayer(lwcPlayer);
                action.setProtection(protection);

                lwcPlayer.addAction(action);
            }

            // events are only used when they already have an action pending
            boolean canAdmin = lwc.canAdminProtection(player, protection);
            Module.Result result;

            Block fakeBlock = EntityBlock.getEntityBlock(entity);
            PlayerInteractEvent fakeEvent = new PlayerInteractEvent(player,
                    rightClick ? org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                            : org.bukkit.event.block.Action.LEFT_CLICK_BLOCK,
                    null, fakeBlock, null);

			// allow changing the protection type
			if (protection == null || (actions.contains("create") && protection.isOwner(player))) {
				LWCEntityInteractEvent evt = new LWCEntityInteractEvent(fakeEvent, entity, actions);
				lwc.getModuleLoader().dispatchEvent(evt);

				result = evt.getResult();
			} else {
				LWCProtectionInteractEvent evt = new LWCProtectionInteractEvent(fakeEvent, protection, actions, canAccess, canAdmin);
				lwc.getModuleLoader().dispatchEvent(evt);

				result = evt.getResult();
			}

            if (result == Module.Result.ALLOW) {
                return false;
            }

            // optional.onlyProtectIfOwnerIsOnline
            if (protection != null && !canAccess) {
                if (lwc.getConfiguration().getBoolean("optional.onlyProtectWhenOwnerIsOnline", false)) {
                    Player owner = protection.getBukkitOwner();

                    // If they aren't online, allow them in :P
                    if (owner == null || !owner.isOnline()) {
                        return false;
                    }
                }
            }

            // optional.onlyProtectIfOwnerIsOffline
            if (protection != null && !canAccess) {
                if (lwc.getConfiguration().getBoolean("optional.onlyProtectWhenOwnerIsOffline", false)) {
                    Player owner = protection.getBukkitOwner();

                    // If they aren't online, allow them in :P
                    if (owner != null && owner.isOnline()) {
                        return false;
                    }
                }
            }
            if (result == Module.Result.DEFAULT) {
                canAccess = lwc.enforceAccess(player, protection, entity, canAccess);
            }

            if (!canAccess || result == Module.Result.CANCEL) {
                return true;
            }
        } catch (Exception e) {
            lwc.sendLocale(player, "protection.internalerror", new Object[]{"id", "PLAYER_INTERACT"});
            e.printStackTrace();
            return true;
        }
        return false;
    }

    /**
     * Compares the enchantments on two item stacks and checks that they are equal (identical)
     *
     * @param stack1
     * @param stack2
     * @return
     */
    private boolean areEnchantmentsEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1 == null || stack2 == null) {
            return false;
        }

        Map<Enchantment, Integer> enchantments1 = stack1.getEnchantments();
        Map<Enchantment, Integer> enchantments2 = stack2.getEnchantments();

        if (enchantments1.size() != enchantments2.size()) {
            return false;
        }

        // Enchanted Books use ItemMeta
        if (stack1.getItemMeta() != null && stack2.getItemMeta() != null) {
            if (!stack1.getItemMeta().equals(stack2.getItemMeta())) {
                return false;
            }
        }

        for (Enchantment enchantment : enchantments1.keySet()) {
            if (!enchantments2.containsKey(enchantment)) {
                return false;
            }

            int level1 = enchantments1.get(enchantment);
            int level2 = enchantments2.get(enchantment);

            if (level1 != level2) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if the rawSlot is smaller than top inventory size, that means
     * specified slot is in the top inventory.
     *
     * @param view the inventory view.
     * @param rawSlot the raw slot.
     * @return true if specified raw slot is in the top inventory.
     *
     * @see InventoryView#getInventory(int)
     */
    private static boolean isRawSlotInTopInventory(InventoryView view, int rawSlot) {
        return rawSlot < view.getTopInventory().getSize();
    }
}
