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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapDetail;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPerspective;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapProjection;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapTopView;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapOverlayView;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapPlot;
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
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sampled data of the overlay layers: a coarse base of the complete range per layer and, after
 * zooming in, the base merged with full detail of the visible window, so zooming reveals detail
 * instead of selecting subsets. Every overlay has its own window, side by side tiles show different
 * parts of the data. Reading raw data is expensive, so view changes are debounced, only one read
 * runs at a time, and a window is only read again when the view leaves it or zooms in well beyond
 * its resolution. Zoomed out, the base is shown without reading.
 */
final class IntensityMapDetailLoader implements IntensityMapSamplingListener {

  // share of the window size added on each side, so that small pans need no read
  private static final double WINDOW_MARGIN = 0.25;
  // 2D: while panning, the window reaches further ahead than behind, so it reads ahead
  private static final double LEAD_MARGIN = 0.5;
  private static final double TRAIL_MARGIN = 0.1;
  // pans shorter than this share of the visible size count as standing still
  private static final double MIN_PAN = 0.02;
  // 2D: data read before are kept around new windows up to this many bins per overlay, beyond
  // that a window is merged with the base again
  private static final long MAX_KEPT_BINS = 4_000_000;
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
  // decision: 2D windows are read while the view moves, checked at most this often, instead of
  // only after the view has settled, so panning does not reveal coarse strips
  private static final Duration WHILE_MOVING = Duration.millis(150);
  // 3D: reading and rebuilding surfaces while rotating would stutter, so the view settles first
  private static final Duration SETTLED = Duration.millis(300);
  private final PauseTransition delay = new PauseTransition(SETTLED);
  // the window of the detailed data, by layer
  private final Map<String, IntensityMapRegion> windows = new HashMap<>();
  // recently read windows by layer, newest first; zooming back shows them without reading
  private final Map<String, Deque<IntensityMapCachedWindow>> cache = new HashMap<>();
  private @Nullable IntensityMapSamplingTask task;
  // visible part of every overlay at the last check, to read ahead while panning
  private final Map<String, IntensityMapRegion> lastVisible = new HashMap<>();
  // visible parts at the check before the last one, for the pan direction
  private @NotNull Map<String, IntensityMapRegion> previous = Map.of();
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
   * The plot shows another part of the data. Zoomed out overlays show their base at once; the
   * windows are checked at intervals while the 2D view moves, and once the 3D view has settled.
   */
  void viewChanged() {
    showZoomedOutBases();
    switch (plot.detail().projection()) {
      case IntensityMapTopView _ -> {
        // throttled, not debounced: moving on does not postpone the check
        if (delay.getStatus() != Animation.Status.RUNNING) {
          delay.setDuration(WHILE_MOVING);
          delay.playFromStart();
        }
      }
      case IntensityMapPerspective _ -> {
        delay.setDuration(SETTLED);
        delay.playFromStart();
      }
    }
  }

  /**
   * Shows the base of zoomed out overlays at once, instead of the coarse surroundings of the last
   * window until the next window check.
   */
  private void showZoomedOutBases() {
    if (closed || sampledDetail == null || detailed.isEmpty()) {
      return;
    }
    final Map<String, IntensityMapOverlayView> views = plot.visibleArea().overlays();
    final IntensityMapProjection projection = plot.detail().projection();
    boolean changed = false;
    for (final String id : List.copyOf(detailed.keySet())) {
      changed |= dropWindowIfZoomedOut(id, views.get(id), projection);
    }
    if (changed) {
      publish.run();
    }
  }

