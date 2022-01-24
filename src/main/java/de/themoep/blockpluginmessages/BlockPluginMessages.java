package de.themoep.blockpluginmessages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/*
 * BlockPluginMessages
 * Copyright (C) 2022 Max Lee aka Phoenix616 (max@themoep.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class BlockPluginMessages extends Plugin implements Listener {

    private YamlConfig config;

    private Set<String> serverWhitelist = new HashSet<String>();
    private Map<String, Set<String>> serverChannels = new HashMap<>();
    private Mode serverMode = Mode.WHITELIST;

    private Map<String, Set<String>> playerChannels = new HashMap<>();
    private Mode playerMode = Mode.WHITELIST;

    public void onEnable() {
        try {
            config = new YamlConfig(this, getDataFolder() + File.separator + "config.yml");
        } catch (IOException e) {
            getLogger().severe("Unable to load configuration! " + getDescription().getName() + " will not be enabled!");
            e.printStackTrace();
        }

        getLogger().info("Loading servers config...");
        for (String name : getConfig().getStringList("servers.whitelist")) {
            serverWhitelist.add(name);
            getLogger().info("Added server " + name + " to whitelist!");
        }

        serverMode = parseMode(getConfig().getString("servers.channels.mode"));
        loadConfig(getConfig().getSection("servers"), serverMode, serverChannels);

        getLogger().info("Loading players config...");
        playerMode = parseMode(getConfig().getString("players.channels.mode"));
        loadConfig(getConfig().getSection("players"), playerMode, playerChannels);

        getProxy().getPluginManager().registerListener(this, this);
    }

    private void loadConfig(Configuration config, Mode mode, Map<String, Set<String>> channels) {
        for (String channel : channels.keySet()) {
            getProxy().unregisterChannel(channel);
        }
        channels.clear();

        if (config != null) {
            Configuration channelsConfig = config.getSection("channels." + mode.getKey());
            if (channelsConfig != null) {
                for (String channel : channelsConfig.getKeys()) {
                    if (!channels.containsKey(channel.toLowerCase())) {
                        channels.put(channel.toLowerCase(), new HashSet<>());
                    }
                    List<String> subChannels = new ArrayList<>();
                    for (String subChannel : channelsConfig.getStringList(channel)) {
                        subChannels.add(subChannel.toLowerCase());
                    }
                    channels.get(channel.toLowerCase()).addAll(subChannels);
                    getLogger().info("Channel " + channel + " now " + mode.getKey() + "s " + (channels.get(channel.toLowerCase()).size() == 0 ? "all subchannels!" : String.join(", ", channelsConfig.getStringList(channel))));
                }
            } else if (mode == Mode.BLACKLIST) {
                getLogger().warning("You don't have any server channels defined!");
            }

            // Not sure if this is really needed but register anyways
            for (String channel : channels.keySet()) {
                getProxy().registerChannel(channel);
            }
        }
    }

    private Mode parseMode(String string) {
        try {
            return Mode.valueOf(string.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            getLogger().log(Level.WARNING, "Invalid mode '" + string + "' detected in config.yml. Using 'whitelist'!");
            return Mode.WHITELIST;
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPluginMessageReceive(PluginMessageEvent event) {
        if (event.getSender() instanceof Server) {
            if (serverWhitelist.contains(((Server) event.getSender()).getInfo().getName())) {
                return;
            }

            handlePluginMessage(event, serverMode, serverChannels, "server " + ((Server) event.getSender()).getInfo().getName());
        } else if (event.getSender() instanceof ProxiedPlayer) {
            if (((ProxiedPlayer) event.getSender()).hasPermission("blockpluginmessages.bypass")
                    || ((ProxiedPlayer) event.getSender()).hasPermission("blockpluginmessages.bypass." + event.getTag().toUpperCase(Locale.ROOT))) {
                return;
            }

            handlePluginMessage(event, playerMode, playerChannels, "player " + ((ProxiedPlayer) event.getSender()).getName());
        }
    }

    private void handlePluginMessage(PluginMessageEvent event, Mode mode, Map<String, Set<String>> channels, String sender) {
        if (channels.containsKey(event.getTag().toLowerCase(Locale.ROOT))) {
            boolean matches = false;
            Set<String> subChannels = channels.get(event.getTag().toLowerCase(Locale.ROOT));
            String channelDescription = event.getTag();
            if (subChannels.size() == 0 || subChannels.contains("!all")) {
                matches = true;
            } else {
                ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
                try {
                    String subChannel = in.readUTF();

                    List<String> args = new ArrayList<>();
                    do {
                        try {
                            args.add(in.readUTF());
                        } catch (IllegalStateException e) {
                            // Nothing more to read
                            break;
                        }
                    } while (true);

                    channelDescription += " - " + subChannel + (args.isEmpty() ? "": " (Data: " + String.join(", " + args));
                    if (subChannels.contains(subChannel.toLowerCase())) {
                        matches = true;
                    }
                } catch (IllegalStateException e) {
                    channelDescription += " - ''";
                    // No sub channel could be read, check for empty string
                    if (subChannels.contains("")) {
                        matches = true;
                    }
                }
            }
            switch (mode) {
                case BLACKLIST:
                    if (matches) {
                        event.setCancelled(true);
                        getLogger().info("Blocked plugin message on channel " + channelDescription + " from" + sender + " as it was on the blacklist");
                    }
                    break;
                case WHITELIST:
                    if (!matches) {
                        event.setCancelled(true);
                        getLogger().info("Blocked plugin message on channel " + channelDescription + " from " + sender + " as it wasn't on the whitelist");
                    }
                    break;
            }
        }
    }

    public YamlConfig getConfig() {
        return config;
    }

    private enum Mode {
        WHITELIST,
        BLACKLIST;

        public String getKey() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
