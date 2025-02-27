/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.pseudospectrumvisualizer;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaMs2CorrParameters;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;

public class PseudoSpectrumVisualizerPane extends SplitPane {

  private static final Logger LOGGER = Logger.getLogger(
      PseudoSpectrumVisualizerPane.class.getName());
  private ModularFeature selectedFeature;
  private Scan pseudoScan;
  private RawDataFile rawDataFile;
  private SpectraPlot spectraPlot;
  private TICPlot ticPlot;
  @Nullable
  private Color color;


  public PseudoSpectrumVisualizerPane(ModularFeature selectedFeature, @Nullable Color color) {
    super();
    setOrientation(Orientation.VERTICAL);
    this.selectedFeature = selectedFeature;
    this.pseudoScan = selectedFeature.getMostIntenseFragmentScan();
    this.rawDataFile = selectedFeature.getRawDataFile();
    this.color = color;
    ticPlot = new TICPlot();
    ticPlot.minHeight(200);
    spectraPlot = new SpectraPlot();
    spectraPlot.minHeight(150);
    init();
  }

  private void init() {
    MZTolerance mzTolerance = extractMzToleranceFromPreviousMethods();
    SpectraVisualizerTab spectraVisualizerTab = new SpectraVisualizerTab(rawDataFile, pseudoScan,
        false);
    spectraVisualizerTab.loadRawData(pseudoScan);
    spectraPlot = spectraVisualizerTab.getSpectrumPlot();
    if (color != null) {
      spectraPlot.getXYPlot().getRenderer().setDefaultPaint(FxColorUtil.fxColorToAWT(color));
    }

    TICVisualizerTab ticVisualizerTab = new TICVisualizerTab(new RawDataFile[]{rawDataFile},
        TICPlotType.BASEPEAK, new ScanSelection(pseudoScan.getMSLevel()),
        mzTolerance.getToleranceRange(selectedFeature.getMZ()), null, null, null);

    ticPlot = ticVisualizerTab.getTICPlot();
    ticPlot.removeAllDataSets();
    ticPlot.setLegendVisible(false);
    PseudoSpectrumFeatureDataSetCalculationTask task = new PseudoSpectrumFeatureDataSetCalculationTask(
        rawDataFile, ticPlot, pseudoScan, selectedFeature, mzTolerance, color);
    MZmineCore.getTaskController().addTask(task);
    BorderPane pnWrapSpectrum = new BorderPane();
    BorderPane pnWrapChrom = new BorderPane();
    task.addTaskStatusListener((task1, newStatus, oldStatus) -> {
      if (newStatus.equals(TaskStatus.FINISHED)) {
        FxThread.runLater(() -> {
          pnWrapChrom.setCenter(ticPlot);
          pnWrapSpectrum.setCenter(spectraPlot);
          getItems().addAll(pnWrapSpectrum, pnWrapChrom);
        });
      }
    });
  }

  /*
   *  Extract mz tolerance from DIA correlation or GC clustering module
   * */
  private MZTolerance extractMzToleranceFromPreviousMethods() {

    try {
      Collection<FeatureListAppliedMethod> appliedMethods = Objects.requireNonNull(
          selectedFeature.getFeatureList()).getAppliedMethods();
        if (pseudoScan.getMSLevel() > 1) {

          // for DIA correlation
          return ParameterUtils.getValueFromAppliedMethods(appliedMethods,
                  DiaMs2CorrParameters.class, DiaMs2CorrParameters.ms2ScanToScanAccuracy)
              .orElse(new MZTolerance(0.005, 15));
        } else {

          // for GC-EI workflow use tolerance of chromatogram building, because deconvolution does
          // not use mz tol parameter
          boolean hasChromatograms = appliedMethods.stream().anyMatch(
              appliedMethod -> appliedMethod.getParameters().getClass()
                  .equals(ADAPChromatogramBuilderParameters.class));
          if (hasChromatograms) {
            return ParameterUtils.getValueFromAppliedMethods(appliedMethods,
                ADAPChromatogramBuilderParameters.class,
                ADAPChromatogramBuilderParameters.mzTolerance).orElse(new MZTolerance(0.005, 15));
          }
        }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING,
          " Could not extract previously used mz tolerance, will apply default settings. "
              + e.getMessage());
    }
    return new MZTolerance(0.005, 15);
  }

}