  /**
   * @param view visible part of the layer, null if it is not in view
   * @return true if the window was dropped, so that the layer shows its base again
   */
  private boolean dropWindowIfZoomedOut(@NotNull final String id,
      @Nullable final IntensityMapOverlayView view,
      @NotNull final IntensityMapProjection projection) {
    final IntensityMapGrid base = sampled.get(id);
    if (view == null || base == null || !view.all() || !baseSuffices(base, view, projection)) {
      return false;
    }
    windows.remove(id);
    return detailed.remove(id) != null;
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
    final Map<RawDataFile, List<IntensityMapLayer>> missing = byFile(current.stream()
        .filter(layer -> !sampled.containsKey(layer.id()) && !outside.contains(layer.id()))
        .toList());
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
    final String message = missing.isEmpty() ? IntensityMapSamplingTask.ADJUSTING
        : "Reading " + count + (count == 1 ? " overlay" : " overlays") + " from " + missing.size()
            + (missing.size() == 1 ? " sample…" : " samples…");
    start(IntensityMapSamplingTask.base(missing, shrink, parameters, detail, normalization.get(),
        frames.get(), message, this), missing.isEmpty() ? -1 : 0);
  }

  /**
   * @return the layers grouped by their raw data file, in order
   */
  private static @NotNull Map<RawDataFile, List<IntensityMapLayer>> byFile(
      @NotNull final Collection<IntensityMapLayer> layers) {
    final Map<RawDataFile, List<IntensityMapLayer>> byFile = new LinkedHashMap<>();
    layers.forEach(
        layer -> byFile.computeIfAbsent(layer.file(), _ -> new ArrayList<>()).add(layer));
    return byFile;
  }

  private void onViewChanged() {
    if (closed || sampledDetail == null) {
      return;
    }
    final IntensityMapSamplingTask running = task;
    if (running != null && running.isBase()) {
      // the base is still read, it requests the windows when done
      return;
    }
    if (running == null && coarser(sampledDetail, plot.detail())) {
      refreshBase();
      return;
    }
    final Map<String, IntensityMapOverlayView> views = plot.visibleArea().overlays();
    final IntensityMapProjection projection = plot.detail().projection();
    final double share = minZoomShare(projection);
    boolean changed = false;
    // the pan direction since the last check, e.g. to read ahead
    final Map<String, IntensityMapRegion> previous = Map.copyOf(lastVisible);
    lastVisible.clear();
    views.forEach((id, view) -> lastVisible.put(id, view.region()));
    this.previous = previous;
    // decision: only overlays in view are read, e.g. not the other side by side tiles when zoomed
    // into one; layers whose window still covers the view are kept
    final Map<IntensityMapLayer, IntensityMapOverlayView> needed = new LinkedHashMap<>();
    for (final IntensityMapLayer layer : layers.get()) {
      final String id = layer.id();
      final IntensityMapOverlayView view = views.get(id);
      final IntensityMapGrid base = sampled.get(id);
      if (view == null || base == null) {
        continue;
      }
      if (view.all() && baseSuffices(base, view, projection)) {
        // zoomed out, the base shows the overlay
        changed |= dropWindowIfZoomedOut(id, view, projection);
        continue;
      }
      if (covers(windows.get(id), view.region(), share)) {
        continue;
      }
      final IntensityMapCachedWindow cached = cached(id, view.region(), share);
      if (cached == null) {
        needed.put(layer, view);
      } else {
        detailed.put(id, cached.data());
        windows.put(id, cached.window());
        changed = true;
      }
    }
    if (changed) {
      publish.run();
    }
    if (needed.isEmpty()) {
      if (running != null) {
        // the view does not need the window being read
        cancel();
        plot.setLoading(null, 0);
      }
      return;
    }
    if (running != null && stillUseful(running, needed)) {
      // decision: a running read of a part in view finishes instead of restarting on every move
      // while panning; the view is checked again when it arrives
      return;
    }
    loadWindows(needed, views.size());
  }

  /**
   * @return true if the running read shows some of the visible parts at a similar zoom, so that
   * finishing it is faster than starting over
   */
  private static boolean stillUseful(@NotNull final IntensityMapSamplingTask running,
      @NotNull final Map<IntensityMapLayer, IntensityMapOverlayView> needed) {
    return needed.entrySet().stream().anyMatch(entry -> {
      final String id = entry.getKey().id();
      final IntensityMapRegion window = running.window(id);
      final IntensityMapRegion visible = entry.getValue().region();
      return running.layerIds().contains(id) && !window.isFull() && window.overlaps(visible)
          && visible.width() <= 2 * window.width() && visible.height() <= 2 * window.height()
          && window.width() <= 4 * visible.width() && window.height() <= 4 * visible.height();
    });
  }

