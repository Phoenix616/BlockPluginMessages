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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BlockPluginMessages
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class BlockPluginMessages extends Plugin implements Listener {

    private YamlConfig config;

    private Set<String> whitelist = new HashSet<String>();
    private Map<String, Set<String>> blocked = new HashMap<String, Set<String>>();

    public void onEnable() {
        try {
            config = new YamlConfig(this, getDataFolder() + File.separator + "config.yml");
        } catch (IOException e) {
            getLogger().severe("Unable to load configuration! " + getDescription().getName() + " will not be enabled!");
            e.printStackTrace();
        }

        for(String name : getConfig().getStringList("serverwhitelist")) {
            whitelist.add(name);
            getLogger().info("Added " + name + " to whitelist!");
        }

        Configuration blockedChannels = getConfig().getSection("blockedchannels");
        if(blockedChannels != null) {
            for(String channel : blockedChannels.getKeys()) {
                if(!blocked.containsKey(channel.toLowerCase())) {
                    blocked.put(channel.toLowerCase(), new HashSet<String>());
                }
                List<String> subChannels = new ArrayList<String>();
                for(String subChannel : blockedChannels.getStringList(channel)) {
                    subChannels.add(subChannel.toLowerCase());
                }
                blocked.get(channel.toLowerCase()).addAll(subChannels);
                getLogger().info("Channel " + channel + " now blocks " + (blocked.get(channel.toLowerCase()).size() == 0 ? "all subchannels!" : join(blockedChannels.getStringList(channel), ", ")));
            }
        } else {
            getLogger().warning("You don't have any blocked channels define! This plugin will do nothing then!");
        }

        // Not sure if this is really needed
        for(String channel : blocked.keySet()) {
            getProxy().registerChannel(channel);
        }

        getProxy().getPluginManager().registerListener(this, this);
    }

    private String join(Collection<String> set, String delimiter) {
        List<String> stringList = new ArrayList<String>(set);
        if(stringList.size() > 0) {
            String r = stringList.get(0);
            for(int i = 1; i < set.size(); i++) {
                r += delimiter + stringList.get(i);
            }
            return r;
        }
        return "";
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPluginMessageReceive(PluginMessageEvent event) {
        if(event.getSender() instanceof Server) {
            if(whitelist.contains(((Server) event.getSender()).getInfo().getName())) {
                return;
            }
            if(blocked.containsKey(event.getTag().toLowerCase())) {
                Set<String> subChannels = blocked.get(event.getTag().toLowerCase());
                if(subChannels.size() == 0 || subChannels.contains("!all")) {
                    getLogger().info("Blocked plugin message on channel " + event.getTag() + " from server " + ((Server) event.getSender()).getInfo().getName());
                    event.setCancelled(true);
                    return;
                }

                ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
                String subchannel = in.readUTF();

                List<String> args = new ArrayList<String>();
                try {
                    String data = in.readUTF();
                    do {
                        args.add(data);
                        try {
                            data = in.readUTF();
                        } catch(IllegalStateException e) {
                            break;
                        }
                    } while(data != null);

                } catch(IllegalStateException e) {}
                if(subChannels.contains(subchannel.toLowerCase())) {
                    getLogger().info("Blocked plugin message on channel " + event.getTag() + " - " + subchannel + " from server " + ((Server) event.getSender()).getInfo().getName() + " (Data: " + join(args, " | ") + ")");
                    event.setCancelled(true);
                }
            }
        }
    }

    public YamlConfig getConfig() {
        return config;
    }
}
