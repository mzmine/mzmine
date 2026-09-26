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

package io.github.mzmine.modules.visualization.intensitymap;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapDetail;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPerspective;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapProjection;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapTopView;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapPlot;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapVisibleArea;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapFrameCache;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapLayer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskPriority;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sampled data of the overlay layers: a coarse base of the complete range per layer, and after
 * zooming in the base merged with full detail of the visible window (user request: zoom like the
 * former 2D plot instead of selecting subsets). Reading raw data is expensive, so view changes are
 * debounced, only one read runs at a time, and the window is only read again when the view leaves
 * it or zooms in well beyond its resolution. Zoomed out, the base is shown without reading.
 */
final class IntensityMapDetailLoader implements IntensityMapSamplingListener {

  // share of the window size added on each side, so that small pans need no read
  private static final double WINDOW_MARGIN = 0.25;
  // 3D: the window is read again when the visible range is smaller than this share of it
  private static final double MIN_ZOOM_SHARE = 0.4;
  // 2D: the window is read again when its bins get coarser than this many screen pixels
  private static final double MAX_PIXELS_PER_BIN = 1.25;
  // the base is read again when the view needs this many times more bins along an axis
  private static final double BASE_REFRESH = 1.25;
  // zoom windows kept per layer to show them again without reading
  private static final int CACHED_WINDOWS = 3;

  private final IntensityMapPlot plot;
  private final ParameterSet parameters;
  private final Supplier<List<IntensityMapLayer>> layers;
  private final Supplier<ImageNormalization> normalization;
  private final Supplier<IntensityMapFrameCache> frames;
  private final Runnable publish;
  // coarse data of the complete range, the view when zoomed out
  private final Map<String, IntensityMapGrid> sampled = new HashMap<>();
  // base merged with full detail of the visible window after zooming in
  private final Map<String, IntensityMapGrid> detailed = new HashMap<>();
  // layers without data in their m/z range
  private final Set<String> outside = new HashSet<>();
  private final PauseTransition delay = new PauseTransition(Duration.millis(300));
  // the window of the detailed data, by layer
  private final Map<String, IntensityMapRegion> windows = new HashMap<>();
  // recently read windows by layer, newest first; zooming back reuses them (user request)
  private final Map<String, Deque<IntensityMapCachedWindow>> cache = new HashMap<>();
  private @Nullable IntensityMapSamplingTask task;
  private @Nullable IntensityMapDetail sampledDetail;
  private boolean closed;

  /**
   * @param layers        current overlay layers
   * @param normalization current intensity normalization
   * @param frames        current mobility frames
   * @param publish       shows the sampled data, see {@link #data(String)}
   */
  IntensityMapDetailLoader(@NotNull final IntensityMapPlot plot,
      @NotNull final ParameterSet parameters,
      @NotNull final Supplier<List<IntensityMapLayer>> layers,
      @NotNull final Supplier<ImageNormalization> normalization,
      @NotNull final Supplier<IntensityMapFrameCache> frames, @NotNull final Runnable publish) {
    this.plot = plot;
    this.parameters = parameters;
    this.layers = layers;
    this.normalization = normalization;
    this.frames = frames;
    this.publish = publish;
    delay.setOnFinished(_ -> onViewChanged());
  }

  /**
   * The plot shows another part of the data, the window is checked once the view has settled.
   */
  void viewChanged() {
    delay.playFromStart();
  }

  /**
   * @return the data to show for the layer: the base, or the base merged with the window
   */
  @Nullable IntensityMapGrid data(@NotNull final String id) {
    return detailed.getOrDefault(id, sampled.get(id));
  }

  /**
   * @return true if some layers have no data in their m/z range
   */
  boolean hasEmptyLayers() {
    return !outside.isEmpty();
  }

  /**
   * @return true while data are read
   */
  boolean isLoading() {
    return task != null;
  }

  /**
   * Forgets all sampled data, e.g. to read with new settings.
   */
  void clear() {
    sampled.clear();
    detailed.clear();
    outside.clear();
    windows.clear();
    cache.clear();
  }

