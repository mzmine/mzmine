package io.github.mzmine.modules.visualization.dash_integration;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.MrmTransitionListType;
import io.github.mzmine.datamodel.otherdetectors.MrmTransitionList;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IntensityTimeSeriesToXYProvider;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class FeatureDataEntryTask extends FxUpdateTask<IntegrationDashboardModel> {

  private final List<FeatureDataEntry> entries = new ArrayList<>();

  private long total = 0L;
  private long processed = 0L;

  protected FeatureDataEntryTask(IntegrationDashboardModel model) {
    super(FeatureDataEntryTask.class.getName(), model);
  }

  @Override
  protected void process() {

    final @NotNull ModularFeatureList flist = model.getFeatureList();
    final List<RawDataFile> files = List.copyOf(model.getSortedFiles());
    final FeatureListRow row = model.getRow();

    total = files.size();

    if (row == null) {
      return;
    }

    final Range<Float> rtRange = row.get(RTRangeType.class);
    final Range<Float> extendedRtRange = RangeUtils.rangeAround(row.getAverageRT(),
        RangeUtils.rangeLength(rtRange) * 3);
    final SimpleColorPalette colors = ConfigService.getDefaultColorPalette();
    final MZTolerance integrationTolerance = model.getIntegrationTolerance();

    for (RawDataFile file : files) {
      final @Nullable ModularFeature feature = (ModularFeature) row.getFeature(file);
      final Range<Double> mzRange =
          feature == null ? integrationTolerance.getToleranceRange(row.getAverageMZ())
              : feature.getRawDataPointsMZRange();

      final IonTimeSeries<Scan> chromatogram = IonTimeSeriesUtils.extractIonTimeSeries(file,
          (List<Scan>) flist.getSeletedScans(file), mzRange, extendedRtRange,
          model.getFeatureList().getMemoryMapStorage());

      final List<IntensityTimeSeriesToXYProvider> additionalData;
      if (feature != null && feature.get(MrmTransitionListType.class) != null) {
        final MrmTransitionList mrms = feature.get(MrmTransitionListType.class);
        AtomicInteger clr = new AtomicInteger(0);
        additionalData = mrms.transitions().stream()
            .map(t -> new IntensityTimeSeriesToXYProvider(t, colors.getAWT(clr.getAndIncrement())))
            .toList();
      } else {
        additionalData = List.of();
      }

      entries.add(new FeatureDataEntry(file, feature != null ? feature.getFeatureData() : null,
          chromatogram, additionalData));
      processed++;
    }
  }

  @Override
  protected void updateGuiModel() {
    model.featureDataEntriesProperty()
        .putAll(entries.stream().collect(Collectors.toMap(FeatureDataEntry::file, e -> e)));
  }

  @Override
  public String getTaskDescription() {
    return "Calculating data sets for integration dashboard";
  }

  @Override
  public double getFinishedPercentage() {
    return total != 0L ? (double) processed / total : 0d;
  }
}
