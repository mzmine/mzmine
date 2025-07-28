/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import static io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.ScanRtCorrectionTask.findStandards;
import static io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.ScanRtCorrectionTask.interpolateMissingCalibrations;
import static io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.ScanRtCorrectionTask.removeNonMonotonousStandards;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.JFreeChartUtils;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods.AbstractRtCorrectionFunction;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods.RtCorrectionFunctions;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.previewpane.AbstractPreviewPane;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.DefaultDrawingSupplier;

public class ScanRtCorrectionPreviewPane extends AbstractPreviewPane<List<FeatureList>> {

  private static final Logger logger = Logger.getLogger(
      ScanRtCorrectionPreviewPane.class.getName());

  private final List<Text> messages = new ArrayList<>();

  /**
   * sets chart to the center of this pane.
   *
   * @param parameters
   */
  public ScanRtCorrectionPreviewPane(ParameterSet parameters) {
    super(parameters);
  }

  @Override
  public @NotNull SimpleXYChart<PlotXYDataProvider> createChart() {
    final NumberFormats formats = ConfigService.getGuiFormats();
    final SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>(
        formats.unit("Original RT", "min"), formats.unit("RT shift", "min"));
    // add some minimum width to force the layout to get a bit bigger when preview is shown
    chart.setMinWidth(200);
    chart.setMinHeight(200);
    chart.setStickyZeroRangeAxis(false);
    return chart;
  }

  @Override
  public void updateChart(@NotNull List<DatasetAndRenderer> datasets,
      @NotNull SimpleXYChart<? extends PlotXYDataProvider> chart) {
    chart.setDatasets(datasets);

    getTopTextFlow().getChildren().setAll(messages);
  }

  @Override
  public @NotNull List<@NotNull DatasetAndRenderer> calculateNewDatasets(
      @Nullable List<FeatureList> flists) {
    messages.clear();

    assert flists != null;

    if (flists.isEmpty()) {
      messages.add(FxTexts.boldText("No feature lists selected, revise parameters. "));
      return List.of();
    }

    var mzTolerance = parameters.getParameter(RTCorrectionParameters.MZTolerance).getValue();
    var rtTolerance = parameters.getParameter(RTCorrectionParameters.RTTolerance).getValue();
    var rtMeasure = parameters.getParameter(RTCorrectionParameters.rtMeasure).getValue();
    var minHeight = parameters.getParameter(RTCorrectionParameters.minHeight).getValue();
    final ValueWithParameters<RtCorrectionFunctions> calibrationMethod = parameters.getParameter(
        RTCorrectionParameters.calibrationFunctionModule).getValueWithParameters();
    final var calibrationModuleParameters = calibrationMethod.parameters();
    final var calibrationModule = calibrationMethod.value().getModuleInstance();

    var sampleTypeFilter = new SampleTypeFilter(
        parameters.getParameter(RTCorrectionParameters.sampleTypes).getValue());

    final List<String> errorMessages = new ArrayList<>();
    if (!parameters.checkParameterValues(errorMessages)) {
      final String error = String.join(", ", errorMessages);
      logger.info(error);
      messages.add(FxTexts.boldText(error));
      return List.of();
    }

    final List<FeatureList> referenceFlistsByNumRows = flists.stream()
        .filter(flist -> flist.getRawDataFiles().stream().allMatch(sampleTypeFilter::matches))
        .sorted(Comparator.comparingInt(FeatureList::getNumberOfRows)).toList();

    if (referenceFlistsByNumRows.isEmpty()) {
      messages.add(FxTexts.boldText(
          "No feature lists selected, revise parameters like %s. ".formatted(
              RTCorrectionParameters.sampleTypes.getName())));
      return List.of();
    }

    final FeatureList baseList = referenceFlistsByNumRows.getFirst();
    final Map<FeatureList, List<FeatureListRow>> mzSortedRows = new HashMap<>();
    referenceFlistsByNumRows.forEach(flist -> mzSortedRows.put(flist,
        flist.stream().sorted(Comparator.comparingDouble(FeatureListRow::getAverageMZ)).toList()));

    final List<RtStandard> goodStandards = findStandards(baseList, referenceFlistsByNumRows,
        mzSortedRows, mzTolerance, rtTolerance, minHeight, rtMeasure);
    goodStandards.sort(Comparator.comparingDouble(rtMeasure::getRt));
    final List<RtStandard> monotonousStandards = removeNonMonotonousStandards(goodStandards,
        referenceFlistsByNumRows, rtMeasure);

    if (monotonousStandards.isEmpty()) {
      messages.add(FxTexts.boldText(
          "No monotonous standards found. Maybe widen the RT tolerance and other parameters. Make sure the feature lists contain features. "));
      return List.of();
    }
    final List<AbstractRtCorrectionFunction> allCalibrations = interpolateMissingCalibrations(
        referenceFlistsByNumRows, flists, ProjectService.getMetadata(), monotonousStandards,
        calibrationModule, rtMeasure, calibrationModuleParameters);

    // use different shapes to make it more obvious which belong together
    final DefaultDrawingSupplier drawingSupplier = JFreeChartUtils.createDefaultDrawingSupplier();
    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette().clone(true);

    final List<DatasetAndRenderer> datasets = new ArrayList<>();

    for (AbstractRtCorrectionFunction cali : allCalibrations) {
      final RawDataFile file = cali.getRawDataFile();

      // use default colors, file colors are usually all the same for QC
      final var clr = palette.getNextColorAWT();

      if (sampleTypeFilter.matches(file)) {
        final AnyXYProvider medianVsOriginal = new AnyXYProvider(clr,
            file.getName() + " standard shift vs %s RT".formatted(rtMeasure.toString()),
            monotonousStandards.size(), i -> (double) rtMeasure.getRt(monotonousStandards.get(i)),
            i -> (monotonousStandards.get(i).standards().get(file).getAverageRT().doubleValue()
                - rtMeasure.getRt(monotonousStandards.get(i))));
        datasets.add(
            new DatasetAndRenderer(new ColoredXYDataset(medianVsOriginal, RunOption.THIS_THREAD),
                new ColoredXYShapeRenderer(true, drawingSupplier.getNextShape())));
      }

      final AnyXYProvider avgFitDataset = new AnyXYProvider(clr,
          file.getName() + " fitted shift at RT vs original RTs".formatted(rtMeasure.toString()),
          file.getNumOfScans(), i -> (double) file.getScan(i).getRetentionTime(),
          i -> (double) -(cali.getCorrectedRt(file.getScan(i).getRetentionTime()) - file.getScan(i)
              .getRetentionTime()));

      datasets.add(
          new DatasetAndRenderer(new ColoredXYDataset(avgFitDataset, RunOption.THIS_THREAD),
              new ColoredXYLineRenderer()));
    }

    return datasets;
  }

  @Override
  public List<FeatureList> getValueForPreview() {
    return Arrays.stream(parameters.getParameter(RTCorrectionParameters.featureLists).getValue()
            .getMatchingFeatureLists()).sorted(Comparator.comparing(FeatureList::getNumberOfRows))
        .map(FeatureList.class::cast).toList();
  }
}
