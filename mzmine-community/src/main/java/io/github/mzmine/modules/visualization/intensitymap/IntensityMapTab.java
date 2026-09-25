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
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.intensitymap.chromatogram.IntensityMapChromatogramPane;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapDetail;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPosition;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapPlot;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapFrameCache;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapLayer;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapSampler;
import io.github.mzmine.modules.visualization.intensitymap.spectrum.IntensityMapSpectrumLookup;
import io.github.mzmine.modules.visualization.intensitymap.spectrum.IntensityMapSpectrumPane;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Owns the overlay layers (raw file and m/z range), the raw-data tasks, and the lifetime of the
 * reusable 3D plot. Sampled layers are cached, so adding or removing overlays only reads the raw
 * data of new layers.
 */
class IntensityMapTab extends MZmineTab {

  private static final Logger logger = Logger.getLogger(IntensityMapTab.class.getName());

  private final List<RawDataFile> files = new ArrayList<>();
  private final List<IntensityMapLayer> layers = new ArrayList<>();
  // coarse data of the complete range, the view when zoomed out
  private final Map<String, IntensityMapGrid> sampled = new HashMap<>();
  // base merged with full detail of the visible window after zooming in
  private final Map<String, IntensityMapGrid> detailed = new HashMap<>();
  // the window of the detailed data, full if none
  private IntensityMapRegion focus = IntensityMapRegion.FULL;
  // layers without data in their m/z range
  private final Set<String> outside = new HashSet<>();
  private static final int MAX_SPECTRA = 6;
  // share of the window size added on each side, so that small pans need no read
  private static final double WINDOW_MARGIN = 0.25;
  // the window is read again when the visible range is smaller than this share of it
  private static final double MIN_ZOOM_SHARE = 0.4;

  private final IntensityMapPlot plot;
  private final @NotNull ParameterSet parameters;
  private final @Nullable IntensityMapSpectrumLookup spectrumSource;
  private final @Nullable IntensityMapSpectrumPane spectrumPane;
  // mobility frames: the chromatogram selects the frame instead of showing spectra
  private final @Nullable IntensityMapChromatogramPane chromatogramPane;
  private final Map<RawDataFile, TICDataSet> chromatograms = new HashMap<>();
  private int chromatogramGeneration;
  // retention time of the shown frames, or a range of averaged frames; no retention time before
  // the first frame is resolved
  private @NotNull IntensityMapFrameCache frames = new IntensityMapFrameCache();
  private final @NotNull IntensityMapDimensions mode;
  // 2D view without heights
  private final boolean flat;
  private final PauseTransition detailDelay = new PauseTransition(Duration.millis(300));
  private final ChangeListener<Boolean> darkModeListener = (_, _, dark) -> updateDarkMode(dark);
  // decision: weak, tabs removed via their context menu are not closed and must not stay reachable
  private final WeakChangeListener<Boolean> weakDarkModeListener = new WeakChangeListener<>(
      darkModeListener);
  private @Nullable SamplingTask samplingTask;
  private @Nullable IntensityMapDetail sampledDetail;
  // decision: the imaging normalization and transformation only apply to imaging data
  private ImageNormalization normalization = ImageNormalization.NO_NORMALIZATION;
  private int nextLayer;
  private boolean closed;

