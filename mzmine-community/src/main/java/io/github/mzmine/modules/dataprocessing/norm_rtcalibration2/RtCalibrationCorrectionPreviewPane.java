package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import static io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTCorrectionTask.findStandards;
import static io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTCorrectionTask.interpolateMissingCalibrations;
import static io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTCorrectionTask.removeNonMonotonousStandards;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.previewpane.AbstractPreviewPane;
import io.github.mzmine.project.ProjectService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RtCalibrationCorrectionPreviewPane extends AbstractPreviewPane<List<FeatureList>> {

  private static final Logger logger = Logger.getLogger(
      RtCalibrationCorrectionPreviewPane.class.getName());

  /**
   * sets chart to the center of this pane.
   *
   * @param parameters
   */
  public RtCalibrationCorrectionPreviewPane(ParameterSet parameters) {
    super(parameters);
  }

  @Override
  public @NotNull SimpleXYChart<PlotXYDataProvider> createChart() {
    final NumberFormats formats = ConfigService.getGuiFormats();
    final SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>(
        formats.unit("Original RT", "min"), formats.unit("RT shift", "min"));
    chart.setStickyZeroRangeAxis(false);
    return chart;
  }

  @Override
  public void updateChart(@NotNull List<DatasetAndRenderer> datasets,
      @NotNull SimpleXYChart<? extends PlotXYDataProvider> chart) {
    chart.applyWithNotifyChanges(false, () -> {
      chart.removeAllDatasets();
      datasets.forEach(ds -> chart.addDataset(ds.dataset(), ds.renderer()));
    });
  }

  @Override
  public @NotNull List<@NotNull DatasetAndRenderer> calculateNewDatasets(
      @Nullable List<FeatureList> flists) {
    var mzTolerance = parameters.getParameter(RTCorrectionParameters.MZTolerance).getValue();
    var rtTolerance = parameters.getParameter(RTCorrectionParameters.RTTolerance).getValue();
    var minHeight = parameters.getParameter(RTCorrectionParameters.minHeight).getValue();
    var bandwidth = parameters.getValue(RTCorrectionParameters.correctionBandwidth);
    var sampleTypeFilter = new SampleTypeFilter(
        parameters.getParameter(RTCorrectionParameters.sampleTypes).getValue());

    final List<String> errorMessages = new ArrayList<>();
    if (!parameters.checkParameterValues(errorMessages)) {
      logger.info(errorMessages.stream().collect(Collectors.joining(", ")));
      return List.of();
    }

    final List<FeatureList> referenceFlistsByNumRows = flists.stream()
        .filter(flist -> flist.getRawDataFiles().stream().allMatch(sampleTypeFilter::matches))
        .sorted(Comparator.comparingInt(FeatureList::getNumberOfRows)).toList();

    final FeatureList baseList = referenceFlistsByNumRows.getFirst();
    final Map<FeatureList, List<FeatureListRow>> mzSortedRows = new HashMap<>();
    referenceFlistsByNumRows.forEach(flist -> mzSortedRows.put(flist,
        flist.stream().sorted(Comparator.comparingDouble(FeatureListRow::getAverageMZ)).toList()));

    final List<RtStandard> goodStandards = findStandards(baseList, referenceFlistsByNumRows,
        mzSortedRows, mzTolerance, rtTolerance, minHeight);
    goodStandards.sort(Comparator.comparingDouble(RtStandard::getMedianRt));
    final List<RtStandard> monotonousStandards = removeNonMonotonousStandards(goodStandards,
        referenceFlistsByNumRows);
    final List<RtCalibrationFunction> allCalibrations = interpolateMissingCalibrations(
        referenceFlistsByNumRows, flists, ProjectService.getMetadata(), monotonousStandards,
        bandwidth);

    final List<DatasetAndRenderer> datasets = new ArrayList<>();

    for (RtCalibrationFunction cali : allCalibrations) {
      final RawDataFile file = cali.getRawDataFile();

      final var clr = ConfigService.getDefaultColorPalette().getNextColorAWT();

      if (sampleTypeFilter.matches(file)) {
        final AnyXYProvider medianVsOriginal = new AnyXYProvider(clr,
            file.getName() + " standard shift vs average RT", monotonousStandards.size(),
            i -> (double) monotonousStandards.get(i).getMedianRt(),
            i -> (monotonousStandards.get(i).standards().get(file).getAverageRT().doubleValue()
                - monotonousStandards.get(i).getMedianRt()));
        datasets.add(
            new DatasetAndRenderer(new ColoredXYDataset(medianVsOriginal, RunOption.THIS_THREAD),
                new ColoredXYShapeRenderer(true)));
      }

//      final AnyXYProvider fitDataset = new AnyXYProvider(/*file.getColorAWT()*/clr,
//          file.getName() + " correction at RT vs original RTs", file.getNumOfScans(),
//          i -> (double) file.getScan(i).getRetentionTime(),
//          i -> (double) cali.getCorrectedRtLoess(file.getScan(i).getRetentionTime()) - file.getScan(
//              i).getRetentionTime());

      final AnyXYProvider avgFitDataset = new AnyXYProvider(/*file.getColorAWT()*/clr,
          file.getName() + " fitted shift at RT vs original RTs", file.getNumOfScans(),
          i -> (double) file.getScan(i).getRetentionTime(),
          i -> (double) -(cali.getCorrectedRtMovAvg(file.getScan(i).getRetentionTime())
              - file.getScan(i).getRetentionTime()));

//      datasets.add(new DatasetAndRenderer(new ColoredXYDataset(fitDataset, RunOption.THIS_THREAD),
//          new ColoredXYLineRenderer()));

      datasets.add(new DatasetAndRenderer(new ColoredXYDataset(avgFitDataset, RunOption.THIS_THREAD),
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
