/*
 * This file is part of TabTPS, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 Jason Penilla
 * Copyright (c) 2026 MCbabel (Folia support)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package xyz.jpenilla.tabtps.folia;

import java.nio.file.Path;
import java.util.Objects;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jpenilla.tabtps.common.TabTPS;
import xyz.jpenilla.tabtps.common.TabTPSPlatform;
import xyz.jpenilla.tabtps.common.command.Commander;
import xyz.jpenilla.tabtps.common.command.commands.TickInfoCommand;
import xyz.jpenilla.tabtps.common.service.RegionStatsService;
import xyz.jpenilla.tabtps.common.service.TickTimeService;
import xyz.jpenilla.tabtps.common.service.UserService;
import xyz.jpenilla.tabtps.common.util.UpdateChecker;
import xyz.jpenilla.tabtps.folia.command.FoliaConsoleCommander;
import xyz.jpenilla.tabtps.folia.command.FoliaPingCommand;
import xyz.jpenilla.tabtps.folia.command.FoliaTickInfoCommandFormatter;
import xyz.jpenilla.tabtps.folia.service.FoliaRegionStatsService;
import xyz.jpenilla.tabtps.folia.service.FoliaTickTimeService;
import xyz.jpenilla.tabtps.folia.service.FoliaUserService;

public final class TabTPSFolia extends JavaPlugin implements TabTPSPlatform<Player, FoliaUser> {
  private TabTPS tabTPS;
  private LegacyPaperCommandManager<Commander> commandManager;
  private UserService<Player, FoliaUser> userService;
  private FoliaTickTimeService tickTimeService;
  private FoliaRegionStatsService regionStatsService;
  private Logger logger;
  private BukkitAudiences audiences;

  @Override
  public void onEnable() {
    this.logger = LoggerFactory.getLogger(this.getLogger().getName());
    this.audiences = BukkitAudiences.create(this);
    this.tickTimeService = new FoliaTickTimeService();
    this.regionStatsService = new FoliaRegionStatsService(this.tickTimeService);

    this.setupCommandManager();
    this.userService = new FoliaUserService(this);

    this.tabTPS = new TabTPS(this);
    this.registerCommands();

    this.getServer().getPluginManager().registerEvents(new FoliaJoinQuitListener(this), this);

    if (this.tabTPS.configManager().pluginSettings().updateChecker()) {
      this.getServer().getAsyncScheduler().runNow(this, task ->
        UpdateChecker.checkVersion(this.getDescription().getVersion()).forEach(this.logger::info)
      );
    }
    final Metrics metrics = new Metrics(this, 8458);
  }

  @Override
  public void onDisable() {
    if (this.tabTPS != null) {
      this.tabTPS.shutdown();
    }
    if (this.audiences != null) {
      this.audiences.close();
      this.audiences = null;
    }
  }

  @Override
  public void shutdown() {
    this.getServer().getPluginManager().disablePlugin(this);
  }

  @Override
  public void onReload() {
    this.getServer().getOnlinePlayers().forEach(Player::updateCommands);
  }

  public BukkitAudiences audiences() {
    return Objects.requireNonNull(this.audiences, "BukkitAudiences");
  }

  private void setupCommandManager() {
    this.commandManager = new LegacyPaperCommandManager<>(
      this,
      ExecutionCoordinator.simpleCoordinator(),
      SenderMapper.create(
        commandSender -> {
          if (commandSender instanceof Player) {
            return this.userService().user((Player) commandSender);
          }
          return FoliaConsoleCommander.from(this.audiences, commandSender);
        },
        commander -> {
          if (commander instanceof FoliaConsoleCommander) {
            return ((FoliaConsoleCommander) commander).commandSender();
          } else if (commander instanceof FoliaUser) {
            return ((FoliaUser) commander).base();
          }
          throw new IllegalArgumentException();
        }
      )
    );

    if (this.commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
      this.commandManager.registerBrigadier();
    } else if (this.commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
      this.commandManager.registerAsynchronousCompletions();
    }
  }

  private void registerCommands() {
    TickInfoCommand.withFormatter(this.tabTPS, this.tabTPS.commands(), new FoliaTickInfoCommandFormatter(this.regionStatsService)).register();
    new FoliaPingCommand(this, this.tabTPS.commands()).register();
  }

  @Override
  public @NonNull CommandManager<Commander> commandManager() {
    return this.commandManager;
  }

  @Override
  public @NonNull UserService<Player, FoliaUser> userService() {
    return this.userService;
  }

  @Override
  public @NonNull Path dataDirectory() {
    return this.getDataFolder().toPath();
  }

  @Override
  public @NonNull TabTPS tabTPS() {
    return this.tabTPS;
  }

  @Override
  public @NonNull TickTimeService tickTimeService() {
    return this.tickTimeService;
  }

  @Override
  public @Nullable RegionStatsService regionStatsService() {
    return this.regionStatsService;
  }

  @Override
  public int maxPlayers() {
    return this.getServer().getMaxPlayers();
  }

  @Override
  public @NonNull Logger logger() {
    return this.logger;
  }
}