  /**
   * @param files    at least one file, all with the same data dimensions
   * @param mzRanges initial m/z overlays for every file, e.g. of selected features. Empty shows the
   *                 complete m/z range of the parameters.
   * @param flat     2D view: a fixed top view without heights
   */
  IntensityMapTab(final RawDataFile @NotNull [] files, @NotNull final ParameterSet parameters,
      @NotNull final List<Range<Double>> mzRanges, final boolean flat) {
    super(title(files, flat), false, false);
    this.flat = flat;
    this.parameters = parameters.cloneParameterSet();
    mode = IntensityMapSampler.resolveMode(files[0],
        parameters.getValue(IntensityMapParameters.mode));
    // decision (user request): the paint scale is chosen in the viewer, it starts with the default
    // of the preferences like other mzmine heatmaps
    final SimpleColorPalette palette = ConfigService.getConfiguration()
        .getDefaultPaintScalePalette();
    plot = new IntensityMapPlot(palette, flat);
    // assumption: the preferences hold the complete list including custom paint scales
    plot.setPaintScales(paintScales(palette));
    plot.setPaintScale(palette);
    plot.setOnAddSelectedFiles(() -> addFiles(MZmineGUI.getSelectedRawDataFiles()));
    plot.setOnAddMzRanges(this::addMzRanges);
    plot.setOnResetMzRanges(this::resetMzRanges);
    if (mode == IntensityMapDimensions.IMAGING) {
      normalization = ConfigService.getConfiguration().getImageNormalization();
      plot.setIntensityNormalization(normalization);
      plot.setOnIntensityNormalizationChanged(value -> {
        normalization = value;
        load(true);
      });
    }
    if (mode == IntensityMapDimensions.MOBILITY_FRAME) {
      // decision (user request): a base peak chromatogram picks the frame by retention time
      spectrumSource = null;
      spectrumPane = null;
      chromatogramPane = new IntensityMapChromatogramPane();
      chromatogramPane.setListener(this::onFrameTimesSelected);
      plot.setDetailPane(chromatogramPane, "Base peak chromatogram");
      plot.setDetailHeader("Base peak chromatogram", IntensityMapChromatogramPane.HINT);
    } else {
      spectrumSource = new IntensityMapSpectrumLookup(this.parameters, mode);
      spectrumPane = new IntensityMapSpectrumPane();
      chromatogramPane = null;
      spectrumPane.setListener(this::onSpectrumClicked);
      // decision: Ctrl/⌘ adds, as for clicks, so a dragged window joins the shown m/z overlays
      spectrumPane.setRangeListener(range -> addMzRanges(List.of(range)));
      plot.setDetailPane(spectrumPane, "Spectrum");
      spectrumPane.descriptionProperty().subscribe(
          value -> plot.setDetailHeader("Spectrum · " + value, IntensityMapSpectrumPane.HINT));
      plot.setSliceMode(spectrumSource.sliceMode());
      plot.setOnSelectionChanged(this::showSpectra);
    }
    plot.setOnRemoveSeries(this::removeLayer);
    plot.setOnSeriesColorChanged(this::onColorChanged);
    for (final RawDataFile file : Arrays.stream(files).distinct().toList()) {
      this.files.add(file);
      if (mzRanges.isEmpty()) {
        addDefaultLayer(file);
      }
      for (final Range<Double> range : mzRanges) {
        addLayer(file, range);
      }
    }
    initTab();
    Platform.runLater(() -> load(false));
  }

  private static @NotNull String title(final RawDataFile @NotNull [] files, final boolean flat) {
    final String prefix = flat ? "2D" : "3D";
    if (files.length == 0) {
      return prefix + " visualizer";
    }
    return prefix + " " + files[0].getName() + (files.length > 1 ? " +" + (files.length - 1) : "");
  }

  private void initTab() {
    plot.setNumberFormats(ConfigService.getGuiFormats());
    plot.setUnitFormat(ConfigService.getConfiguration().getUnitFormat());
    if (mode == IntensityMapDimensions.IMAGING && !flat) {
      // decision (user request): images read best as flat log-scaled reliefs
      plot.setTransform(PaintScaleTransform.LOG10);
      plot.setHeightScale(plot.minimumHeightScale());
      // decision (user request): the weakest pixels start at the floor, not on a tall base
      plot.setHeightsFromLowest(true);
    }
    setContent(plot);
    setOnClosed(_ -> close());
    detailDelay.setOnFinished(_ -> onDetailChanged());
    plot.setDetailListener(_ -> detailDelay.playFromStart());
    ConfigService.isDarkModeProperty().addListener(weakDarkModeListener);
    updateDarkMode(ConfigService.isDarkModeProperty().get());
  }