  /**
   * @param previous visible part at the last check, null if unknown
   * @return shares of the visible size added {left, right, below, above}: more in the direction of
   * a pan
   */
  static double @NotNull [] margins(@Nullable final IntensityMapRegion previous,
      @NotNull final IntensityMapRegion visible) {
    final double[] shares = {WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN};
    if (previous == null || previous.isFull() || visible.isFull() || previous.x() == null
        || previous.y() == null || visible.x() == null || visible.y() == null) {
      return shares;
    }
    final double dx = center(visible.x()) - center(previous.x());
    final double dy = center(visible.y()) - center(previous.y());
    if (Math.abs(dx) > MIN_PAN * visible.width()) {
      shares[0] = dx < 0 ? LEAD_MARGIN : TRAIL_MARGIN;
      shares[1] = dx < 0 ? TRAIL_MARGIN : LEAD_MARGIN;
    }
    if (Math.abs(dy) > MIN_PAN * visible.height()) {
      shares[2] = dy < 0 ? LEAD_MARGIN : TRAIL_MARGIN;
      shares[3] = dy < 0 ? TRAIL_MARGIN : LEAD_MARGIN;
    }
    return shares;
  }

  private static double center(@NotNull final Range<Double> range) {
    return (range.lowerEndpoint() + range.upperEndpoint()) / 2;
  }

  /**
   * @return true if the base shows the visible part finely enough
   */
  private boolean baseSuffices(@NotNull final IntensityMapGrid base,
      @NotNull final IntensityMapOverlayView view,
      @NotNull final IntensityMapProjection projection) {
    final IntensityMapDetail detail = sampledDetail;
    return switch (projection) {
      case IntensityMapPerspective _ -> true;
      // decision: a nearly complete tile may still be larger on screen than the base was sampled
      // for, e.g. side by side after zooming in, so its bins are checked as well
      case IntensityMapTopView _ -> {
        if (detail == null) {
          yield true;
        }
        // bins of the base for dense data; sparser data would not get finer from a window either
        final var bins = detail.grid(Integer.MAX_VALUE, Integer.MAX_VALUE, base.pixels());
        yield view.width() <= bins.x() * MAX_PIXELS_PER_BIN
            && view.height() <= bins.y() * MAX_PIXELS_PER_BIN;
      }
    };
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
    start(IntensityMapSamplingTask.base(read, Map.of(), parameters, baseDetail(current.size()),
        normalization.get(), frames.get(), IntensityMapSamplingTask.ADJUSTING, this), -1);
  }

  /**
   * @return smallest share of the window that may be visible before the window is read again
   */
  private static double minZoomShare(@NotNull final IntensityMapProjection projection) {
    return switch (projection) {
      // decision: the 2D view keeps about one bin per screen pixel at every zoom. Reads are
      // throttled and run one at a time, so this stays cheap.
      case IntensityMapTopView _ -> 1 / (MAX_PIXELS_PER_BIN * (1 + 2 * WINDOW_MARGIN));
      case IntensityMapPerspective _ -> MIN_ZOOM_SHARE;
    };
  }

  /**
   * @return share of the window size added on each side of the visible range
   */
  static double margin() {
    return WINDOW_MARGIN;
  }