  /**
   * Forgets the data of removed layers.
   */
  void retain(@NotNull final Collection<String> ids) {
    sampled.keySet().retainAll(ids);
    detailed.keySet().retainAll(sampled.keySet());
    windows.keySet().retainAll(sampled.keySet());
    cache.keySet().retainAll(sampled.keySet());
  }

  void remove(@NotNull final String id) {
    sampled.remove(id);
    detailed.remove(id);
    outside.remove(id);
    windows.remove(id);
    cache.remove(id);
  }

  /**
   * Reads the base of layers without data, and shrinks existing layers to their share of the render
   * budget without reading raw data again.
   *
   * @param all resample every layer, otherwise only layers without data
   */
  void load(final boolean all) {
    final List<IntensityMapLayer> current = layers.get();
    final IntensityMapDetail detail = baseDetail(current.size());
    if (all) {
      clear();
    }
    final Map<RawDataFile, List<IntensityMapLayer>> missing = new LinkedHashMap<>();
    for (final IntensityMapLayer layer : current) {
      if (!sampled.containsKey(layer.id()) && !outside.contains(layer.id())) {
        missing.computeIfAbsent(layer.file(), _ -> new ArrayList<>()).add(layer);
      }
    }
    final Map<String, IntensityMapGrid> shrink = new HashMap<>();
    for (final var entry : sampled.entrySet()) {
      final IntensityMapGrid data = entry.getValue();
      final var size = detail.grid(data.width(), data.height(), data.pixels());
      if (size.x() < data.width() || size.y() < data.height()) {
        shrink.put(entry.getKey(), data);
      }
    }
    cancel();
    if (missing.isEmpty() && shrink.isEmpty()) {
      publish.run();
      return;
    }
    final int count = missing.values().stream().mapToInt(List::size).sum();
    final String message =
        "Reading " + count + (count == 1 ? " overlay" : " overlays") + " from " + missing.size() + (
            missing.size() == 1 ? " sample…" : " samples…");
    start(new IntensityMapSamplingTask(missing, shrink, Map.of(), parameters, detail,
        IntensityMapRegion.FULL, normalization.get(), frames.get(), message, this));
    plot.setLoading(missing.isEmpty() ? "Adjusting detail…" : message, missing.isEmpty() ? -1 : 0);
  }

  private void onViewChanged() {
    if (closed || sampledDetail == null) {
      return;
    }
    final IntensityMapSamplingTask running = task;
    if (running != null && running.window().isFull()) {
      // the base is still read, it requests the window when done
      return;
    }
    if (running == null && coarser(sampledDetail, plot.detail())) {
      refreshBase();
      return;
    }
    final IntensityMapVisibleArea area = plot.visibleArea();
    final IntensityMapRegion visible = area.window();
    if (visible.isFull()) {
      if (!detailed.isEmpty() || running != null) {
        cancel();
        plot.setLoading(null, 0);
        detailed.clear();
        windows.clear();
        publish.run();
      }
      return;
    }
    // decision (user request): only overlays in view are read, e.g. not the other side by side
    // tiles when zoomed into one; layers whose window still covers the view are kept
    final double share = minZoomShare(plot.detail().projection());
    final List<IntensityMapLayer> uncovered = layers.get().stream().filter(
        layer -> sampled.containsKey(layer.id()) && area.overlays().contains(layer.id()) && !covers(
            windows.get(layer.id()), visible, share)).toList();
    final List<IntensityMapLayer> needed = new ArrayList<>();
    boolean restored = false;
    for (final IntensityMapLayer layer : uncovered) {
      final IntensityMapCachedWindow cached = cached(layer.id(), visible, share);
      if (cached == null) {
        needed.add(layer);
      } else {
        detailed.put(layer.id(), cached.data());
        windows.put(layer.id(), cached.window());
        restored = true;
      }
    }
    if (restored) {
      publish.run();
    }
    if (needed.isEmpty()) {
      return;
    }
    if (running != null && covers(running.window(), visible, share) && running.layerIds()
        .containsAll(needed.stream().map(IntensityMapLayer::id).toList())) {
      return;
    }
    loadWindow(visible.expand(WINDOW_MARGIN), needed, area.overlays().size());
  }