  /**
   * @return all paint scales of the preferences, including custom ones, and the initial one
   */
  private static @NotNull List<SimpleColorPalette> paintScales(
      @NotNull final SimpleColorPalette initial) {
    final List<SimpleColorPalette> scales = new ArrayList<>(
        ConfigService.getPreferences().getParameter(MZminePreferences.defaultPaintScale)
            .getPalettes());
    if (!scales.contains(initial)) {
      scales.add(initial);
    }
    for (final SimpleColorPalette scale : SimpleColorPalette.DEFAULT_PAINT_SCALES) {
      if (!scales.contains(scale)) {
        scales.add(scale);
      }
    }
    return scales;
  }

  private void updateDarkMode(final boolean dark) {
    plot.setDarkMode(dark);
  }

  private @NotNull Color nextColor() {
    // decision: colors follow the list position, so replacing a selection keeps familiar colors
    final SimpleColorPalette colors = ConfigService.getDefaultColorPalette();
    final int used = layers.size();
    for (int i = 0; i < colors.size(); i++) {
      final Color candidate = colors.get((used + i) % colors.size());
      if (layers.stream().noneMatch(layer -> layer.color().equals(candidate))) {
        return candidate;
      }
    }
    return colors.get(used % colors.size());
  }

  private @NotNull String nextId() {
    return "layer-" + nextLayer++;
  }

  private void addDefaultLayer(@NotNull final RawDataFile file) {
    final Range<Double> range = IntensityMapSampler.defaultMzRange(file, parameters);
    if (!IntensityMapLayer.validRange(range)) {
      plot.setStatus(file.getName() + " has no data in the selected m/z range");
      return;
    }
    final Color color = nextColor();
    layers.add(new IntensityMapLayer(nextId(), file, range, color, true));
  }

  private boolean addLayer(@NotNull final RawDataFile file, @NotNull final Range<Double> range) {
    final boolean exists = layers.stream().anyMatch(
        layer -> layer.file().equals(file) && !layer.fullRange() && layer.mzRange().equals(range));
    if (exists || !IntensityMapLayer.validRange(range)) {
      return false;
    }
    final Color color = nextColor();
    layers.add(new IntensityMapLayer(nextId(), file, range, color, false));
    return true;
  }

  private void addFiles(@NotNull final Collection<? extends RawDataFile> additional) {
    if (additional.isEmpty()) {
      plot.setStatus("Select raw files in the project, then use Add selected raw files");
      return;
    }
    for (final RawDataFile file : additional) {
      if (mode != IntensityMapSampler.resolveMode(file,
          parameters.getValue(IntensityMapParameters.mode))) {
        plot.setStatus("Choose files with matching coordinate dimensions for an overlay");
        return;
      }
    }
    // new samples receive the same extraction ranges as the current overlays
    final List<Range<Double>> ranges = layers.stream().filter(layer -> !layer.fullRange())
        .map(IntensityMapLayer::mzRange).distinct().toList();
    final boolean full = layers.isEmpty() || layers.stream().anyMatch(IntensityMapLayer::fullRange);
    boolean changed = false;
    for (final RawDataFile file : additional) {
      if (files.contains(file)) {
        continue;
      }
      files.add(file);
      changed = true;
      if (full) {
        addDefaultLayer(file);
      }
      for (final Range<Double> range : ranges) {
        addLayer(file, range);
      }
    }
    if (changed) {
      load(false);
    } else {
      plot.setStatus("The selected raw files are already shown");
    }
  }

  private void addMzRanges(@NotNull final List<Range<Double>> ranges) {
    if (files.isEmpty()) {
      plot.setStatus("Add raw files first, then enter m/z values");
      return;
    }
    boolean changed = false;
    for (final RawDataFile file : files) {
      // decision: specific m/z overlays replace the all-m/z default. It would otherwise dominate
      // the shared intensity scale and hide the extracted ions.
      final boolean removed = layers.removeIf(
          layer -> layer.file().equals(file) && layer.fullRange());
      changed |= removed;
      for (final Range<Double> range : ranges) {
        changed |= addLayer(file, range);
      }
    }
    sampled.keySet().retainAll(layers.stream().map(IntensityMapLayer::id).toList());
    detailed.keySet().retainAll(sampled.keySet());
    if (changed) {
      load(false);
    } else {
      plot.setStatus("These m/z overlays are already shown");
    }
  }

