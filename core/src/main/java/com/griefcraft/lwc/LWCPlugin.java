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
package com.griefcraft.lwc;

import com.griefcraft.listeners.LWCBlockListener;
import com.griefcraft.listeners.LWCEntityListener;
import com.griefcraft.listeners.LWCPlayerListener;
import com.griefcraft.listeners.LWCServerListener;
import com.griefcraft.scripting.event.LWCCommandEvent;
import com.griefcraft.sql.Database;
import com.griefcraft.util.Completions;
import com.griefcraft.util.StringUtil;
import com.griefcraft.util.locale.LWCResourceBundle;
import com.griefcraft.util.locale.LocaleClassLoader;
import com.griefcraft.util.locale.UTF8Control;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.jar.JarFile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LWCPlugin extends JavaPlugin {

    /**
     * The LWC instance
     */
    private LWC lwc;

    /**
     * The message parser to parse messages with
     */
    private MessageParser messageParser;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        String commandName = command.getName().toLowerCase();
        String argString = StringUtil.join(args, 0);
        boolean isPlayer = (sender instanceof Player); // check if they're a player

        // these can only apply to players, not the console (who has absolute player :P)
        if (isPlayer) {
            // Aliases
            String aliasCommand = null;
            String[] aliasArgs = new String[0];

            if (commandName.equals("cpublic")) {
                aliasCommand = "create";
                aliasArgs = new String[]{"public"};
            } else if (commandName.equals("cpassword")) {
                aliasCommand = "create";
                aliasArgs = ("password " + argString).split(" ");
            } else if (commandName.equals("cprivate") || commandName.equals("lock")) {
                aliasCommand = "create";
                aliasArgs = ("private " + argString).split(" ");
            } else if (commandName.equals("cdonation")) {
                aliasCommand = "create";
                aliasArgs = ("donation " + argString).split(" ");
            } else if (commandName.equals("cdisplay")) {
                aliasCommand = "create";
                aliasArgs = ("display " + argString).split(" ");
            } else if (commandName.equals("csupply")) {
                aliasCommand = "create";
                aliasArgs = ("supply " + argString).split(" ");
            } else if (commandName.equals("cmodify")) {
                aliasCommand = "modify";
                aliasArgs = argString.isEmpty() ? new String[0] : argString.split(" ");
            } else if (commandName.equals("cinfo")) {
                aliasCommand = "info";
            } else if (commandName.equals("cunlock")) {
                aliasCommand = "unlock";
                aliasArgs = argString.isEmpty() ? new String[0] : argString.split(" ");
            } else if (commandName.equals("cremove") || commandName.equals("unlock")) {
                aliasCommand = "remove";
                aliasArgs = new String[]{"protection"};
            } else if (commandName.equals("climits")) {
                aliasCommand = "limits";
                aliasArgs = argString.isEmpty() ? new String[0] : argString.split(" ");
            } else if (commandName.equals("cadmin")) {
                aliasCommand = "admin";
                aliasArgs = argString.isEmpty() ? new String[0] : argString.split(" ");
            } else if (commandName.equals("cremoveall")) {
                aliasCommand = "remove";
                aliasArgs = new String[]{"allprotections"};
            }

            // Flag aliases
            if (commandName.equals("credstone")) {
                aliasCommand = "flag";
                aliasArgs = ("redstone " + argString).split(" ");
            } else if (commandName.equals("cmagnet")) {
                aliasCommand = "flag";
                aliasArgs = ("magnet " + argString).split(" ");
            } else if (commandName.equals("cexempt")) {
                aliasCommand = "flag";
                aliasArgs = ("exemption " + argString).split(" ");
            } else if (commandName.equals("cautoclose")) {
                aliasCommand = "flag";
                aliasArgs = ("autoclose " + argString).split(" ");
            } else if (commandName.equals("callowexplosions") || commandName.equals("ctnt")) {
                aliasCommand = "flag";
                aliasArgs = ("allowexplosions " + argString).split(" ");
            } else if (commandName.equals("chopper")) {
                aliasCommand = "flag";
                aliasArgs = ("hopper " + argString).split(" ");
            }

            // Mode aliases
            if (commandName.equals("cdroptransfer")) {
                aliasCommand = "mode";
                aliasArgs = ("droptransfer " + argString).split(" ");
            } else if (commandName.equals("cpersist")) {
                aliasCommand = "mode";
                aliasArgs = ("persist " + argString).split(" ");
            } else if (commandName.equals("cnospam")) {
                aliasCommand = "mode";
                aliasArgs = ("nospam " + argString).split(" ");
            }

            if (aliasCommand != null) {
                lwc.getModuleLoader().dispatchEvent(new LWCCommandEvent(sender, aliasCommand, aliasArgs));
                return true;
            }
        }

        if (args.length == 0) {
            lwc.sendFullHelp(sender);
            return true;
        }

        ///// Dispatch command to modules
        LWCCommandEvent evt = new LWCCommandEvent(sender, args[0].toLowerCase(), args.length > 1 ? StringUtil.join(args, 1).split(" ") : new String[0]);
        lwc.getModuleLoader().dispatchEvent(evt);

        if (evt.isCancelled()) {
            return true;
        }

        if (!isPlayer) {
            lwc.sendLocale(sender, "lwc.commandnotsupported");
            return true;
        }

        // Prevent Bukkit from handling the error which gives a non-descript "/lwc"
        lwc.sendLocale(sender, "lwc.invalidcommand");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String currentArg = args[args.length - 1];
        switch (label) {
            case "lwc":
                if (args.length >= 1) {
                    switch (args[0].toLowerCase()) {
                        case "create":
                            if (args.length == 2) {
                                return Completions.protectionTypes(currentArg);
                            } else if (args.length > 2 && ("public".equals(args[1].toLowerCase()) || "password".equals(args[1].toLowerCase()))) {
                                break;
                            }
                            return Completions.cmodify(currentArg, sender, false);
                        case "modify":
                        case "default":
                            if (args.length >= 2 && Completions.protectionTypes().contains(args[1].toLowerCase())) {
                                break;
                            }
                            return Completions.cmodify(currentArg, sender, args.length == 2);
                        case "limits":
                            if (lwc.isAdmin(sender)) {
                                return Completions.players(currentArg, sender);
                            }
                            break;
                        case "remove":
                            return Completions.remove(currentArg);
                        case "mode":
                            if (args.length == 2) {
                                return Completions.modes(currentArg);
                            } else if (args.length == 3 && "droptransfer".equals(args[1].toLowerCase())) {
                                return Completions.droptransfer(currentArg);
                            }
                            break;
                        case "flag":
                            if (args.length == 2) {
                                return Completions.flags(currentArg);
                            } else if (args.length == 3) {
                                return Completions.toggles(currentArg);
                            }
                            break;
                        case "admin":
                            if (lwc.isAdmin(sender)) {
                                if (args.length == 2) {
                                    return Completions.admin(currentArg);
                                } else if (args.length > 2) {
                                    return onTabCompleteAdmin(sender, Arrays.copyOfRange(args, 1, args.length));
                                }
                            }
                            break;
                        default:
                            if (args.length == 1) {
                                return Completions.lwc(currentArg);
                            }
                            break;
                    }
                }
                break;
            case "lock":
                return Completions.cmodify(currentArg, sender, false);
            case "cadmin":
                if (lwc.isAdmin(sender)) {
                    if (args.length == 1) {
                        return Completions.admin(currentArg);
                    }
                    return onTabCompleteAdmin(sender, args);
                }
                break;
            case "cmodify":
            case "cdefault":
                if (args.length >= 1 && Completions.protectionTypes().contains(args[0].toLowerCase())) {
                    break;
                }
                return Completions.cmodify(currentArg, sender, args.length == 1);
            case "climits":
                if (lwc.isAdmin(sender)) {
                    return Completions.players(currentArg, sender);
                }
                break;
            case "cdroptransfer":
                if (args.length == 1) {
                    return Completions.droptransfer(currentArg);
                }
                break;
            case "credstone":
            case "cmagnet":
            case "cexempt":
            case "cautoclose":
            case "callowexplosions":
            case "chopper":
            case "ctnt":
                if (args.length == 1) {
                    return Completions.toggles(currentArg);
                }
                break;
            default:
                break;
        }
        return Collections.emptyList();
    }

    private List<String> onTabCompleteAdmin(CommandSender sender, String[] args) {
        String currentArg = args[args.length - 1];
        if (args.length >= 1) {
            switch (args[0].toLowerCase()) {
                case "view":
                    if (args.length == 2) {
                        return Completions.integers(currentArg);
                    }
                    break;
                case "find":
                case "forceowner":
                    if (args.length == 2) {
                        return Completions.players(currentArg, sender);
                    } else if (args.length == 3) {
                        return Completions.integers(currentArg);
                    }
                    break;
                case "remove":
                    if (args.length == 2) {
                        return Completions.integers(currentArg);
                    }
                    break;
                case "purge":
                    return Completions.players(currentArg, sender);
                default:
                    break;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void onDisable() {
        LWC.ENABLED = false;

        if (lwc != null) {
            lwc.destruct();
        }

        // cancel all tasks we created
        getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public void onEnable() {
        // preload();
        lwc = new LWC(this);

        LWCInfo.setVersion(getDescription().getVersion());
        LWC.ENABLED = true;

        loadLocales();
        loadDatabase();

        // Load the rest of LWC
        lwc.load();

        if (this.isEnabled()) {
            try {
                registerEvents();
            } catch (NoSuchFieldError e) {
            }
        }
    }

    /**
     * Load the database
     */
    public void loadDatabase() {
        String database = lwc.getConfiguration().getString("database.adapter");

        if (database.equalsIgnoreCase("mysql")) {
            Database.DefaultType = Database.Type.MySQL;
        } else {
            Database.DefaultType = Database.Type.SQLite;
        }
    }

    /**
     * Load LWC localizations
     */
    public void loadLocales() {
        LWCResourceBundle locale;
        String localization = getCurrentLocale();

        // located in plugins/LWC/locale/, values in that overrides the ones in the default :-)
        ResourceBundle optionalBundle = null;

        // Open the LWC jar file
        try (JarFile file = new JarFile(getFile())) {
            ResourceBundle defaultBundle;

            // Attempt to load the default locale
            defaultBundle = new PropertyResourceBundle(new InputStreamReader(file.getInputStream(file.getJarEntry("lang/lwc_en.properties")), "UTF-8"));
            locale = new LWCResourceBundle(defaultBundle);

            try {
                optionalBundle = ResourceBundle.getBundle("lwc", new Locale(localization), new LocaleClassLoader(), new UTF8Control());
            } catch (MissingResourceException e) {
            }

            if (optionalBundle != null) {
                locale.addExtensionBundle(optionalBundle);
            }

            // and now check if a bundled locale the same as the server's locale exists
            try {
                optionalBundle = new PropertyResourceBundle(new InputStreamReader(file.getInputStream(file.getJarEntry("lang/lwc_" + localization + ".properties")), "UTF-8"));
            } catch (MissingResourceException e) {
            } catch (NullPointerException e) {
                // file wasn't found :p - that's ok
            }

            // ensure both bundles aren't the same
            if (defaultBundle == optionalBundle) {
                optionalBundle = null;
            }

            if (optionalBundle != null) {
                locale.addExtensionBundle(optionalBundle);
            }
        } catch (MissingResourceException e) {
            log("We are missing the default locale in LWC.jar.. What happened to it? :-(");
            throw e;
        } catch (IOException e) {
            log("Uh-oh: " + e.getMessage());
            return;
        }

        // create the message parser
        messageParser = new SimpleMessageParser(locale);
    }

    /**
     * Load shared libraries and other misc things
     */
    /* private void preload() {
        updater = new Updater();

        // Set the SQLite native library path
        String nativeLibraryFolder = updater.getOSSpecificFolder();

        if (nativeLibraryFolder != null) {
            System.setProperty("org.sqlite.lib.path", nativeLibraryFolder);
        }
    } */
    /**
     * Log a string to the console
     *
     * @param str
     */
    private void log(String str) {
        getLogger().info(str);
    }

    /**
     * Register all of the events used by LWC
     */
    private void registerEvents() {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        pluginManager.registerEvents(new LWCPlayerListener(this), this);
        pluginManager.registerEvents(new LWCEntityListener(this), this);
        pluginManager.registerEvents(new LWCBlockListener(this), this);
        pluginManager.registerEvents(new LWCServerListener(this), this);
    }

    /**
     * @return the current locale in use
     */
    public String getCurrentLocale() {
        return lwc.getConfiguration().getString("core.locale", "en");
    }

    /**
     * @return the LWC instance
     */
    public LWC getLWC() {
        return lwc;
    }

    /**
     * Gets the message parser
     *
     * @return
     */
    public MessageParser getMessageParser() {
        return messageParser;
    }

    /**
     * @return the Updater instance
     */
    /* public Updater getUpdater() {
        return updater;
    } */
    @Override
    public File getFile() {
        return super.getFile();
    }

}