  /**
   * @return a window read before that shows the visible range at a similar resolution, null if none
   */
  private @Nullable IntensityMapCachedWindow cached(@NotNull final String id,
      @NotNull final IntensityMapRegion visible, final double share) {
    final Deque<IntensityMapCachedWindow> entries = cache.get(id);
    if (entries == null) {
      return null;
    }
    for (final IntensityMapCachedWindow entry : entries) {
      if (covers(entry.window(), visible, share)) {
        return entry;
      }
    }
    return null;
  }

  private void remember(@NotNull final String id, @NotNull final IntensityMapRegion window,
      @NotNull final IntensityMapGrid data) {
    final Deque<IntensityMapCachedWindow> entries = cache.computeIfAbsent(id,
        _ -> new ArrayDeque<>());
    entries.removeIf(entry -> entry.window().equals(window));
    entries.addFirst(new IntensityMapCachedWindow(window, data));
    while (entries.size() > CACHED_WINDOWS) {
      entries.removeLast();
    }
  }

  /**
   * @return sampling detail of the base: the complete range at the resolution of the zoomed out
   * view, shared by all layers
   */
  private @NotNull IntensityMapDetail baseDetail(final int layerCount) {
    final IntensityMapDetail view = plot.detail();
    return new IntensityMapDetail(view.width(), view.height(), Math.max(1, layerCount),
        view.projection());
  }

  /**
   * @return true if the view needs noticeably more bins than the base was sampled with, e.g. after
   * the first layout or after enlarging the window
   */
  private static boolean coarser(@NotNull final IntensityMapDetail sampled,
      @NotNull final IntensityMapDetail view) {
    final var projection = view.projection();
    final var before = sampled.projection();
    return projection.viewColumns(view.width()) > before.viewColumns(sampled.width()) * BASE_REFRESH
        || projection.viewRows(view.width(), view.height())
        > before.viewRows(sampled.width(), sampled.height()) * BASE_REFRESH;
  }

  /**
   * Reads the base of all layers again at the resolution of the view. The old base stays visible
   * meanwhile.
   */
  private void refreshBase() {
    final List<IntensityMapLayer> current = layers.get();
    final Map<RawDataFile, List<IntensityMapLayer>> read = new LinkedHashMap<>();
    for (final IntensityMapLayer layer : current) {
      if (sampled.containsKey(layer.id())) {
        read.computeIfAbsent(layer.file(), _ -> new ArrayList<>()).add(layer);
      }
    }
    if (read.isEmpty()) {
      return;
    }
    start(new IntensityMapSamplingTask(read, Map.of(), Map.of(), parameters,
        baseDetail(current.size()), IntensityMapRegion.FULL, normalization.get(), frames.get(),
        "Adjusting detail…", this));
    plot.setLoading("Adjusting detail…", -1);
  }

  /**
   * @return smallest share of the window that may be visible before the window is read again
   */
  private static double minZoomShare(@NotNull final IntensityMapProjection projection) {
    return switch (projection) {
      // decision (user request): the 2D view keeps about one bin per screen pixel at every zoom,
      // like the former 2D plot. Reads are debounced and one at a time, so this stays cheap.
      case IntensityMapTopView _ -> 1 / (MAX_PIXELS_PER_BIN * (1 + 2 * WINDOW_MARGIN));
      case IntensityMapPerspective _ -> MIN_ZOOM_SHARE;
    };
  }

  /**
   * @param share smallest share of the window that may be visible
   * @return true if the window contains the visible range at a similar resolution
   */
  private static boolean covers(@Nullable final IntensityMapRegion window,
      @NotNull final IntensityMapRegion visible, final double share) {
    return window != null && !window.isFull() && window.encloses(visible)
        && visible.width() >= window.width() * share && visible.height() >= window.height() * share;
  }