  private void removeLayer(@NotNull final String id) {
    final IntensityMapLayer layer = layers.stream().filter(l -> l.id().equals(id)).findFirst()
        .orElse(null);
    if (layer == null) {
      return;
    }
    layers.remove(layer);
    sampled.remove(id);
    detailed.remove(id);
    outside.remove(id);
    if (layers.stream().noneMatch(l -> l.file().equals(layer.file()))) {
      files.remove(layer.file());
    }
    // decision: while reading, the cache may be cleared for resampling; publishing now would
    // empty the view and reset the overlay settings. The running task publishes when done.
    if (samplingTask == null) {
      publish();
    }
  }

  /**
   * Replaces all overlays by the ranges for every file.
   */
  private void setMzRanges(@NotNull final List<Range<Double>> ranges) {
    if (files.isEmpty()) {
      return;
    }
    layers.clear();
    clearSampled();
    for (final RawDataFile file : files) {
      for (final Range<Double> range : ranges) {
        addLayer(file, range);
      }
    }
    load(false);
  }

  private void resetMzRanges() {
    if (files.isEmpty()) {
      return;
    }
    if (!layers.isEmpty() && layers.stream().allMatch(IntensityMapLayer::fullRange)) {
      return;
    }
    layers.clear();
    clearSampled();
    files.forEach(this::addDefaultLayer);
    load(false);
  }

  private void onSpectrumClicked(@NotNull final IntensityMapSpectrumPane.Click click) {
    final MZTolerance tolerance = plot.mzTolerance();
    if (tolerance == null) {
      plot.setStatus("Enter a valid m/z tolerance in the overlay panel");
      return;
    }
    if (click.additive()) {
      // Ctrl/⌘ + click on a selected range removes it again
      final List<IntensityMapLayer> selected = layers.stream()
          .filter(layer -> !layer.fullRange() && layer.mzRange().contains(click.mz())).toList();
      if (!selected.isEmpty()) {
        removeLayers(selected);
        return;
      }
    }
    final double center = click.scan() == null ? click.mz()
        : IntensityMapSpectrumLookup.apex(click.scan(), tolerance.getToleranceRange(click.mz()),
            click.mz());
    final Range<Double> range = tolerance.getToleranceRange(center);
    if (click.additive()) {
      addMzRanges(List.of(range));
    } else {
      setMzRanges(List.of(range));
    }
  }

  /**
   * Removes overlays. Files without remaining overlays show the complete m/z range again.
   */
  private void removeLayers(@NotNull final List<IntensityMapLayer> removed) {
    layers.removeAll(removed);
    for (final IntensityMapLayer layer : removed) {
      sampled.remove(layer.id());
      detailed.remove(layer.id());
      outside.remove(layer.id());
    }
    for (final RawDataFile file : files) {
      if (layers.stream().noneMatch(layer -> layer.file().equals(file))) {
        addDefaultLayer(file);
      }
    }
    load(false);
  }

  private void showSpectra(@NotNull final IntensityMapPosition selection) {
    if (spectrumSource == null || spectrumPane == null) {
      return;
    }
    final List<IntensityMapSpectrumPane.Spectrum> spectra = new ArrayList<>();
    String description = "";
    for (final RawDataFile file : files) {
      if (spectra.size() == MAX_SPECTRA) {
        break;
      }
      final Scan scan = spectrumSource.scanAt(file, selection.x(), selection.y());
      if (scan == null) {
        continue;
      }
      final Color color = fileColor(file);
      spectra.add(new IntensityMapSpectrumPane.Spectrum(file.getName(), scan, color));
      if (description.isEmpty()) {
        description = spectrumSource.describe(scan);
      }
    }
    spectrumPane.setSpectra(description, spectra);
  }

