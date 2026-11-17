package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max.AdaptiveLocalMaxMassDetector;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MzIntensityDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.ContinuousRenderer;
import io.github.mzmine.util.color.ColorUtils;
import java.awt.Color;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs the mass detection of the {@link MassDetectorSetupDialog} preview off the GUI thread, like
 * the {@link MassDetectionTask}, and adds the centroids and the preprocessed (e.g., smoothed) series
 * to the plot afterwards. Some detectors scan the whole raw file before the first spectrum (e.g.,
 * the auto saturation threshold of the adaptive local maximum detector).
 */
class MassDetectorPreviewUpdateTask extends FxUpdateTask<SpectraPlot> {

  static final String NAME = "Mass detection preview";

  private final @Nullable MassDetector massDetector;
  private final @Nullable MassDetector previousDetector;
  private final @NotNull Scan scan;

  private double @Nullable [][] mzValues;
  private double @Nullable [] preprocessedMzs;
  private double @Nullable [] preprocessedIntensities;

  /**
   * @param plot             the preview plot that already shows the scan
   * @param massDetector     detector created from the current parameters, null for invalid
   *                         parameters (adds nothing but replaces older previews)
   * @param previousDetector detector of the previous preview, hands over its per file cache, or
   *                         null
   * @param scan             the scan shown in the plot
   */
  MassDetectorPreviewUpdateTask(@NotNull final SpectraPlot plot,
      @Nullable final MassDetector massDetector, @Nullable final MassDetector previousDetector,
      @NotNull final Scan scan) {
    super(NAME, plot);
    this.massDetector = massDetector;
    this.previousDetector = previousDetector;
    this.scan = scan;
  }

  @Override
  protected void process() {
    if (massDetector == null) {
      return;
    }
    // the auto saturation threshold scans the whole raw file, reuse it across parameter changes
    if (massDetector instanceof AdaptiveLocalMaxMassDetector adaptive
        && previousDetector instanceof AdaptiveLocalMaxMassDetector previous
        && adaptive != previous) {
      adaptive.takeOverFileCache(previous);
    }
    mzValues = massValues(massDetector, scan);

    // If the detector preprocessed (e.g. smoothed) the intensities, show that series as a line so
    // the user can see what maximum and edge detection ran on.
    if (massDetector instanceof PreprocessedIntensitiesProvider provider) {
      final double[] preprocessed = provider.getLastPreprocessedIntensities();
      if (preprocessed != null && preprocessed.length == scan.getNumberOfDataPoints()) {
        final double[] mzs = new double[preprocessed.length];
        for (int i = 0; i < mzs.length; i++) {
          mzs[i] = scan.getMzValue(i);
        }
        preprocessedMzs = mzs;
        preprocessedIntensities = preprocessed;
      }
    }
  }

  /**
   * Mobility scans are centroided like in the {@link MassDetectionTask}: all mobility scans of the
   * frame at once, which some detectors need (e.g., the saturation handling of the adaptive local
   * maximum detector). Only the centroids of the selected mobility scan are returned.
   *
   * @return [mzs, intensities] of the scan
   */
  private static double @NotNull [][] massValues(@NotNull final MassDetector massDetector,
      @NotNull final Scan scan) {
    if (!(scan instanceof MobilityScan mobilityScan)) {
      return massDetector.getMassValues(scan);
    }
    final List<double[][]> values = massDetector.getMobilityScanMassValues(
        mobilityScan.getFrame().getMobilityScans());
    if (massDetector instanceof PreprocessedIntensitiesProvider) {
      // the frame path leaves the preprocessed intensities of the last mobility scan, redo the
      // selected scan for the smoothed series
      massDetector.getMassValues(scan);
    }
    return values.get(mobilityScan.getMobilityScanNumber());
  }

  @Override
  protected void updateGuiModel() {
    if (mzValues == null) {
      return;
    }
    if (preprocessedMzs != null && preprocessedIntensities != null) {
      final MzIntensityDataSet preprocessedDataSet = new MzIntensityDataSet("Smoothed",
          preprocessedMzs, preprocessedIntensities);
      final Color preprocessedColor = ColorUtils.getContrastPaletteColorAWT(
          scan.getDataFile().getColorAWT(), ConfigService.getDefaultColorPalette());
      model.addDataSet(preprocessedDataSet, preprocessedColor, false,
          new ContinuousRenderer(preprocessedColor, false), false, true);
    }
    model.addDataSet(new MassListDataSet(mzValues[0], mzValues[1]),
        SpectraVisualizerTab.peaksColor, false, true);
  }

  @Override
  public String getTaskDescription() {
    return "Mass detection preview of " + scan;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }
}