  /**
   * Reads the window of the layers and merges it with their base.
   *
   * @param shown number of overlays in view, which share the render budget
   */
  private void loadWindow(@NotNull final IntensityMapRegion window,
      @NotNull final List<IntensityMapLayer> needed, final int shown) {
    final Map<RawDataFile, List<IntensityMapLayer>> read = new LinkedHashMap<>();
    for (final IntensityMapLayer layer : needed) {
      read.computeIfAbsent(layer.file(), _ -> new ArrayList<>()).add(layer);
    }
    final IntensityMapDetail view = plot.detail();
    // the margin around the visible range gets the same resolution as the visible range; the
    // window and the coarse base outside it share the render budget of the overlays in view
    final double enlarged = 1 + 2 * WINDOW_MARGIN;
    final IntensityMapDetail detail = new IntensityMapDetail(view.width() * enlarged,
        view.height() * enlarged, 2 * Math.max(1, shown), view.projection());
    cancel();
    start(new IntensityMapSamplingTask(read, Map.of(), Map.copyOf(sampled), parameters, detail,
        window, normalization.get(), frames.get(), "Adjusting detail…", this));
    plot.setLoading("Adjusting detail…", -1);
  }

  private void start(@NotNull final IntensityMapSamplingTask next) {
    task = next;
    MZmineCore.getTaskController().addTask(next, TaskPriority.HIGH);
  }

  private void cancel() {
    if (task != null) {
      task.cancel();
      task = null;
    }
  }

  @Override
  public void progress(@NotNull final IntensityMapSamplingTask source, final double progress) {
    Platform.runLater(() -> {
      if (task == source) {
        plot.setLoading(source.message(), progress);
      }
    });
  }

  @Override
  public void finished(@NotNull final IntensityMapSamplingTask source,
      @NotNull final Map<String, IntensityMapGrid> results, @NotNull final Set<String> empty,
      @NotNull final Map<String, String> skipped) {
    Platform.runLater(() -> {
      if (closed || task != source) {
        return;
      }
      task = null;
      if (source.isCanceled()) {
        plot.setLoading(null, 0);
        return;
      }
      // overlays removed while reading
      final Set<String> current = new HashSet<>(
          layers.get().stream().map(IntensityMapLayer::id).toList());
      results.keySet().retainAll(current);
      empty.retainAll(current);
      plot.setLoading(null, 0);
      if (source.window().isFull()) {
        sampled.putAll(results);
        outside.addAll(empty);
        sampledDetail = source.detail();
        // detail of a zoomed view belongs to the previous base
        detailed.clear();
        windows.clear();
        cache.clear();
        publish.run();
        delay.playFromStart();
      } else {
        for (final String id : source.layerIds()) {
          // layers without data in the window, or whose file failed, keep their base
          final IntensityMapGrid data = results.getOrDefault(id, sampled.get(id));
          if (current.contains(id) && data != null) {
            detailed.put(id, data);
            windows.put(id, source.window());
            remember(id, source.window(), data);
          }
        }
        publish.run();
      }
      if (!skipped.isEmpty()) {
        final var first = skipped.entrySet().iterator().next();
        plot.setStatus(first.getValue() + " in " + String.join(", ", skipped.keySet())
            + ". Adjust the scan selection of the module.");
      }
    });
  }

  /**
   * Clears the loading state if the task is still the current one, e.g. after it was canceled in
   * the task manager.
   */
  @Override
  public void released(@NotNull final IntensityMapSamplingTask source) {
    Platform.runLater(() -> {
      if (!closed && task == source) {
        task = null;
        plot.setLoading(null, 0);
      }
    });
  }

  @Override
  public void failed(@NotNull final IntensityMapSamplingTask source,
      @NotNull final String message) {
    Platform.runLater(() -> {
      if (!closed && task == source) {
        task = null;
        plot.setLoading(null, 0);
        plot.setStatus(message);
      }
    });
  }

  void close() {
    closed = true;
    delay.stop();
    cancel();
  }
}
