package de.themoep.blockpluginmessages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

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

    public void onEnable() {
        try {
            config = new YamlConfig(this, getDataFolder() + File.separator + "config.yml");
        } catch (IOException e) {
            getLogger().severe("Unable to load configuration! " + getDescription().getName() + " will not be enabled!");
            e.printStackTrace();
        }

        for (String name : getConfig().getStringList("servers.whitelist")) {
            serverWhitelist.add(name);
            getLogger().info("Added server " + name + " to whitelist!");
        }

        serverMode = parseMode(getConfig().getString("servers.channels.mode"));

        Configuration serverChannelsConfig = getConfig().getSection("servers.channels." + serverMode);
        if (serverChannelsConfig != null) {
            for (String channel : serverChannelsConfig.getKeys()) {
                if (!serverChannels.containsKey(channel.toLowerCase())) {
                    serverChannels.put(channel.toLowerCase(), new HashSet<>());
                }
                List<String> subChannels = new ArrayList<>();
                for (String subChannel : serverChannelsConfig.getStringList(channel)) {
                    subChannels.add(subChannel.toLowerCase());
                }
                serverChannels.get(channel.toLowerCase()).addAll(subChannels);
                getLogger().info("Channel " + channel + " now " + serverMode.getKey() + "s " + (serverChannels.get(channel.toLowerCase()).size() == 0 ? "all subchannels!" : String.join(", ", serverChannelsConfig.getStringList(channel))));
            }
        } else if (serverMode == Mode.BLACKLIST) {
            getLogger().warning("You don't have any server channels defined!");
        }

        // Not sure if this is really needed
        for (String channel : this.serverChannels.keySet()) {
            getProxy().registerChannel(channel);
        }

        getProxy().getPluginManager().registerListener(this, this);
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
            if (serverChannels.containsKey(event.getTag().toLowerCase())) {
                boolean matches = false;
                Set<String> subChannels = serverChannels.get(event.getTag().toLowerCase());
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
                switch (serverMode) {
                    case BLACKLIST:
                        if (matches) {
                            event.setCancelled(true);
                            getLogger().info("Blocked plugin message on channel " + channelDescription + " from server " + ((Server) event.getSender()).getInfo().getName() + " as it was on the blacklsit");
                        }
                        break;
                    case WHITELIST:
                        if (!matches) {
                            event.setCancelled(true);
                            getLogger().info("Blocked plugin message on channel " + channelDescription + " from server " + ((Server) event.getSender()).getInfo().getName() + " as it wasn't on the whitelist");
                        }
                        break;
                }
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
