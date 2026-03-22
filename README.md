<img src="https://i.imgur.com/EYo9tBh.png" width="128">

# TabTPS-Folia

A fork of [TabTPS by jpenilla](https://github.com/jpenilla/TabTPS) with **Folia support**.

Shows TPS, MSPT, and other server information in the tab menu, boss bar, and action bar — with **per-region tick data** on Folia servers.

## Platform

- [Folia](https://papermc.io/software/folia) (Minecraft 1.20+, Java 21)

## Features

### Regional TPS/MSPT

On Folia, each player sees the TPS and MSPT of **their current region**, not a global average. When a player moves between regions, the displayed values update automatically.

### Additional Folia Modules

These modules can be used in display configs to show server-wide region statistics:

| Module | Description |
|---|---|
| `tps` | TPS of the player's current region |
| `mspt` | MSPT of the player's current region |
| `lowest_region_tps` | Lowest TPS across all active regions |
| `median_region_tps` | Median TPS across all active regions |
| `highest_region_tps` | Highest TPS across all active regions |
| `ping` | Player ping |
| `cpu` | CPU usage |
| `memory` | Memory usage |
| `players` | Online player count |

### Live Information Displays

#### Tab menu
* Command: ``/tabtps toggle tab``
* ![tab menu](https://i.imgur.com/93NmuUA.png)

#### Action bar
* Command: ``/tabtps toggle actionbar``
* ![action bar](https://i.imgur.com/aMzzNRR.png)

#### Boss bar
* Command: ``/tabtps toggle bossbar``
* ![boss bar](https://i.postimg.cc/xCJnGYfb/bossbar.png)

Configure display modules and themes in `plugins/TabTPS/display-configs/` and `plugins/TabTPS/themes/`.

### Commands

#### Tick Info command
* Command: ``/tickinfo`` or ``/mspt``
* Shows region TPS overview (Lowest/Median/Highest)
* Permission required: ``tabtps.tps``
* ![tps command](https://i.imgur.com/w69C8r1.png)

#### Memory command
* Command: ``/memory``, `/mem`, or ``/ram``
* Permission required: ``tabtps.tps``
* ![memory command](https://i.imgur.com/eYeUNMc.png)

#### Ping command
* Commands: ``/ping``, `/ping [username]`, or ``/pingall``
* Permissions: ``tabtps.ping`` to view your own ping, ``tabtps.ping.others`` to view other users ping.
* ![ping command](https://i.imgur.com/0agY7lB.png)
* ![ping all](https://i.imgur.com/t1lBt2b.png)

#### Reload command
* Command: ``/tabtps reload``
* Permission required: ``tabtps.reload``

## Building

```bash
./gradlew :tabtps-folia:shadowJar
```

The built JAR will be at `folia/build/libs/tabtps-folia-*.jar`.

## Credits

- Original [TabTPS](https://github.com/jpenilla/TabTPS) by [jpenilla](https://github.com/jpenilla) (MIT License)
- Folia support by [MCbabel](https://github.com/MCbabel)

## License

[MIT License](license.txt) — Copyright (c) 2020-2024 Jason Penilla, Copyright (c) 2026 MCbabel
