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

package io.github.mzmine.modules.visualization.surface3d;

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
import io.github.mzmine.modules.visualization.surface3d.chromatogram.Surface3DChromatogramPane;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DData;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DDetail;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DRegion;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DSelection;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DSeries;
import io.github.mzmine.modules.visualization.surface3d.plot.Surface3DPlot;
import io.github.mzmine.modules.visualization.surface3d.sampling.Surface3DFrames;
import io.github.mzmine.modules.visualization.surface3d.sampling.Surface3DLayer;
import io.github.mzmine.modules.visualization.surface3d.sampling.Surface3DSampler;
import io.github.mzmine.modules.visualization.surface3d.spectrum.Surface3DSpectrumPane;
import io.github.mzmine.modules.visualization.surface3d.spectrum.Surface3DSpectrumSource;
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
class Surface3DVisualizerTab extends MZmineTab {

  private static final Logger logger = Logger.getLogger(Surface3DVisualizerTab.class.getName());

  private final List<RawDataFile> files = new ArrayList<>();
  private final List<Surface3DLayer> layers = new ArrayList<>();
  private final Map<String, Surface3DData> sampled = new HashMap<>();
  // layers without data inside the current region
  private final Set<String> outside = new HashSet<>();
  private static final int MAX_SPECTRA = 6;

  private final Surface3DPlot plot;
  private final @NotNull ParameterSet parameters;
  private final @Nullable Surface3DSpectrumSource spectrumSource;
  private final @Nullable Surface3DSpectrumPane spectrumPane;
  // mobility frames: the chromatogram selects the frame instead of showing spectra
  private final @Nullable Surface3DChromatogramPane chromatogramPane;
  private final Map<RawDataFile, TICDataSet> chromatograms = new HashMap<>();
  private int chromatogramGeneration;
  // retention time of the shown frames, or a range of averaged frames; no retention time before
  // the first frame is resolved
  private @NotNull Surface3DFrames frames = new Surface3DFrames();
  private final @NotNull Surface3DDataMode mode;
  private final PauseTransition detailDelay = new PauseTransition(Duration.millis(500));
  private final ChangeListener<Boolean> darkModeListener = (_, _, dark) -> updateDarkMode(dark);
  // decision: weak, tabs removed via their context menu are not closed and must not stay reachable
  private final WeakChangeListener<Boolean> weakDarkModeListener = new WeakChangeListener<>(
      darkModeListener);
  private @Nullable SamplingTask samplingTask;
  private @Nullable Surface3DDetail sampledDetail;
  private Surface3DRegion region = Surface3DRegion.FULL;
  // decision: the imaging normalization and transformation only apply to imaging data
  private ImageNormalization normalization = ImageNormalization.NO_NORMALIZATION;
  private int nextLayer;
  private boolean closed;

