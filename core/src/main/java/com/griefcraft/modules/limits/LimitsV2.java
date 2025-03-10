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

package com.griefcraft.modules.limits;

import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.lwc.LWC;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCCommandEvent;
import com.griefcraft.scripting.event.LWCProtectionRegisterEvent;
import com.griefcraft.scripting.event.LWCReloadEvent;
import com.griefcraft.util.config.Configuration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;

public class LimitsV2 extends JavaModule {

    /**
     * The limit represented by unlimited
     */
    public final static int UNLIMITED = Integer.MAX_VALUE;

    /**
     * If the limits module is enabled
     */
    private boolean enabled = true;

    /**
     * The limits configuration
     */
    private final Configuration configuration = Configuration.load("limitsv2.yml");

    /**
     * A map of the default limits
     */
    private final List<Limit> defaultLimits = new LinkedList();

    /**
     * A map of all of the player limits
     */
    private final Map<String, List<Limit>> playerLimits = new HashMap();

    /**
     * A map of all of the group limits - downcasted to lowercase to simplify comparisons
     */
    private final Map<String, List<Limit>> groupLimits = new HashMap();

    /**
     * A map mapping string representations of materials to their Material counterpart
     */
    private final Map<String, Material> materialCache = new HashMap();

    /**
     * A map mapping string representations of entity types to their EntityType counterpart
     */
    private final Map<String, EntityType> entityCache = new HashMap<>();

    {
        for (Material material : Material.values()) {
            String materialName = LWC.normalizeMaterialName(material);

            // add the name & the block id
            materialCache.put(materialName, material);

            if (!materialName.equals(material.toString().toLowerCase())) {
                materialCache.put(material.toString().toLowerCase(), material);
            }
        }

        for (EntityType entityType : EntityType.values()) {
            entityCache.put(entityType.toString().toLowerCase(), entityType);
            entityCache.put(EntityBlock.calcTypeString(entityType), entityType);
        }
    }

    public abstract class Limit {

        /**
         * The limit
         */
        private final int limit;

        public Limit(int limit) {
            this.limit = limit;
        }

        /**
         * Get the player's protection count that should be used with this limit
         *
         * @param player
         * @return
         */
        public abstract int getProtectionCount(Player player);

        /**
         * Get the name of the type that this limit represents

         * @return
         */
        public abstract String getTypeName();

        /**
         * Check whether or not this limit accepts a certain type
         *
         * @param type
         * @return
         */
        public boolean accepts(String type) {
            return getTypeName().equalsIgnoreCase(type);
        }

        /**
         * @return
         */
        public int getLimit() {
            return limit;
        }
    }

    public final class DefaultLimit extends Limit {

        public DefaultLimit(int limit) {
            super(limit);
        }

        @Override
        public int getProtectionCount(Player player) {
            return LWC.getInstance().getPhysicalDatabase().getProtectionCount(player.getName());
        }

        @Override
        public String getTypeName() {
            return "default";
        }

    }

    public final class BlockLimit extends Limit {

        /**
         * The block material to limit
         */
        private final Material material;

        public BlockLimit(Material material, int limit) {
            super(limit);
            this.material = material;
        }

        @Override
        public int getProtectionCount(Player player) {
            return LWC.getInstance().getPhysicalDatabase().getProtectionCount(player.getName(), material);
        }

        @Override
        public String getTypeName() {
            return material.toString();
        }

        /**
         * @return
         */
        public Material getMaterial() {
            return material;
        }

    }

    public final class EntityLimit extends Limit {

        /**
         * The block material to limit
         */
        private final EntityType entityType;

        public EntityLimit( EntityType entityType, int limit) {
            super(limit);
            this.entityType = entityType;
        }

        @Override
        public int getProtectionCount(Player player) {
            return LWC.getInstance().getPhysicalDatabase().getProtectionCount(player.getName(), EntityBlock.calcTypeString(entityType));
        }

        @Override
        public String getTypeName() {
            return entityType.toString();
        }

        /**
         * @return
         */
        public EntityType getEntityType() {
            return entityType;
        }

    }