  private void updateDetail(@NotNull final List<IntensityMapSeries> series) {
    updateChromatograms();
    if (spectrumPane == null) {
      return;
    }
    final List<IntensityMapSpectrumPane.Marker> markers = new ArrayList<>();
    final Set<Range<Double>> ranges = new HashSet<>();
    for (final IntensityMapLayer layer : layers) {
      if (!layer.fullRange() && ranges.add(layer.mzRange())) {
        markers.add(new IntensityMapSpectrumPane.Marker(layer.mzRange(), layer.color()));
      }
    }
    spectrumPane.setMarkers(markers);
    if (plot.getSelection() == null && !series.isEmpty()) {
      // start with the most intense position so that the spectrum is informative right away
      final IntensityMapPosition maximum = maximumPosition(series.getFirst().data());
      if (maximum != null) {
        plot.setSelection(maximum);
        showSpectra(maximum);
      }
    }
  }

  private void onFrameTimesSelected(@NotNull final Range<Float> retentionTimes) {
    frames = frames.select(retentionTimes);
    load(true);
  }

  /**
   * Shows the base peak chromatograms of all files. New files are read in the background, colors
   * follow the overlays.
   */
  private void updateChromatograms() {
    if (chromatogramPane == null) {
      return;
    }
    chromatograms.keySet().retainAll(files);
    final List<RawDataFile> missing = files.stream().filter(f -> !chromatograms.containsKey(f))
        .toList();
    showChromatograms();
    if (missing.isEmpty()) {
      return;
    }
    final int generation = ++chromatogramGeneration;
    Thread.ofVirtual().name("3D base peak chromatograms").start(() -> {
      final Map<RawDataFile, TICDataSet> results = new HashMap<>();
      try {
        for (final RawDataFile file : missing) {
          final Scan[] scans = IntensityMapSampler.scanSelection(parameters,
              IntensityMapDimensions.MOBILITY_FRAME).getMatchingScans(file);
          // the dataset computes its values on construction
          results.put(file, new TICDataSet(file, Arrays.asList(scans),
              IntensityMapSampler.defaultMzRange(file, parameters), null, TICPlotType.BASEPEAK));
        }
      } catch (final RuntimeException ex) {
        logger.log(Level.WARNING, "Cannot compute base peak chromatograms", ex);
      }
      Platform.runLater(() -> {
        if (closed || generation != chromatogramGeneration) {
          return;
        }
        results.keySet().retainAll(files);
        chromatograms.putAll(results);
        showChromatograms();
      });
    });
  }

  private void showChromatograms() {
    if (chromatogramPane == null) {
      return;
    }
    final List<IntensityMapChromatogramPane.Chromatogram> shown = new ArrayList<>();
    for (final RawDataFile file : files) {
      final TICDataSet data = chromatograms.get(file);
      if (data != null) {
        shown.add(new IntensityMapChromatogramPane.Chromatogram(data, fileColor(file)));
      }
    }
    chromatogramPane.setChromatograms(shown);
  }

  private @NotNull Color fileColor(@NotNull final RawDataFile file) {
    return layers.stream().filter(layer -> layer.file().equals(file)).map(IntensityMapLayer::color)
        .findFirst().orElse(Color.GRAY);
  }

  /**
   * Resolves the initial frame and shows the frame of the first file in the header and
   * chromatogram. Overlaid files show their frame closest to the same retention time.
   */
  private void updateFrame() {
    if (chromatogramPane == null || files.isEmpty()) {
      return;
    }
    final NumberFormats formats = ConfigService.getGuiFormats();
    final Scan[] scans = IntensityMapSampler.scanSelection(parameters,
        IntensityMapDimensions.MOBILITY_FRAME).getMatchingScans(files.getFirst());
    final Range<Float> times = frames.retentionTimes();
    String position = "no frame";
    final List<Frame> averaged = times == null || IntensityMapFrameCache.isSingle(times) ? List.of()
        : IntensityMapFrameCache.within(scans, times);
    if (averaged.size() > 1) {
      // averaged frames are merged during sampling, not on the FX thread
      chromatogramPane.setSelected(times);
      position = "RT " + formats.rt(times) + " min · " + averaged.size() + " frames averaged";
    } else {
      try {
        // decision: start with the most intense frame, so that the first view is informative
        final Frame frame = frames.frame(scans);
        if (times == null) {
          frames = frames.select(Range.singleton(frame.getRetentionTime()));
        }
        chromatogramPane.setSelected(Range.singleton(frame.getRetentionTime()));
        position =
            "RT " + formats.rt(frame.getRetentionTime()) + " min · frame #" + frame.getScanNumber();
      } catch (final IllegalArgumentException ex) {
        // no frame matches the scan selection, sampling reports it
        chromatogramPane.setSelected(times);
      }
    }
    plot.setDetailHeader("Base peak chromatogram · " + position, IntensityMapChromatogramPane.HINT);
  }

