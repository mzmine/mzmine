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
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapDetail;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapFrameCache;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapLayer;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapSampler;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Reads the overlays of the visualizer from the raw data files, in parallel per file. Either the
 * base of the complete range, or a window whose data are merged into the bases.
 */
final class IntensityMapSamplingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(IntensityMapSamplingTask.class.getName());
  private final Map<RawDataFile, List<IntensityMapLayer>> missing;
  private final Map<String, IntensityMapGrid> shrink;
  // bases to merge window data into, empty for base reads
  private final Map<String, IntensityMapGrid> bases;
  private final ParameterSet settings;
  private final IntensityMapDetail detail;
  // full for base reads
  private final IntensityMapRegion window;
  private final ImageNormalization normalization;
  private final IntensityMapFrameCache frames;
  private final String message;
  private final IntensityMapSamplingListener listener;
  private final Map<RawDataFile, Double> fileProgress = new ConcurrentHashMap<>();
  private volatile double progress;
  private volatile double reported;

  /**
   * @param missing layers to read, by file
   * @param shrink  sampled layers that only shrink to the detail, without reading
   * @param bases   bases of the window, empty for base reads
   * @param window  data window, full for base reads
   * @param message progress message
   */
  IntensityMapSamplingTask(@NotNull final Map<RawDataFile, List<IntensityMapLayer>> missing,
      @NotNull final Map<String, IntensityMapGrid> shrink,
      @NotNull final Map<String, IntensityMapGrid> bases, @NotNull final ParameterSet settings,
      @NotNull final IntensityMapDetail detail, @NotNull final IntensityMapRegion window,
      @NotNull final ImageNormalization normalization, @NotNull final IntensityMapFrameCache frames,
      @NotNull final String message, @NotNull final IntensityMapSamplingListener listener) {
    super(null, Instant.now());
    this.missing = missing;
    this.shrink = shrink;
    this.settings = settings;
    this.detail = detail;
    this.bases = bases;
    this.window = window;
    this.normalization = normalization;
    this.frames = frames;
    this.message = message;
    this.listener = listener;
  }

  @NotNull IntensityMapRegion window() {
    return window;
  }

  @NotNull IntensityMapDetail detail() {
    return detail;
  }

  @NotNull String message() {
    return message;
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Reading 3D overlays";
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  private void advance(@NotNull final RawDataFile file, final int completed, final int total) {
    fileProgress.put(file, total == 0 ? 1 : (double) completed / total);
    progress = fileProgress.values().stream().mapToDouble(Double::doubleValue).sum() / Math.max(1,
        missing.size());
    // throttle FX updates
    if (progress - reported >= 0.02) {
      reported = progress;
      listener.progress(this, progress);
    }
  }

  @Override
  public void run() {
    if (isCanceled()) {
      listener.released(this);
      return;
    }
    setStatus(TaskStatus.PROCESSING);
    try {
      final Map<String, IntensityMapGrid> results = new ConcurrentHashMap<>();
      final Set<String> empty = ConcurrentHashMap.newKeySet();
      final Map<String, String> skipped = new ConcurrentHashMap<>();
      for (final var entry : shrink.entrySet()) {
        final IntensityMapGrid data = entry.getValue();
        final var size = detail.grid(data.width(), data.height(), data.pixels());
        results.put(entry.getKey(), data.downsample(size.x(), size.y()));
      }
      // files are independent, reading them in parallel scales with overlaid samples
      missing.entrySet().parallelStream().forEach(entry -> {
        final RawDataFile file = entry.getKey();
        final List<IntensityMapLayer> fileLayers = entry.getValue();
        final IntensityMapGrid[] data;
        try {
          data = IntensityMapSampler.sample(file, settings,
              fileLayers.stream().map(IntensityMapLayer::mzRange).toList(), window, normalization,
              frames, detail, new IntensityMapSampler.Progress() {
                @Override
                public boolean canceled() {
                  return isCanceled();
                }

                @Override
                public void advance(final int completed, final int total) {
                  IntensityMapSamplingTask.this.advance(file, completed, total);
                }
              });
        } catch (final IllegalArgumentException ex) {
          // decision: one file without matching scans must not hide all other samples
          skipped.put(file.getName(), ex.getMessage());
          fileLayers.forEach(layer -> empty.add(layer.id()));
          return;
        }
        for (int i = 0; i < data.length; i++) {
          final String id = fileLayers.get(i).id();
          if (window.isFull()) {
            if (data[i] == null) {
              empty.add(id);
            } else {
              results.put(id, data[i]);
            }
          } else {
            // no signal in the window keeps the coarse base
            final IntensityMapGrid base = bases.get(id);
            if (base != null) {
              results.put(id, data[i] == null ? base : IntensityMapGrid.merge(base, data[i]));
            }
          }
        }
      });
      if (isCanceled()) {
        listener.released(this);
        return;
      }
      listener.finished(this, results, empty, skipped);
      setStatus(TaskStatus.FINISHED);
    } catch (final RuntimeException ex) {
      if (isCanceled() || ex instanceof CancellationException
          || ex.getCause() instanceof CancellationException) {
        cancel();
        listener.released(this);
        return;
      }
      logger.log(Level.WARNING, "3D sampling failed", ex);
      error("3D sampling failed: " + ex.getMessage(), ex);
      listener.failed(this, getErrorMessage());
    }
  }
}
