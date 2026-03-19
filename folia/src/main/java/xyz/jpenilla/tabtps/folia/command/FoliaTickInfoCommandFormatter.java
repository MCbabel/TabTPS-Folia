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
package xyz.jpenilla.tabtps.folia.command;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.tabtps.common.command.commands.TickInfoCommand;
import xyz.jpenilla.tabtps.common.config.Theme;
import xyz.jpenilla.tabtps.common.service.RegionStatsService;
import xyz.jpenilla.tabtps.common.util.TPSUtil;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public final class FoliaTickInfoCommandFormatter implements TickInfoCommand.Formatter {
  private final RegionStatsService regionStatsService;

  public FoliaTickInfoCommandFormatter(final @NonNull RegionStatsService regionStatsService) {
    this.regionStatsService = regionStatsService;
  }

  @Override
  public @NonNull List<Component> formatTickTimes() {
    final List<Component> output = new ArrayList<>();
    final Theme.Colors colors = Theme.DEFAULT.colorScheme();

    output.add(text()
      .color(GRAY)
      .append(text("Region TPS Overview", GRAY))
      .build());

    output.add(text()
      .append(space())
      .append(text("├─", WHITE))
      .append(space())
      .append(text("Lowest", GRAY))
      .append(text(" - ", WHITE))
      .append(TPSUtil.coloredTps(this.regionStatsService.lowestRegionTps(), colors))
      .build());

    output.add(text()
      .append(space())
      .append(text("├─", WHITE))
      .append(space())
      .append(text("Median", GRAY))
      .append(text(" - ", WHITE))
      .append(TPSUtil.coloredTps(this.regionStatsService.medianRegionTps(), colors))
      .build());

    output.add(text()
      .append(space())
      .append(text("└─", WHITE))
      .append(space())
      .append(text("Highest", GRAY))
      .append(text(" - ", WHITE))
      .append(TPSUtil.coloredTps(this.regionStatsService.highestRegionTps(), colors))
      .build());

    return output;
  }
}
