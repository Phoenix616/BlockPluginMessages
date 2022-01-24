# BlockPluginMessages

Bungee plugin to block certain plugin messages sent by servers or players.

Supports both a whitelist as well as a blacklist mode and (one level of)
 sub channel matching e.g. for the inbuilt BungeeCord channel.

## Config

```yaml
servers:
  # Servers that are allowed everything
  whitelist:
  - server1
  - server2
  channels:
    mode: whitelist
    # Channels to allow in whitelist mode
    whitelist:
      # List of allowed subchannels, use an empty list [] to allow all
      "someplugin:channel": []
    # Channels to block in blacklist mode
    blacklist:
      # List of blocked subchannels, use an empty list [] to block all
      BungeeCord:
      - Forward
      - ServerIP
      - Connect
      - ConnectOther
      "plugin2:channel": []
players:
  # Player bypass permission: blockpluginmessages.bypass[.<channel>]
  channels:
    mode: whitelist
    # Channels to allow in whitelist mode
    whitelist:
      # List of allowed subchannels, use an empty list [] to allow all
      # World name packets used by VoxelMap, Xaero's Map, JourneyMap and Rei's Minimap
      "worldinfo:world_id": []
      "xaeroworldmap:main": []
    # Channels to block in blacklist mode
    blacklist:
      # List of blocked subchannels, use an empty list [] to block all
      BungeeCord: [] # Technically the bungee blocks this itself already
      "another:channel": []
```

## Download

Builds of the plugin are available on the [Minebench.de Jenkins](https://ci.minebench.de/job/BlockPluginMessages/).

## License

This plugin is licensed under the GPLv3.

```
BlockPluginMessages
Copyright (C) 2022 Max Lee aka Phoenix616 (max@themoep.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```