  /**
   * @param files    at least one file, all with the same data dimensions
   * @param mzRanges initial m/z overlays for every file, e.g. of selected features. Empty shows
   *                 the complete m/z range of the parameters.
   */
  Surface3DVisualizerTab(final RawDataFile @NotNull [] files,
      @NotNull final ParameterSet parameters, @NotNull final List<Range<Double>> mzRanges) {
    super(title(files), false, false);
    this.parameters = parameters.cloneParameterSet();
    mode = Surface3DSampler.resolveMode(files[0],
        parameters.getValue(Surface3DVisualizerParameters.mode));
    final SimpleColorPalette palette = parameters.getValue(Surface3DVisualizerParameters.palette);
    plot = new Surface3DPlot(palette);
    // assumption: the preferences hold the complete list including custom paint scales
    plot.setPaintScales(paintScales(List.of(palette)));
    plot.setPaintScale(palette);
    plot.setOnAddSelectedFiles(() -> addFiles(MZmineGUI.getSelectedRawDataFiles()));
    plot.setOnAddMzRanges(this::addMzRanges);
    plot.setOnResetMzRanges(this::resetMzRanges);
    if (mode == Surface3DDataMode.IMAGING) {
      normalization = ConfigService.getConfiguration().getImageNormalization();
      plot.setIntensityNormalization(normalization);
      plot.setOnIntensityNormalizationChanged(value -> {
        normalization = value;
        load(true);
      });
    }
    if (mode == Surface3DDataMode.MOBILITY_FRAME) {
      // decision (user request): a base peak chromatogram picks the frame by retention time
      spectrumSource = null;
      spectrumPane = null;
      chromatogramPane = new Surface3DChromatogramPane();
      chromatogramPane.setListener(this::onFrameTimesSelected);
      plot.setDetailPane(chromatogramPane, "Base peak chromatogram");
      plot.setDetailHeader("Base peak chromatogram", Surface3DChromatogramPane.HINT);
    } else {
      spectrumSource = new Surface3DSpectrumSource(this.parameters, mode);
      spectrumPane = new Surface3DSpectrumPane();
      chromatogramPane = null;
      spectrumPane.setListener(this::onSpectrumClicked);
      // decision: Ctrl/⌘ adds, as for clicks, so a dragged window joins the shown m/z overlays
      spectrumPane.setRangeListener(range -> addMzRanges(List.of(range)));
      plot.setDetailPane(spectrumPane, "Spectrum");
      spectrumPane.descriptionProperty().subscribe(value -> plot.setDetailHeader(
          "Spectrum · " + value, Surface3DSpectrumPane.HINT));
      plot.setSliceMode(spectrumSource.sliceMode());
      plot.setOnSelectionChanged(this::showSpectra);
    }
    plot.setOnRemoveSeries(this::removeLayer);
    plot.setOnSeriesColorChanged(this::onColorChanged);
    plot.setOnRegionChanged(this::setRegion);
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

  private static @NotNull String title(final RawDataFile @NotNull [] files) {
    if (files.length == 0) {
      return "3D visualizer";
    }
    return "3D " + files[0].getName() + (files.length > 1 ? " +" + (files.length - 1) : "");
  }

  private void initTab() {
    plot.setNumberFormats(ConfigService.getGuiFormats());
    plot.setUnitFormat(ConfigService.getConfiguration().getUnitFormat());
    if (mode == Surface3DDataMode.IMAGING) {
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
   * @return all paint scales of the preferences, including custom ones, and the given ones
   */
  private static @NotNull List<SimpleColorPalette> paintScales(
      @NotNull final List<SimpleColorPalette> additional) {
    final List<SimpleColorPalette> scales = new ArrayList<>(
        ConfigService.getPreferences().getParameter(MZminePreferences.defaultPaintScale)
            .getPalettes());
    for (final SimpleColorPalette scale : additional) {
      if (!scales.contains(scale)) {
        scales.add(scale);
      }
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
    final Range<Double> range = Surface3DSampler.defaultMzRange(file, parameters);
    if (!Surface3DLayer.validRange(range)) {
      plot.setStatus(file.getName() + " has no data in the selected m/z range");
      return;
    }
    final Color color = nextColor();
    layers.add(new Surface3DLayer(nextId(), file, range, color, true));
  }

  private boolean addLayer(@NotNull final RawDataFile file, @NotNull final Range<Double> range) {
    final boolean exists = layers.stream().anyMatch(
        layer -> layer.file().equals(file) && !layer.fullRange() && layer.mzRange().equals(range));
    if (exists || !Surface3DLayer.validRange(range)) {
      return false;
    }
    final Color color = nextColor();
    layers.add(new Surface3DLayer(nextId(), file, range, color, false));
    return true;
  }

  private void addFiles(@NotNull final Collection<? extends RawDataFile> additional) {
    if (additional.isEmpty()) {
      plot.setStatus("Select raw files in the project, then use Add selected raw files");
      return;
    }
    for (final RawDataFile file : additional) {
      if (mode != Surface3DSampler.resolveMode(file,
          parameters.getValue(Surface3DVisualizerParameters.mode))) {
        plot.setStatus("Choose files with matching coordinate dimensions for an overlay");
        return;
      }
    }
    // new samples receive the same extraction ranges as the current overlays
    final List<Range<Double>> ranges = layers.stream().filter(layer -> !layer.fullRange())
        .map(Surface3DLayer::mzRange).distinct().toList();
    final boolean full = layers.isEmpty() || layers.stream().anyMatch(Surface3DLayer::fullRange);
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
    sampled.keySet().retainAll(layers.stream().map(Surface3DLayer::id).toList());
    if (changed) {
      load(false);
    } else {
      plot.setStatus("These m/z overlays are already shown");
    }
  }

  private void removeLayer(@NotNull final String id) {
    final Surface3DLayer layer = layers.stream().filter(l -> l.id().equals(id)).findFirst()
        .orElse(null);
    if (layer == null) {
      return;
    }
    layers.remove(layer);
    sampled.remove(id);
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
    sampled.clear();
    outside.clear();
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
    if (!layers.isEmpty() && layers.stream().allMatch(Surface3DLayer::fullRange)) {
      return;
    }
    layers.clear();
    sampled.clear();
    outside.clear();
    files.forEach(this::addDefaultLayer);
    load(false);
  }

  private void onSpectrumClicked(@NotNull final Surface3DSpectrumPane.Click click) {
    final MZTolerance tolerance = plot.mzTolerance();
    if (tolerance == null) {
      plot.setStatus("Enter a valid m/z tolerance in the overlay panel");
      return;
    }
    if (click.additive()) {
      // Ctrl/⌘ + click on a selected range removes it again
      final List<Surface3DLayer> selected = layers.stream()
          .filter(layer -> !layer.fullRange() && layer.mzRange().contains(click.mz())).toList();
      if (!selected.isEmpty()) {
        removeLayers(selected);
        return;
      }
    }
    final double center = click.scan() == null ? click.mz()
        : Surface3DSpectrumSource.apex(click.scan(), tolerance.getToleranceRange(click.mz()),
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
  private void removeLayers(@NotNull final List<Surface3DLayer> removed) {
    layers.removeAll(removed);
    for (final Surface3DLayer layer : removed) {
      sampled.remove(layer.id());
      outside.remove(layer.id());
    }
    for (final RawDataFile file : files) {
      if (layers.stream().noneMatch(layer -> layer.file().equals(file))) {
        addDefaultLayer(file);
      }
    }
    load(false);
  }

  private void showSpectra(@NotNull final Surface3DSelection selection) {
    if (spectrumSource == null || spectrumPane == null) {
      return;
    }
    final List<Surface3DSpectrumPane.Spectrum> spectra = new ArrayList<>();
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
      spectra.add(new Surface3DSpectrumPane.Spectrum(file.getName(), scan, color));
      if (description.isEmpty()) {
        description = spectrumSource.describe(scan);
      }
    }
    spectrumPane.setSpectra(description, spectra);
  }

  private void updateDetail(@NotNull final List<Surface3DSeries> series) {
    updateChromatograms();
    if (spectrumPane == null) {
      return;
    }
    final List<Surface3DSpectrumPane.Marker> markers = new ArrayList<>();
    final Set<Range<Double>> ranges = new HashSet<>();
    for (final Surface3DLayer layer : layers) {
      if (!layer.fullRange() && ranges.add(layer.mzRange())) {
        markers.add(new Surface3DSpectrumPane.Marker(layer.mzRange(), layer.color()));
      }
    }
    spectrumPane.setMarkers(markers);
    if (plot.getSelection() == null && !series.isEmpty()) {
      // start with the most intense position so that the spectrum is informative right away
      final Surface3DSelection maximum = maximumPosition(series.getFirst().data());
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
          final Scan[] scans = Surface3DSampler.scanSelection(parameters,
              Surface3DDataMode.MOBILITY_FRAME).getMatchingScans(file);
          // the dataset computes its values on construction
          results.put(file, new TICDataSet(file, Arrays.asList(scans),
              Surface3DSampler.defaultMzRange(file, parameters), null, TICPlotType.BASEPEAK));
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
    final List<Surface3DChromatogramPane.Chromatogram> shown = new ArrayList<>();
    for (final RawDataFile file : files) {
      final TICDataSet data = chromatograms.get(file);
      if (data != null) {
        shown.add(new Surface3DChromatogramPane.Chromatogram(data, fileColor(file)));
      }
    }
    chromatogramPane.setChromatograms(shown);
  }

  private @NotNull Color fileColor(@NotNull final RawDataFile file) {
    return layers.stream().filter(layer -> layer.file().equals(file)).map(Surface3DLayer::color)
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
    final Scan[] scans = Surface3DSampler.scanSelection(parameters,
        Surface3DDataMode.MOBILITY_FRAME).getMatchingScans(files.getFirst());
    final Range<Float> times = frames.retentionTimes();
    String position = "no frame";
    final List<Frame> averaged = times == null || Surface3DFrames.isSingle(times) ? List.of()
        : Surface3DFrames.within(scans, times);
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
        position = "RT " + formats.rt(frame.getRetentionTime()) + " min · frame #"
            + frame.getScanNumber();
      } catch (final IllegalArgumentException ex) {
        // no frame matches the scan selection, sampling reports it
        chromatogramPane.setSelected(times);
      }
    }
    plot.setDetailHeader("Base peak chromatogram · " + position, Surface3DChromatogramPane.HINT);
  }

  private static @Nullable Surface3DSelection maximumPosition(@NotNull final Surface3DData data) {
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
    return best < 0 ? null : new Surface3DSelection(data.xValue(best % data.width()),
        data.yValue(best / data.width()));
  }

  /**
   * Keeps layer colors in sync with the color pickers, so the spectrum uses the same colors.
   */
  private void onColorChanged(@NotNull final String id, @NotNull final Color color) {
    for (int i = 0; i < layers.size(); i++) {
      final Surface3DLayer layer = layers.get(i);
      if (layer.id().equals(id) && !layer.color().equals(color)) {
        layers.set(i, new Surface3DLayer(id, layer.file(), layer.mzRange(), color,
            layer.fullRange()));
        updateDetail(List.of());
        final Surface3DSelection selection = plot.getSelection();
        if (selection != null) {
          showSpectra(selection);
        }
        return;
      }
    }
  }

  private void setRegion(@NotNull final Surface3DRegion region) {
    this.region = region;
    plot.setRegion(region);
    load(true);
  }

  private void onDetailChanged() {
    final Surface3DDetail previous = sampledDetail;
    if (closed || previous == null || samplingTask != null) {
      return;
    }
    // only a view-limited grid gains detail from a larger view or zoom
    final boolean viewLimited = sampled.values().stream().anyMatch(Surface3DData::viewLimited);
    final Surface3DDetail detail = plot.detail();
    if (viewLimited && (detail.width() * detail.zoom() > previous.width() * previous.zoom() * 1.2
        || detail.height() * detail.zoom() > previous.height() * previous.zoom() * 1.2)) {
      load(true);
    }
  }

  /**
   * @param all resample every layer, otherwise only layers without data
   */
  private void load(final boolean all) {
    if (closed) {
      return;
    }
    updateFrame();
    final Surface3DDetail view = plot.detail();
    final Surface3DDetail detail = new Surface3DDetail(view.width(), view.height(), view.zoom(),
        Math.max(1, layers.size()));
    if (all) {
      sampled.clear();
      outside.clear();
    }
    final Map<RawDataFile, List<Surface3DLayer>> missing = new LinkedHashMap<>();
    for (final Surface3DLayer layer : layers) {
      if (!sampled.containsKey(layer.id()) && !outside.contains(layer.id())) {
        missing.computeIfAbsent(layer.file(), _ -> new ArrayList<>()).add(layer);
      }
    }
    // existing layers shrink to their share of the render budget without re-reading raw data
    final Map<String, Surface3DData> shrink = new HashMap<>();
    for (final var entry : sampled.entrySet()) {
      final Surface3DData data = entry.getValue();
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
    final String message = "Reading " + count + (count == 1 ? " overlay" : " overlays") + " from "
        + missing.size() + (missing.size() == 1 ? " sample…" : " samples…");
    final SamplingTask task = new SamplingTask(missing, shrink, parameters, detail, region,
        normalization, frames, message);
    samplingTask = task;
    plot.setLoading(missing.isEmpty() ? "Adjusting detail…" : message,
        missing.isEmpty() ? -1 : 0);
    MZmineCore.getTaskController().addTask(task, TaskPriority.HIGH);
  }

  private void cancelSampling() {
    if (samplingTask != null) {
      samplingTask.cancel();
      samplingTask = null;
    }
  }

  private void publish() {
    final List<Surface3DSeries> series = new ArrayList<>();
    for (final Surface3DLayer layer : layers) {
      final Surface3DData data = sampled.get(layer.id());
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
      plot.setStatus("No data inside the selected region. Use Full range to show all data.");
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

    private final Map<RawDataFile, List<Surface3DLayer>> missing;
    private final Map<String, Surface3DData> shrink;
    private final ParameterSet settings;
    private final Surface3DDetail detail;
    private final Surface3DRegion taskRegion;
    private final ImageNormalization taskNormalization;
    private final Surface3DFrames taskFrames;
    private final String message;
    private final Map<RawDataFile, Double> fileProgress = new ConcurrentHashMap<>();
    private volatile double progress;
    private volatile double reported;

    private SamplingTask(@NotNull final Map<RawDataFile, List<Surface3DLayer>> missing,
        @NotNull final Map<String, Surface3DData> shrink, @NotNull final ParameterSet settings,
        @NotNull final Surface3DDetail detail, @NotNull final Surface3DRegion region,
        @NotNull final ImageNormalization normalization,
        @NotNull final Surface3DFrames frames, @NotNull final String message) {
      super(null, Instant.now());
      this.missing = missing;
      this.shrink = shrink;
      this.settings = settings;
      this.detail = detail;
      this.taskRegion = region;
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
      progress = fileProgress.values().stream().mapToDouble(Double::doubleValue).sum()
          / Math.max(1, missing.size());
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
     * Clears the loading state if this task is still the current one, e.g. after it was canceled
     * in the task manager.
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
        final Map<String, Surface3DData> results = new ConcurrentHashMap<>();
        final Set<String> empty = ConcurrentHashMap.newKeySet();
        final Map<String, String> skipped = new ConcurrentHashMap<>();
        for (final var entry : shrink.entrySet()) {
          final Surface3DData data = entry.getValue();
          final var size = detail.grid(data.width(), data.height(), data.pixels());
          results.put(entry.getKey(), data.downsample(size.x(), size.y()));
        }
        // files are independent, reading them in parallel scales with overlaid samples
        missing.entrySet().parallelStream().forEach(entry -> {
          final RawDataFile file = entry.getKey();
          final List<Surface3DLayer> fileLayers = entry.getValue();
          final Surface3DData[] data;
          try {
            data = Surface3DSampler.sample(file, settings,
                fileLayers.stream().map(Surface3DLayer::mzRange).toList(),
                taskRegion, taskNormalization, taskFrames, detail,
                new Surface3DSampler.Progress() {
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
            if (data[i] == null) {
              empty.add(fileLayers.get(i).id());
            } else {
              results.put(fileLayers.get(i).id(), data[i]);
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
          final Set<String> current = new HashSet<>(layers.stream().map(Surface3DLayer::id)
              .toList());
          results.keySet().retainAll(current);
          empty.retainAll(current);
          sampled.putAll(results);
          outside.addAll(empty);
          sampledDetail = detail;
          plot.setLoading(null, 0);
          publish();
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