    public final class SignLimit extends Limit {

        public SignLimit(int limit) {
            super(limit);
        }

        @Override
        public int getProtectionCount(Player player) {
            LWC lwc = LWC.getInstance();
            return lwc.getPhysicalDatabase().getProtectionCountSimilar(player.getName(), "SIGN");
        }

        @Override
        public boolean accepts(String type) {
            return type.toLowerCase().contains("sign");
        }

        @Override
        public String getTypeName() {
            return "sign";
        }

    }

    public LimitsV2() {
        enabled = LWC.getInstance().getConfiguration().getBoolean("optional.useProtectionLimits", true);

        if (enabled) {
            loadLimits();
        }
    }

    @Override
    public void onReload(LWCReloadEvent event) {
        if (enabled) {
            reload();
        }
    }

    @Override
    public void onRegisterProtection(LWCProtectionRegisterEvent event) {
        if (!enabled || event.isCancelled()) {
            return;
        }

        LWC lwc = event.getLWC();
        Player player = event.getPlayer();
        String typeName;
        if (event.getBlock() instanceof EntityBlock) {
            typeName = ((EntityBlock) event.getBlock()).getEntityType().toString();
        } else {
            typeName = event.getBlock().getType().toString();
        }

        if (hasReachedLimit(player, typeName)) {
            lwc.sendLocale(player, "protection.exceeded");
            event.setCancelled(true);
        }
    }

    @Override
    public void onCommand(LWCCommandEvent event) {
        if (!enabled || event.isCancelled()) {
            return;
        }

        if (!event.hasFlag("limits")) {
            return;
        }

        LWC lwc = event.getLWC();
        CommandSender sender = event.getSender();
        String[] args = event.getArgs();
        event.setCancelled(true);

        String playerName;

        if (args.length == 0) {
            if (args.length == 0 && !(sender instanceof Player)) {
                sender.sendMessage(ChatColor.DARK_RED + "You are not a player!");
                return;
            }

            playerName = sender.getName();
        } else {
            if (lwc.isAdmin(sender)) {
                playerName = args[0];
            } else {
                lwc.sendLocale(sender, "protection.accessdenied");
                return;
            }
        }

        Player player = lwc.getPlugin().getServer().getPlayer(playerName);

        if (player == null) {
            sender.sendMessage(ChatColor.DARK_RED + "That player is not online!");
            return;
        }

        // send their limits to them
        sendLimits(sender, player, getPlayerLimits(player));
    }

    /**
     * Gets the raw limits configuration
     *
     * @return
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Reload the limits
     */
    public void reload() {
        loadLimits();
    }

    /**
     * Sends the list of limits to the player
     *
     * @param sender the commandsender to send the limits to
     * @param target the player limits are being shown for, can be null
     * @param limits
     */
    public void sendLimits(CommandSender sender, Player target, List<Limit> limits) {
        LWC lwc = LWC.getInstance();

        for (Limit limit : limits) {
            if (limit == null) {
                continue;
            }

            String stringLimit = limit.getLimit() == UNLIMITED ? "Unlimited" : Integer.toString(limit.getLimit());
            String colour = ChatColor.YELLOW.toString();

            if (target != null) {
                boolean reachedLimit = hasReachedLimit(target, limit.getTypeName());
                colour = reachedLimit ? ChatColor.DARK_RED.toString() : ChatColor.DARK_GREEN.toString();
            }

            String currentProtected = target != null ? (Integer.toString(limit.getProtectionCount(target)) + "/") : "";
            if (limit instanceof DefaultLimit) {
                sender.sendMessage("Default: " + colour + currentProtected + stringLimit);
            } else if (limit instanceof BlockLimit) {
                BlockLimit blockLimit = (BlockLimit) limit;
                sender.sendMessage(LWC.materialToString(blockLimit.getMaterial()) + ": " + colour + currentProtected + stringLimit);
            } else if (limit instanceof EntityLimit) {
                EntityLimit entityLimit = (EntityLimit) limit;
                sender.sendMessage(LWC.entityToString(entityLimit.getEntityType()) + ": " + colour + currentProtected + stringLimit);
            } else if (limit instanceof SignLimit) {
                sender.sendMessage("Sign: " + colour + currentProtected + stringLimit);
            } else {
                sender.sendMessage(limit.getClass().getSimpleName() + ": " + ChatColor.YELLOW + stringLimit);
            }

        }
    }