  private static @Nullable IntensityMapPosition maximumPosition(
      @NotNull final IntensityMapGrid data) {
    int best = -1;
    float maximum = -1;
    for (int row = 0; row < data.height(); row++) {
      for (int column = 0; column < data.width(); column++) {
        final float value = data.intensity(column, row);
        if (value > maximum) {
          maximum = value;
          best = row * data.width() + column;
        }
      }
    }
    return best < 0 ? null : new IntensityMapPosition(data.xValue(best % data.width()),
        data.yValue(best / data.width()));
  }

  /**
   * Keeps layer colors in sync with the color pickers, so the spectrum uses the same colors.
   */
  private void onColorChanged(@NotNull final String id, @NotNull final Color color) {
    for (int i = 0; i < layers.size(); i++) {
      final IntensityMapLayer layer = layers.get(i);
      if (layer.id().equals(id) && !layer.color().equals(color)) {
        layers.set(i,
            new IntensityMapLayer(id, layer.file(), layer.mzRange(), color, layer.fullRange()));
        updateDetail(List.of());
        final IntensityMapPosition selection = plot.getSelection();
        if (selection != null) {
          showSpectra(selection);
        }
        return;
      }
    }
  }

  private void clearSampled() {
    sampled.clear();
    detailed.clear();
    outside.clear();
    focus = IntensityMapRegion.FULL;
  }

  /**
   * Shows the visible data window in full detail once the view has settled (user request: zoom like
   * the former 2D plot instead of selecting subsets). Reading raw data is expensive, so the event
   * is debounced, only one read runs at a time, and the window is only read again when the view
   * leaves it or zooms in well beyond its resolution. Zoomed out, the base is shown without
   * reading.
   */
  private void onDetailChanged() {
    if (closed || sampledDetail == null) {
      return;
    }
    final SamplingTask running = samplingTask;
    if (running != null && running.window.isFull()) {
      // the base is still read, it requests the window when done
      return;
    }
    final IntensityMapRegion visible = plot.visibleWindow();
    if (visible.isFull()) {
      if (!focus.isFull() || running != null) {
        cancelSampling();
        plot.setLoading(null, 0);
        focus = IntensityMapRegion.FULL;
        detailed.clear();
        publish();
      }
      return;
    }
    final boolean complete = layers.stream()
        .allMatch(layer -> !sampled.containsKey(layer.id()) || detailed.containsKey(layer.id()));
    if ((complete && covers(focus, visible)) || (running != null && covers(running.window,
        visible))) {
      return;
    }
    loadWindow(visible.expand(WINDOW_MARGIN));
  }

  /**
   * @return true if the window contains the visible range at a similar resolution
   */
  private static boolean covers(@NotNull final IntensityMapRegion window,
      @NotNull final IntensityMapRegion visible) {
    return !window.isFull() && window.encloses(visible)
        && visible.width() >= window.width() * MIN_ZOOM_SHARE
        && visible.height() >= window.height() * MIN_ZOOM_SHARE;
  }

