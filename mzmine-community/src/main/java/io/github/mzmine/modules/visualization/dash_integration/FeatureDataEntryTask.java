package io.github.mzmine.modules.visualization.dash_integration;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.MrmTransitionListType;
import io.github.mzmine.datamodel.otherdetectors.MrmTransition;
import io.github.mzmine.datamodel.otherdetectors.MrmTransitionList;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IntensityTimeSeriesToXYProvider;
import io.github.mzmine.gui.preferences.NumberFormats;
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
    final NumberFormats formats = ConfigService.getGuiFormats();

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
      if (file instanceof ImagingRawDataFile) {
        processed++;
        continue;
      }

      final @Nullable ModularFeature feature = (ModularFeature) row.getFeature(file);
      final Range<Double> mzRange =
          feature == null ? integrationTolerance.getToleranceRange(row.getAverageMZ())
              : feature.getRawDataPointsMZRange();

      final IonTimeSeries<? extends Scan> chromatogram = extractChromatogram(file, feature, flist,
          mzRange, extendedRtRange, row);

      final List<IntensityTimeSeriesToXYProvider> additionalData;
      if (feature != null && feature.get(MrmTransitionListType.class) != null) {
        final MrmTransitionList mrms = feature.get(MrmTransitionListType.class);
        AtomicInteger clr = new AtomicInteger(0);
        additionalData = mrms.transitions().stream().map(t -> new IntensityTimeSeriesToXYProvider(
            t.chromatogram()
                .subSeries(null, extendedRtRange.lowerEndpoint(), extendedRtRange.upperEndpoint()),
            colors.getAWT(clr.getAndIncrement()),
            "%s -> %s".formatted(formats.mz(t.q1mass()), formats.mz(t.q3mass())))).toList();
      } else {
        additionalData = List.of();
      }

      entries.add(new FeatureDataEntry(file, feature != null ? feature.getFeatureData() : null,
          chromatogram, additionalData));
      processed++;
    }
  }

  private IonTimeSeries<? extends Scan> extractChromatogram(RawDataFile file,
      @Nullable ModularFeature feature, @NotNull ModularFeatureList flist, Range<Double> mzRange,
      Range<Float> extendedRtRange, FeatureListRow row) {
    final IonTimeSeries<? extends Scan> chromatogram;

    if (feature != null && feature.get(MrmTransitionListType.class) != null) {
      final MrmTransitionList mrms = feature.get(MrmTransitionListType.class);
      final MrmTransition quant = mrms.quantifier();
      return quant.chromatogram().subSeries(null, extendedRtRange.lowerEndpoint().floatValue(),
          extendedRtRange.upperEndpoint().floatValue());
    }
    if (flist.hasFeatureType(MrmTransitionListType.class)) {
      return IonTimeSeries.EMPTY;
    }

    if (file instanceof IMSRawDataFile ims && (feature == null
        || feature.get(MobilityType.class) != null)) {
      final int previousBinningWith = BinningMobilogramDataAccess.getPreviousBinningWith(flist,
          ims.getMobilityType());
      var chrom = IonTimeSeriesUtils.extractIonMobilogramTimeSeries(
          new MobilityScanDataAccess(ims, MobilityScanDataType.MASS_LIST,
              (List<Frame>) flist.getSeletedScans(file)), mzRange, extendedRtRange,
          row.getMobilityRange(), flist.getMemoryMapStorage(),
          new BinningMobilogramDataAccess(ims, previousBinningWith));
      chromatogram = (IonTimeSeries<? extends Scan>) model.getPostProcessingMethod().apply(chrom);
    } else {
      var chrom = IonTimeSeriesUtils.extractIonTimeSeries(file,
          (List<Scan>) flist.getSeletedScans(file), mzRange, extendedRtRange,
          model.getFeatureList().getMemoryMapStorage());
      chromatogram = (IonTimeSeries<? extends Scan>) model.getPostProcessingMethod().apply(chrom);
    }
    return chromatogram;
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
