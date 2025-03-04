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
package com.griefcraft.util;

import com.griefcraft.cache.MethodCounter;
import com.griefcraft.cache.ProtectionCache;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCInfo;
import com.griefcraft.scripting.MetaData;
import com.griefcraft.sql.Database;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.Plugin;

import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;

public class Statistics {

    /**
     * Number of queries executed on the database
     */
    private static int queries = 0;

    /**
     * Time when LWC was started
     */
    private static long startTime = 0L;

    /**
     * Add a query
     */
    public static void addQuery() {
        queries++;
    }

    /**
     * @return the average of a value
     */
    public static double getAverage(long value) {
        return (double) value / getTimeRunningSeconds();
    }

    /**
     * Obtain an entity count of a Entity class.
     *
     * @param clazz If null, all entities are counted
     * @return
     */
    public static int getEntityCount(Class<? extends Entity> clazz) {
        int count = 0;

        for (World world : Bukkit.getServer().getWorlds()) {
            if (clazz == null) {
                count += world.getEntities().size();
            } else {
                for (Entity entity : world.getEntities()) {
                    if (entity != null && clazz.isInstance(entity)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * Send a performance report to a Console Sender
     *
     * @param sender
     */
    public static void sendReport(CommandSender sender) {
        LWC lwc = LWC.getInstance();

        sender.sendMessage(" ");
        sender.sendMessage(ChatColor.DARK_RED + "LWC Report");
        sender.sendMessage("  Version: " + ChatColor.DARK_GREEN + LWCInfo.FULL_VERSION);
        sender.sendMessage("  Running time: " + ChatColor.DARK_GREEN + TimeUtil.timeToString(getTimeRunningSeconds()));
        sender.sendMessage("  Players: " + ChatColor.DARK_GREEN + Bukkit.getServer().getOnlinePlayers().size() + "/" + Bukkit.getServer().getMaxPlayers());
        sender.sendMessage("  Item entities: " + ChatColor.DARK_GREEN + getEntityCount(Item.class) + "/" + getEntityCount(null));
        sender.sendMessage("  Permissions API: " + ChatColor.DARK_GREEN + lwc.getPermissions().getClass().getSimpleName());
        sender.sendMessage("  Currency API: " + ChatColor.DARK_GREEN + lwc.getCurrency().getClass().getSimpleName());
        sender.sendMessage(" ");
        sender.sendMessage(ChatColor.DARK_RED + " ==== Modules ====");

        for (Map.Entry<Plugin, List<MetaData>> entry : lwc.getModuleLoader().getRegisteredModules().entrySet()) {
            Plugin plugin = entry.getKey();
            List<MetaData> modules = entry.getValue();

            // Why?
            if (plugin == null) {
                continue;
            }

            sender.sendMessage("  " + ChatColor.DARK_GREEN + plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion() + ChatColor.YELLOW + " -> " + ChatColor.DARK_GREEN + modules.size() + ChatColor.YELLOW + " registered modules");
        }
        sender.sendMessage(" ");

        sender.sendMessage(ChatColor.DARK_RED + " ==== Database ====");
        sender.sendMessage("  Engine: " + ChatColor.DARK_GREEN + Database.DefaultType);
        sender.sendMessage("  Protections: " + ChatColor.DARK_GREEN + formatNumber(lwc.getPhysicalDatabase().getProtectionCount()));
        sender.sendMessage("  Queries: " + ChatColor.DARK_GREEN + formatNumber(queries) + " | " + String.format("%.2f", getAverage(queries)) + " / second");
        sender.sendMessage(" ");

        sender.sendMessage(ChatColor.DARK_RED + " ==== Cache ==== ");

        ProtectionCache cache = lwc.getProtectionCache();

        double cachePercentFilled = ((double) cache.size() / cache.totalCapacity()) * 100;

        String cacheColour = ChatColor.DARK_GREEN.toString();
        if (cachePercentFilled > 75 && cachePercentFilled < 85) {
            cacheColour = ChatColor.YELLOW.toString();
        } else if (cachePercentFilled > 85 && cachePercentFilled < 95) {
            cacheColour = ChatColor.RED.toString();
        } else if (cachePercentFilled > 95) {
            cacheColour = ChatColor.DARK_RED.toString();
        }

        sender.sendMessage("  Usage: " + cacheColour + String.format("%.2f", cachePercentFilled) + "% " + ChatColor.WHITE + " ( " + cache.size() + "/" + cache.totalCapacity() + " [" + cache.capacity() + "+" + cache.adaptiveCapacity() + "] )");
        sender.sendMessage("  Profile: ");
        sendMethodCounter(sender, cache.getMethodCounter());
        // sender.sendMessage("  Reads: " + formatNumber(cache.getReads()) + " | " + String.format("%.2f", getAverage(cache.getReads())) + " / second");
        // sender.sendMessage("  Writes: " + formatNumber(cache.getWrites()) + " | " + String.format("%.2f", getAverage(cache.getWrites())) + " / second");
    }

    private static void sendMethodCounter(CommandSender sender, MethodCounter counter) {
        Map<String, Integer> sorted = counter.sortByValue();

        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            String method = entry.getKey();
            int count = entry.getValue();

            sender.sendMessage("    " + method + ": " + formatNumber(count) + " (" + String.format("%.2f", getAverage(count)) + " / second)");
        }

    }

    /**
     * Format a number
     *
     * @param number
     * @return
     */
    public static String formatNumber(long number) {
        return NumberFormat.getInstance().format(number);
    }

    /**
     * @return time in seconds it was being ran
     */
    public static int getTimeRunningSeconds() {
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }

    /**
     * Initialize
     */
    public static void init() {
        startTime = System.currentTimeMillis();
    }

}