    /**
     * Checks if a player has reached their protection limit
     *
     * @param player
     * @param type the material type the player has interacted with
     * @return
     */
    public boolean hasReachedLimit(Player player, String type) {
        Limit limit = getEffectiveLimit(player, type);

        // if they don't have a limit it's not possible to reach it ^^
        //  ... but if it's null, what the hell did the server owner do?
        if (limit == null) {
            return false;
        }

        // Get the effective limit placed upon them
        int neverPassThisNumber = limit.getLimit();

        // get the amount of protections the player has
        int protections = limit.getProtectionCount(player);

        return protections >= neverPassThisNumber;
    }

    /**
     * Find limits that are attached to the player via permissions (e.g lwc.protect.*.10 = 10 of any protection)
     *
     * @param player
     * @return
     */
    private List<Limit> findLimitsViaPermissions(Player player) {
        List<Limit> limits = new LinkedList();

        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String permission = pai.getPermission();
            boolean value = pai.getValue();

            if (!value || !permission.startsWith("lwc.protect.")) {
                continue;
            }

            String[] split = permission.substring("lwc.protect.".length()).split(".");

            if (split.length != 2) {
                continue;
            }

            String matchName = split[0];
            String strCount = split[1];

            int count;
            try {
                count = Integer.parseInt(strCount);
            } catch (NumberFormatException e) {
                continue;
            }

            if (matchName.equals("*")) {
                limits.add(new DefaultLimit(count));
            } else if (matchName.equals("sign")) {
                limits.add(new SignLimit(count));
            } else {
                Material material = materialCache.get(matchName);
                if (material != null) {
                    limits.add(new BlockLimit(material, count));
                }

                EntityType entityType = entityCache.get(matchName);
                if (entityType != null) {
                    limits.add(new EntityLimit(entityType, count));
                }

            }
        }

