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
package xyz.jpenilla.tabtps.folia.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jpenilla.tabtps.common.User;
import xyz.jpenilla.tabtps.common.service.TickTimeService;

public final class FoliaTickTimeService implements TickTimeService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FoliaTickTimeService.class);

  final @Nullable RegionTickDataAccessor accessor;

  public FoliaTickTimeService() {
    RegionTickDataAccessor resolved = null;
    try {
      resolved = new RegionTickDataAccessor();
      LOGGER.info("Successfully initialized Folia region tick data accessor");
    } catch (final Throwable t) {
      LOGGER.warn("Failed to initialize Folia region tick data accessor, TPS/MSPT displays will show fallback values", t);
    }
    this.accessor = resolved;
  }

  @Override
  public double averageMspt() {
    if (this.accessor != null) {
      final List<double[]> allData = this.accessor.collectAllRegionData();
      if (!allData.isEmpty()) {
        double sum = 0;
        for (final double[] d : allData) sum += d[1];
        return sum / allData.size();
      }
    }
    return 0;
  }

  @Override
  public double @NonNull [] recentTps() {
    if (this.accessor != null) {
      final List<double[]> allData = this.accessor.collectAllRegionData();
      if (!allData.isEmpty()) {
        double sum = 0;
        for (final double[] d : allData) sum += d[0];
        final double avg = sum / allData.size();
        return new double[]{avg, avg, avg};
      }
    }
    return new double[]{20.0, 20.0, 20.0};
  }

  @Override
  public double averageMspt(final @Nullable User<?> user) {
    if (this.accessor != null && user != null && user.base() instanceof Player) {
      final double[] data = this.accessor.getRegionDataForPlayer((Player) user.base());
      if (data != null) return data[1];
    }
    return this.averageMspt();
  }

  @Override
  public double @NonNull [] recentTps(final @Nullable User<?> user) {
    if (this.accessor != null && user != null && user.base() instanceof Player) {
      final double[] data = this.accessor.getRegionDataForPlayer((Player) user.base());
      if (data != null) {
        return new double[]{data[0], data[0], data[0]};
      }
    }
    return this.recentTps();
  }

  static final class RegionTickDataAccessor {
    private final Method getServer;
    private final Method getAllLevels;
    private final Field regioniserField;
    private final Method computeForAllRegions;
    private final Method getRegionAt;
    private final Method getData;
    private final Method getSchedulingHandle;
    private final Method getTickReport;
    private final Method timePerTickDataMethod;
    private final Method rawDataMethod;
    private final Method getCraftWorldHandle;

    RegionTickDataAccessor() throws ReflectiveOperationException {
      final Class<?> mcServerClass = Class.forName("net.minecraft.server.MinecraftServer");
      this.getServer = mcServerClass.getMethod("getServer");
      this.getAllLevels = mcServerClass.getMethod("getAllLevels");

      final Class<?> serverLevelClass = Class.forName("net.minecraft.server.level.ServerLevel");

      Field regField = null;
      for (final Field f : serverLevelClass.getDeclaredFields()) {
        if (f.getType().getSimpleName().contains("ThreadedRegionizer")) {
          f.setAccessible(true);
          regField = f;
          break;
        }
      }
      if (regField == null) {
        throw new NoSuchFieldException("Could not find ThreadedRegionizer field on ServerLevel. Fields: " + dumpFields(serverLevelClass));
      }
      this.regioniserField = regField;

      final Class<?> regionizerClass = regField.getType();

      Method computeMethod = null;
      for (final Method m : regionizerClass.getDeclaredMethods()) {
        if (m.getName().equals("computeForAllRegionsUnsynchronised") && m.getParameterCount() == 1) {
          m.setAccessible(true);
          computeMethod = m;
          break;
        }
      }
      if (computeMethod == null) {
        throw new NoSuchMethodException("Could not find computeForAllRegionsUnsynchronised on " + regionizerClass.getName());
      }
      this.computeForAllRegions = computeMethod;

      Method regionAtMethod = null;
      for (final Method m : regionizerClass.getDeclaredMethods()) {
        if (m.getName().equals("getRegionAtUnsynchronised") && m.getParameterCount() == 2) {
          m.setAccessible(true);
          regionAtMethod = m;
          break;
        }
      }
      this.getRegionAt = regionAtMethod;

      final Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit.CraftWorld");
      this.getCraftWorldHandle = craftWorldClass.getMethod("getHandle");

      final Class<?> threadedRegionClass = Class.forName("io.papermc.paper.threadedregions.ThreadedRegionizer$ThreadedRegion");

      Method getDataMethod = null;
      for (final Method m : threadedRegionClass.getDeclaredMethods()) {
        if (m.getName().equals("getData") && m.getParameterCount() == 0) {
          m.setAccessible(true);
          getDataMethod = m;
          break;
        }
      }
      if (getDataMethod == null) {
        throw new NoSuchMethodException("Could not find getData on ThreadedRegion. Methods: " + dumpMethods(threadedRegionClass));
      }
      this.getData = getDataMethod;

      // getData() returns generic T at compile time, but runtime type is TickRegions$TickRegionData
      final Class<?> regionDataClass = Class.forName("io.papermc.paper.threadedregions.TickRegions$TickRegionData");

      Method handleMethod = null;
      for (final Method m : regionDataClass.getDeclaredMethods()) {
        final String retName = m.getReturnType().getSimpleName();
        if (m.getParameterCount() == 0 && (retName.contains("ScheduleHandle") || retName.contains("RegionSchedule"))) {
          m.setAccessible(true);
          handleMethod = m;
          break;
        }
      }
      if (handleMethod == null) {
        for (final Method m : regionDataClass.getDeclaredMethods()) {
          if (m.getParameterCount() == 0 && !m.getReturnType().isPrimitive() && !m.getReturnType().equals(Object.class)) {
            for (final Method sub : m.getReturnType().getDeclaredMethods()) {
              final String subName = sub.getName().toLowerCase();
              if (subName.contains("tick") && subName.contains("report")) {
                m.setAccessible(true);
                handleMethod = m;
                break;
              }
            }
            if (handleMethod != null) break;
          }
        }
      }
      if (handleMethod == null) {
        throw new NoSuchMethodException("Could not find scheduling handle on " + regionDataClass.getName() + ". Methods: " + dumpMethods(regionDataClass));
      }
      this.getSchedulingHandle = handleMethod;

      final Class<?> handleClass = handleMethod.getReturnType();

      Method tickReportMethod = null;
      for (final Method m : handleClass.getDeclaredMethods()) {
        final String name = m.getName().toLowerCase();
        if (m.getParameterCount() <= 1 && (name.contains("tickreport") || name.contains("tick_report"))) {
          m.setAccessible(true);
          tickReportMethod = m;
          break;
        }
      }
      if (tickReportMethod == null) {
        for (final Method m : handleClass.getDeclaredMethods()) {
          if (m.getParameterCount() == 0 && !m.getReturnType().isPrimitive()) {
            boolean hasTpsMspt = false;
            for (final Method rm : m.getReturnType().getDeclaredMethods()) {
              final String rmName = rm.getName().toLowerCase();
              if (rmName.contains("tps") || rmName.contains("mspt")) {
                hasTpsMspt = true;
                break;
              }
            }
            if (hasTpsMspt) {
              m.setAccessible(true);
              tickReportMethod = m;
              break;
            }
          }
        }
      }
      if (tickReportMethod == null) {
        throw new NoSuchMethodException("Could not find tick report on " + handleClass.getName() + ". Methods: " + dumpMethods(handleClass));
      }
      this.getTickReport = tickReportMethod;

      final Class<?> reportClass = tickReportMethod.getReturnType();

      Method timePerTickData = null;
      for (final Method m : reportClass.getDeclaredMethods()) {
        if (m.getParameterCount() == 0 && m.getName().equals("timePerTickData")) {
          m.setAccessible(true);
          timePerTickData = m;
          break;
        }
      }
      if (timePerTickData == null) {
        throw new NoSuchMethodException("Could not find timePerTickData on " + reportClass.getName() + ". Methods: " + dumpMethods(reportClass));
      }
      this.timePerTickDataMethod = timePerTickData;

      final Class<?> segmentedAverageClass = timePerTickData.getReturnType();
      Method rawData = null;
      for (final Method m : segmentedAverageClass.getDeclaredMethods()) {
        if (m.getName().equals("rawData") && m.getReturnType() == long[].class) {
          m.setAccessible(true);
          rawData = m;
          break;
        }
      }
      if (rawData == null) {
        throw new NoSuchMethodException("Could not find rawData on " + segmentedAverageClass.getName() + ". Methods: " + dumpMethods(segmentedAverageClass));
      }
      this.rawDataMethod = rawData;

      LOGGER.info("Folia path: MinecraftServer -> ServerLevel.{} -> computeForAllRegionsUnsynchronised -> getData() -> {}.{} -> {}.{} -> timePerTickData().rawData()",
        regioniserField.getName(),
        regionDataClass.getSimpleName(), handleMethod.getName(),
        handleClass.getSimpleName(), tickReportMethod.getName());
    }

    @Nullable double[] getRegionDataForPlayer(final Player player) {
      if (this.getRegionAt == null || this.getCraftWorldHandle == null) return null;
      try {
        final World world = player.getWorld();
        final Location loc = player.getLocation();
        final int chunkX = loc.getBlockX() >> 4;
        final int chunkZ = loc.getBlockZ() >> 4;

        final Object serverLevel = this.getCraftWorldHandle.invoke(world);
        final Object regionizer = this.regioniserField.get(serverLevel);
        if (regionizer == null) return null;

        final Object region = this.getRegionAt.invoke(regionizer, chunkX, chunkZ);
        if (region == null) return null;

        return this.extractRegionData(region);
      } catch (final Throwable t) {
        return null;
      }
    }

    private @Nullable double[] extractRegionData(final Object region) throws Throwable {
      final Object data = this.getData.invoke(region);
      if (data == null) return null;
      final Object handle = this.getSchedulingHandle.invoke(data);
      if (handle == null) return null;

      final Object report;
      if (this.getTickReport.getParameterCount() == 1) {
        report = this.getTickReport.invoke(handle, System.nanoTime());
      } else {
        report = this.getTickReport.invoke(handle);
      }
      if (report == null) return null;

      final Object timePerTickAvg = this.timePerTickDataMethod.invoke(report);
      if (timePerTickAvg == null) return null;
      final long[] rawData = (long[]) this.rawDataMethod.invoke(timePerTickAvg);
      if (rawData == null || rawData.length == 0) return null;

      long sum = 0;
      int count = 0;
      for (final long val : rawData) {
        if (val != 0) { sum += val; count++; }
      }
      if (count == 0) return null;

      final double avgNanos = (double) sum / count;
      final double mspt = avgNanos * 1.0E-6D;
      final double tps = Math.min(20.0, 1000.0 / mspt);
      return new double[]{tps, mspt};
    }

    @SuppressWarnings("unchecked")
    List<double[]> collectAllRegionData() {
      final List<double[]> results = new ArrayList<>();
      try {
        final Object server = this.getServer.invoke(null);
        final Iterable<Object> levels = (Iterable<Object>) this.getAllLevels.invoke(server);

        for (final Object level : levels) {
          final Object regionizer = this.regioniserField.get(level);
          if (regionizer == null) continue;

          final Consumer<Object> regionConsumer = region -> {
            try {
              final double[] regionData = this.extractRegionData(region);
              if (regionData != null) {
                synchronized (results) {
                  results.add(regionData);
                }
              }
            } catch (final Throwable ignored) {
            }
          };

          this.computeForAllRegions.invoke(regionizer, regionConsumer);
        }
      } catch (final Throwable t) {
        // silent fallback
      }
      return results;
    }

    List<Double> collectAllRegionTps() {
      final List<double[]> data = this.collectAllRegionData();
      final List<Double> tps = new ArrayList<>(data.size());
      for (final double[] d : data) tps.add(d[0]);
      Collections.sort(tps);
      return tps;
    }

    private static String dumpMethods(final Class<?> clazz) {
      final StringBuilder sb = new StringBuilder("[");
      for (final Method m : clazz.getDeclaredMethods()) {
        sb.append(m.getReturnType().getSimpleName()).append(" ").append(m.getName()).append("(").append(m.getParameterCount()).append("), ");
      }
      sb.append("]");
      return sb.toString();
    }

    private static String dumpFields(final Class<?> clazz) {
      final StringBuilder sb = new StringBuilder("[");
      for (final Field f : clazz.getDeclaredFields()) {
        sb.append(f.getType().getSimpleName()).append(" ").append(f.getName()).append(", ");
      }
      sb.append("]");
      return sb.toString();
    }
  }
}
