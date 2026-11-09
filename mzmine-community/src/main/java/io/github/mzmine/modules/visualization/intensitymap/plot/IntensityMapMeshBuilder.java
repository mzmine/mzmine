/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.intensitymap.plot;

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSmoothing;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapScale;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * Builds meshes and smooths overlays in the background, on a small thread pool. Newer requests
 * cancel older ones through generation counters, and results only reach the FX thread while they
 * are current.
 */
final class IntensityMapMeshBuilder implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(IntensityMapMeshBuilder.class.getName());
  private final ExecutorService builders;
  private final AtomicInteger generation = new AtomicInteger();
  private final AtomicInteger smoothingGeneration = new AtomicInteger();
  // smoothing results by overlay id, so layout or scale changes do not smooth again
  private final Map<String, IntensityMapSmoothedGrid> smoothed = new HashMap<>();
  private volatile boolean closed;

  IntensityMapMeshBuilder() {
    final int threads = Math.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 4);
    final ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(), runnable -> {
      final Thread thread = new Thread(runnable, "3D surface geometry");
      thread.setDaemon(true);
      return thread;
    });
    // idle threads end, also when a plot is discarded without close()
    pool.allowCoreThreadTimeOut(true);
    builders = pool;
  }

  /**
   * Cancels running mesh builds, e.g. when the overlays change.
   */
  void invalidate() {
    generation.incrementAndGet();
  }

  /**
   * Cancels all work and forgets smoothed overlays.
   */
  void reset() {
    generation.incrementAndGet();
    smoothingGeneration.incrementAndGet();
    smoothed.clear();
  }

  /**
   * Forgets smoothed overlays that were removed.
   */
  void retainSmoothed(@NotNull final Collection<String> ids) {
    smoothed.keySet().retainAll(ids);
  }

  /**
   * Builds the meshes that the overlay states need for the scale.
   *
   * @param onDone  on the FX thread with the new meshes by overlay id, if no newer build started
   * @param onError on the FX thread if a build failed
   * @return number of meshes built in the background, 0 if onDone already ran
   */
  int build(@NotNull final List<IntensityMapSeries> snapshot,
      @NotNull final Map<String, IntensityMapSeriesState> states,
      @NotNull final IntensityMapScale target,
      @NotNull final Consumer<Map<String, IntensityMapBuiltMesh>> onDone,
      @NotNull final Consumer<Throwable> onError) {
    final int request = generation.incrementAndGet();
    final BooleanSupplier canceled = () -> generation.get() != request || closed;
    final Map<String, CompletableFuture<IntensityMapBuiltMesh>> builds = new LinkedHashMap<>();
    for (final IntensityMapSeries value : snapshot) {
      if (states.get(value.id()).needsMesh(value.data(), target)) {
        builds.put(value.id(), CompletableFuture.supplyAsync(
            () -> new IntensityMapBuiltMesh(IntensityMapMesh.build(value.data(), target, canceled),
                IntensityMapMesh.envelope(value.data(), target,
                    IntensityMapTileLayout.FIT_DIVISIONS, canceled)), builders));
      }
    }
    if (builds.isEmpty()) {
      onDone.accept(Map.of());
      return 0;
    }
    CompletableFuture.allOf(builds.values().toArray(CompletableFuture[]::new))
        .whenComplete((_, error) -> Platform.runLater(() -> {
          if (closed || generation.get() != request) {
            return;
          }
          if (error != null) {
            logger.log(Level.WARNING, "Cannot build 3D surface", error);
            onError.accept(error);
            return;
          }
          final Map<String, IntensityMapBuiltMesh> meshes = new HashMap<>();
          builds.forEach((id, future) -> meshes.put(id, future.join()));
          onDone.accept(meshes);
        }));
    return builds.size();
  }

  /**
   * Smooths the overlays in the background if needed. Results are cached per overlay.
   *
   * @param onReady called with the displayed overlays if all are available
   * @param retry   on the FX thread when background smoothing finished, to show the overlays
   * @param onError on the FX thread if smoothing failed
   * @return true if smoothing runs in the background
   */
  boolean smooth(@NotNull final List<IntensityMapSeries> overlays,
      @NotNull final IntensityMapSmoothing params,
      @NotNull final Consumer<List<IntensityMapSeries>> onReady, @NotNull final Runnable retry,
      @NotNull final Consumer<Throwable> onError) {
    if (!params.active()) {
      smoothingGeneration.incrementAndGet();
      smoothed.clear();
      onReady.accept(overlays);
      return false;
    }
    final List<IntensityMapSeries> missing = overlays.stream().filter(value -> {
      final IntensityMapSmoothedGrid cached = smoothed.get(value.id());
      return cached == null || cached.source() != value.data() || !cached.params().equals(params);
    }).toList();
    if (missing.isEmpty()) {
      onReady.accept(overlays.stream().map(
          value -> new IntensityMapSeries(value.id(), value.name(), value.description(),
              smoothed.get(value.id()).result(), value.color())).toList());
      return false;
    }
    final int request = smoothingGeneration.incrementAndGet();
    final BooleanSupplier canceled = () -> smoothingGeneration.get() != request || closed;
    CompletableFuture.supplyAsync(() -> {
      final Map<String, IntensityMapSmoothedGrid> results = new HashMap<>();
      for (final IntensityMapSeries value : missing) {
        results.put(value.id(), new IntensityMapSmoothedGrid(value.data(), params,
            params.apply(value.data(), canceled)));
      }
      return results;
    }, builders).whenComplete((results, error) -> Platform.runLater(() -> {
      if (closed || smoothingGeneration.get() != request) {
        return;
      }
      if (error != null) {
        logger.log(Level.WARNING, "Cannot smooth 3D data", error);
        onError.accept(error);
        return;
      }
      smoothed.putAll(results);
      retry.run();
    }));
    return true;
  }

  @Override
  public void close() {
    closed = true;
    generation.incrementAndGet();
    builders.shutdownNow();
  }
}