        return limits;
    }

    /**
     * Gets the list of limits that may apply to the player.
     * For group limits, it uses the highest one found.
     *
     * @param player
     * @return
     */
    public List<Limit> getPlayerLimits(Player player) {
        LWC lwc = LWC.getInstance();
        List<Limit> limits = new LinkedList();

        // get all of their own limits
        String playerName = player.getName().toLowerCase();
        if (playerLimits.containsKey(playerName)) {
            limits.addAll(playerLimits.get(playerName));
        }

        for (Limit limit : findLimitsViaPermissions(player)) {
            Limit matched = findLimit(limits, limit);

            if (matched != null) {
                // Is our limit better?
                if (limit.getLimit() > matched.getLimit()) {
                    limits.remove(matched);
                    limits.add(limit);
                }
            } else {
                limits.add(limit);
            }
        }

        // Look over the group limits
        for (String group : lwc.getPermissions().getGroups(player)) {
            if (groupLimits.containsKey(group.toLowerCase())) {
                for (Limit limit : groupLimits.get(group.toLowerCase())) {
                    // try to match one already inside what we found
                    Limit matched = findLimit(limits, limit);

                    if (matched != null) {
                        // Is our limit better?
                        if (limit.getLimit() > matched.getLimit()) {
                            limits.remove(matched);
                            limits.add(limit);
                        }
                    } else {
                        limits.add(limit);
                    }
                }
            }
        }

        // Look at the default limits
        for (Limit limit : defaultLimits) {
            // try to match one already inside what we found
            Limit matched = findLimit(limits, limit);

            if (matched == null) {
                limits.add(limit);
            }
        }

        return limits;
    }

    /**
     * Get the player's effective limit that should take precedence
     *
     * @param player
     * @param string
     * @return
     */
    public Limit getEffectiveLimit(Player player, String string) {
        return getEffectiveLimit(getPlayerLimits(player), string);
    }

    /**
     * Gets an immutable list of the default limits
     *
     * @return
     */
    public List<Limit> getDefaultLimits() {
        return Collections.unmodifiableList(defaultLimits);
    }

    /**
     * Gets an unmodiable map of the player limits
     *
     * @return
     */
    public Map<String, List<Limit>> getPlayerLimits() {
        return Collections.unmodifiableMap(playerLimits);
    }

    /**
     * Gets an unmodiable map of the group limits
     *
     * @return
     */
    public Map<String, List<Limit>> getGroupLimits() {
        return Collections.unmodifiableMap(groupLimits);
    }

    /**
     * Orders the limits, putting the default limit at the top of the list
     *
     * @param limits
     * @return
     */
    private List<Limit> orderLimits(List<Limit> limits) {
        Limit defaultLimit = null;

        // Locate the default limit
        for (Limit limit : limits) {
            if (limit instanceof DefaultLimit) {
                defaultLimit = limit;
                break;
            }
        }

        // remove it
        limits.remove(defaultLimit);

        // readd it at the head
        limits.add(0, defaultLimit);

        return limits;
    }

    /**
     * Gets the material's effective limit that should take precedence
     *
     * @param limits
     * @param type
     * @return Limit object if one is found otherwise NULL
     */
    private Limit getEffectiveLimit(List<Limit> limits, String type) {
        if (limits == null) {
            return null;
        }

        // Temporary storage to use if the default is found so we save time if no override was found
        Limit defaultLimit = null;

        for (Limit limit : limits) {
            // Record the default limit if found
            if (limit instanceof DefaultLimit) {
                defaultLimit = limit;
            } else if (limit.accepts(type)) {
                return limit;
            }
        }

        return defaultLimit;
    }

    /**
     * Load all of the limits
     */
    private void loadLimits() {
        // make sure we're working on a clean slate
        defaultLimits.clear();
        playerLimits.clear();
        groupLimits.clear();

        // add the default limits
        defaultLimits.addAll(findLimits("defaults"));

        // add all of the player limits
        try {
            for (String player : configuration.getKeys("players")) {
                playerLimits.put(player.toLowerCase(), findLimits("players." + player));
            }
        } catch (NullPointerException e) { }

        // add all of the group limits
        try {
            for (String group : configuration.getKeys("groups")) {
                groupLimits.put(group.toLowerCase(), findLimits("groups." + group));
            }
        } catch (NullPointerException e) { }
    }

    /**
     * Find and match all of the limits in a given list of nodes for the config
     *
     * @param node
     * @return
     */
    private List<Limit> findLimits(String node) {
        List<Limit> limits = new LinkedList();
        List<String> keys = configuration.getKeys(node);

        for (String key : keys) {
            String value = configuration.getString(node + "." + key);

            int limit;

            if (value.equalsIgnoreCase("unlimited")) {
                limit = UNLIMITED;
            } else {
                limit = Integer.parseInt(value);
            }

            // Match default
            if (key.equalsIgnoreCase("default")) {
                limits.add(new DefaultLimit(limit));
            } else if (key.equalsIgnoreCase("sign")) {
                limits.add(new SignLimit(limit));
            } else {
                // resolve the type
                Material material = materialCache.get(key);
                if (material != null) {
                    limits.add(new BlockLimit(material, limit));
                }

                EntityType entityType = entityCache.get(key);
                if (entityType != null) {
                    limits.add(new EntityLimit(entityType, limit));
                }
            }
        }

        // Order it
        orderLimits(limits);

        return limits;
    }

    /**
     * Find a limit in the list of limits that equals the given limit in class;
     * The LIMIT itself does not need to be equal; only the type
     *
     * @param limits
     * @param compare
     * @return
     */
    private Limit findLimit(List<Limit> limits, Limit compare) {
        for (Limit limit : limits) {
            if (limit != null && compare.getTypeName().equals(limit.getTypeName())) {
                return limit;
            }
        }

        return null;
    }

}