  /**
   * @param plotDetail detail of the plot
   * @param overlay    visible part of the overlay
   * @param enlargedX  width of the window per visible width, including its margins
   * @param enlargedY  height of the window per visible height
   * @param shown      number of overlays in view
   * @return sampling detail of a zoom window of the overlay
   */
  static @NotNull IntensityMapDetail windowDetail(@NotNull final IntensityMapDetail plotDetail,
      @NotNull final IntensityMapOverlayView overlay, final double enlargedX,
      final double enlargedY, final int shown) {
    final IntensityMapProjection projection = plotDetail.projection();
    // the margin around the visible range gets the same resolution as the visible range
    return switch (projection) {
      // decision: the flat view samples the screen size of the visible part, e.g. of a side by
      // side tile, so zoomed views are not coarser than the zoomed out view. Its budget counts
      // bins, which fits the window and the base around it.
      case IntensityMapTopView _ ->
          new IntensityMapDetail(overlay.width() * enlargedX, overlay.height() * enlargedY,
              Math.max(1, shown), projection);
      // the tilted view also shows the coarse base outside the window, both share the budget
      case IntensityMapPerspective _ ->
          new IntensityMapDetail(plotDetail.width() * enlargedX, plotDetail.height() * enlargedY,
              2 * Math.max(1, shown), projection);
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
   * Reads the windows of the layers, each around its visible part, and merges them with the data
   * shown so far.
   *
   * @param shown number of overlays in view, which share the render budget
   */
  private void loadWindows(@NotNull final Map<IntensityMapLayer, IntensityMapOverlayView> needed,
      final int shown) {
    final IntensityMapDetail view = plot.detail();
    final IntensityMapProjection projection = view.projection();
    final Map<String, IntensityMapWindowRequest> requests = new HashMap<>();
    final Map<String, IntensityMapGrid> bases = new HashMap<>();
    needed.forEach((layer, overlay) -> {
      final String id = layer.id();
      final double[] shares = switch (projection) {
        case IntensityMapTopView _ -> margins(previous.get(id), overlay.region());
        case IntensityMapPerspective _ -> margins(null, overlay.region());
      };
      requests.put(id, new IntensityMapWindowRequest(
          overlay.region().expand(shares[0], shares[1], shares[2], shares[3]),
          windowDetail(view, overlay, 1 + shares[0] + shares[1], 1 + shares[2] + shares[3],
              shown)));
      bases.put(id, mergeTarget(id, projection, shown));
    });
    cancel();
    start(IntensityMapSamplingTask.windows(byFile(needed.keySet()), Map.copyOf(bases), requests,
        parameters, view, normalization.get(), frames.get(), this), -1);
  }

  /**
   * @return the data a new window of the layer is merged into: in 2D the data shown so far, so that
   * parts read before stay sharp while panning; otherwise, or once they grow too large, the base
   */
  private @NotNull IntensityMapGrid mergeTarget(@NotNull final String id,
      @NotNull final IntensityMapProjection projection, final int shown) {
    final IntensityMapGrid base = sampled.get(id);
    final IntensityMapGrid current = detailed.get(id);
    return switch (projection) {
      case IntensityMapTopView _ ->
          current != null && (long) current.width() * current.height() <= MAX_KEPT_BINS / Math.max(
              1, shown) ? current : base;
      // the surface budget counts every bin
      case IntensityMapPerspective _ -> base;
    };
  }

  /**
   * @param progress initial progress, negative for indeterminate
   */
  private void start(@NotNull final IntensityMapSamplingTask next, final double progress) {
    task = next;
    MZmineCore.getTaskController().addTask(next, TaskPriority.HIGH);
    plot.setLoading(next.message(), progress);
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
      if (source.isBase()) {
        sampled.putAll(results);
        outside.addAll(empty);
        sampledDetail = source.detail();
        // zoom windows were merged with the replaced base
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
            windows.put(id, source.window(id));
            remember(id, source.window(id), data);
          }
        }
        publish.run();
        // the view may have moved on while reading
        onViewChanged();
      }
      if (!skipped.isEmpty()) {
        final var first = skipped.entrySet().iterator().next();
        final String message = first.getValue() + " in " + String.join(", ", skipped.keySet())
            + ". Adjust the scan selection of the module.";
        plot.setStatus(message);
        plot.setWarning(message);
      } else if (source.isBase()) {
        plot.setWarning(null);
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
        plot.setWarning(message);
      }
    });
  }

  void close() {
    closed = true;
    delay.stop();
    cancel();
  }
}