  /**
   * Reads the window of every overlay and merges it with the base.
   */
  private void loadWindow(@NotNull final IntensityMapRegion window) {
    final Map<RawDataFile, List<IntensityMapLayer>> read = new LinkedHashMap<>();
    for (final IntensityMapLayer layer : layers) {
      if (sampled.containsKey(layer.id())) {
        read.computeIfAbsent(layer.file(), _ -> new ArrayList<>()).add(layer);
      }
    }
    if (read.isEmpty()) {
      return;
    }
    final IntensityMapDetail view = plot.detail();
    // decision: the window and the coarse base outside it share the render budget
    final IntensityMapDetail detail = new IntensityMapDetail(view.width(), view.height(),
        2 * Math.max(1, layers.size()), view.flat());
    cancelSampling();
    final SamplingTask task = new SamplingTask(read, Map.of(), Map.copyOf(sampled), parameters,
        detail, window, normalization, frames, "Adjusting detail…");
    samplingTask = task;
    plot.setLoading("Adjusting detail…", -1);
    MZmineCore.getTaskController().addTask(task, TaskPriority.HIGH);
  }

  /**
   * @param all resample every layer, otherwise only layers without data
   */
  private void load(final boolean all) {
    if (closed) {
      return;
    }
    updateFrame();
    final IntensityMapDetail view = plot.detail();
    // the base covers the complete range at the resolution of the zoomed out view
    final IntensityMapDetail detail = new IntensityMapDetail(view.width(), view.height(),
        Math.max(1, layers.size()), view.flat());
    if (all) {
      clearSampled();
    }
    final Map<RawDataFile, List<IntensityMapLayer>> missing = new LinkedHashMap<>();
    for (final IntensityMapLayer layer : layers) {
      if (!sampled.containsKey(layer.id()) && !outside.contains(layer.id())) {
        missing.computeIfAbsent(layer.file(), _ -> new ArrayList<>()).add(layer);
      }
    }
    // existing layers shrink to their share of the render budget without re-reading raw data
    final Map<String, IntensityMapGrid> shrink = new HashMap<>();
    for (final var entry : sampled.entrySet()) {
      final IntensityMapGrid data = entry.getValue();
      final var size = detail.grid(data.width(), data.height(), data.pixels());
      if (size.x() < data.width() || size.y() < data.height()) {
        shrink.put(entry.getKey(), data);
      }
    }
    cancelSampling();
    if (missing.isEmpty() && shrink.isEmpty()) {
      publish();
      return;
    }
    final int count = missing.values().stream().mapToInt(List::size).sum();
    final String message =
        "Reading " + count + (count == 1 ? " overlay" : " overlays") + " from " + missing.size() + (
            missing.size() == 1 ? " sample…" : " samples…");
    final SamplingTask task = new SamplingTask(missing, shrink, Map.of(), parameters, detail,
        IntensityMapRegion.FULL, normalization, frames, message);
    samplingTask = task;
    plot.setLoading(missing.isEmpty() ? "Adjusting detail…" : message, missing.isEmpty() ? -1 : 0);
    MZmineCore.getTaskController().addTask(task, TaskPriority.HIGH);
  }

  private void cancelSampling() {
    if (samplingTask != null) {
      samplingTask.cancel();
      samplingTask = null;
    }
  }

  private void publish() {
    final List<IntensityMapSeries> series = new ArrayList<>();
    for (final IntensityMapLayer layer : layers) {
      final IntensityMapGrid data = focus.isFull() ? sampled.get(layer.id())
          : detailed.getOrDefault(layer.id(), sampled.get(layer.id()));
      if (data != null) {
        series.add(layer.toSeries(data));
      }
    }
    try {
      plot.setSeries(series);
    } catch (final IllegalArgumentException ex) {
      plot.setStatus(ex.getMessage());
    }
    updateDetail(series);
    if (series.isEmpty() && !layers.isEmpty() && !outside.isEmpty()) {
      plot.setStatus("No data in the selected m/z ranges");
    }
  }

  private void close() {
    closed = true;
    detailDelay.stop();
    cancelSampling();
    chromatogramGeneration++;
    ConfigService.isDarkModeProperty().removeListener(weakDarkModeListener);
    plot.close();
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return List.copyOf(files);
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return List.of();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return List.of();
  }

  @Override
  public void onRawDataFileSelectionChanged(
      @NotNull final Collection<? extends RawDataFile> selected) {
  }

  @Override
  public void onFeatureListSelectionChanged(
      @NotNull final Collection<? extends FeatureList> selected) {
  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      @NotNull final Collection<? extends FeatureList> selected) {
  }

  private final class SamplingTask extends AbstractTask {

    private final Map<RawDataFile, List<IntensityMapLayer>> missing;
    private final Map<String, IntensityMapGrid> shrink;
    // bases to merge window data into, empty for base reads
    private final Map<String, IntensityMapGrid> bases;
    private final ParameterSet settings;
    private final IntensityMapDetail detail;
    // full for base reads
    private final IntensityMapRegion window;
    private final ImageNormalization taskNormalization;
    private final IntensityMapFrameCache taskFrames;
    private final String message;
    private final Map<RawDataFile, Double> fileProgress = new ConcurrentHashMap<>();
    private volatile double progress;
    private volatile double reported;

    private SamplingTask(@NotNull final Map<RawDataFile, List<IntensityMapLayer>> missing,
        @NotNull final Map<String, IntensityMapGrid> shrink,
        @NotNull final Map<String, IntensityMapGrid> bases, @NotNull final ParameterSet settings,
        @NotNull final IntensityMapDetail detail, @NotNull final IntensityMapRegion window,
        @NotNull final ImageNormalization normalization,
        @NotNull final IntensityMapFrameCache frames, @NotNull final String message) {
      super(null, Instant.now());
      this.missing = missing;
      this.shrink = shrink;
      this.settings = settings;
      this.detail = detail;
      this.bases = bases;
      this.window = window;
      this.taskNormalization = normalization;
      this.taskFrames = frames;
      this.message = message;
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
        final double value = progress;
        Platform.runLater(() -> {
          if (samplingTask == this) {
            plot.setLoading(message, value);
          }
        });
      }
    }

    /**
     * Clears the loading state if this task is still the current one, e.g. after it was canceled in
     * the task manager.
     */
    private void release() {
      Platform.runLater(() -> {
        if (!closed && samplingTask == this) {
          samplingTask = null;
          plot.setLoading(null, 0);
        }
      });
    }

    @Override
    public void run() {
      if (isCanceled()) {
        release();
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
                fileLayers.stream().map(IntensityMapLayer::mzRange).toList(), window,
                taskNormalization, taskFrames, detail, new IntensityMapSampler.Progress() {
                  @Override
                  public boolean canceled() {
                    return isCanceled();
                  }

                  @Override
                  public void advance(final int completed, final int total) {
                    SamplingTask.this.advance(file, completed, total);
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
          release();
          return;
        }
        Platform.runLater(() -> {
          if (closed || samplingTask != this) {
            return;
          }
          samplingTask = null;
          if (isCanceled()) {
            plot.setLoading(null, 0);
            return;
          }
          // overlays removed while reading
          final Set<String> current = new HashSet<>(
              layers.stream().map(IntensityMapLayer::id).toList());
          results.keySet().retainAll(current);
          empty.retainAll(current);
          plot.setLoading(null, 0);
          if (window.isFull()) {
            sampled.putAll(results);
            outside.addAll(empty);
            sampledDetail = detail;
            // detail of a zoomed view belongs to the previous base
            detailed.clear();
            focus = IntensityMapRegion.FULL;
            publish();
            detailDelay.playFromStart();
          } else {
            detailed.putAll(results);
            focus = window;
            publish();
          }
          if (!skipped.isEmpty()) {
            final var first = skipped.entrySet().iterator().next();
            plot.setStatus(first.getValue() + " in " + String.join(", ", skipped.keySet())
                + ". Adjust the scan selection of the module.");
          }
        });
        setStatus(TaskStatus.FINISHED);
      } catch (final RuntimeException ex) {
        if (isCanceled() || ex instanceof CancellationException
            || ex.getCause() instanceof CancellationException) {
          cancel();
          release();
          return;
        }
        logger.log(Level.WARNING, "3D sampling failed", ex);
        error("3D sampling failed: " + ex.getMessage(), ex);
        Platform.runLater(() -> {
          if (!closed && samplingTask == this) {
            samplingTask = null;
            plot.setLoading(null, 0);
            plot.setStatus(getErrorMessage());
          }
        });
      }
    }
  }
}
