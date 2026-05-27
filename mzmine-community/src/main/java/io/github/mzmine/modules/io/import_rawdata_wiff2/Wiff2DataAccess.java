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

package io.github.mzmine.modules.io.import_rawdata_wiff2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MetadataOnlyScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.OtherFeatureUtils;
import io.github.mzmine.datamodel.features.types.otherdectectors.ChromatogramTypeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.WavelengthType;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.builders.SimpleBuildingScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DIAMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.otherdetectors.DetectorType;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFileImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesDataImpl;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCV;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.AdcChannelsDescriptions;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.BinaryData;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.CentroidOptions;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.ChannelTrace;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.ControlledVocabularyParameter;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.DataProviderGrpc;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.DataProviderGrpc.DataProviderBlockingStub;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.Experiment;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.GetAdcChannelDescriptionsRequest;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.GetChannelTracesRequest;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.GetExperimentsRequest;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.GetMrmXicRequest;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.GetSpectraRequest;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.GetWavelengthSpectraRequest;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.ListSamplesRequest;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.MassRangeConfiguration;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.MrmXic;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.Precursor;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.Sample;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.ScanWindow;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.SmoothingOptions;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.SourceFile;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.Spectrum;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.TimeRange;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.WavelengthSpectrum;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.io.File;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfDouble;
import java.lang.foreign.ValueLayout.OfInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections4.IteratorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;

public class Wiff2DataAccess implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(Wiff2DataAccess.class.getName());
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(Wiff2DataAccess.class);
  private static final OfDouble doubleLayout = ValueLayout.JAVA_DOUBLE.withOrder(
      ByteOrder.LITTLE_ENDIAN).withByteAlignment(1); // byte buffer from protobuf is not aligned.
  public static final OfInt intLayout = ValueLayout.JAVA_INT.withOrder(ByteOrder.LITTLE_ENDIAN)
      .withByteAlignment(1); // byte buffer from protobuf is not aligned.

  private final ManagedChannel channel;
  private final DataProviderBlockingStub dataProvider;
  @NotNull
  private final File file;
  private final boolean centroid;
  @NotNull
  private final ScanImportProcessorConfig scanProcessorConfig;
  /**
   * cached samples list. need to cache to properly close in {@link #close()}
   */
  private List<Sample> samples;

  public Wiff2DataAccess(@NotNull final File file, final boolean centroid,
      @NotNull final ScanImportProcessorConfig scanProcessorConfig) throws IOException {
    this.file = file;
    this.centroid = centroid;
    this.scanProcessorConfig = scanProcessorConfig;

    final ClearcoreServer server = ClearcoreServer.getOrStart();

    ManagedChannel tempChannel = null;
    int tryCount = 0;
    while (tempChannel == null) {
      try {
        tempChannel = ManagedChannelBuilder.forAddress(server.address(), server.port())
            .usePlaintext().keepAliveTimeout(90, TimeUnit.SECONDS)
            .maxInboundMessageSize(1024 * 1024 * 5).maxRetryAttempts(3).build();
        TimeUnit.MILLISECONDS.sleep(200);
        break;
      } catch (StatusRuntimeException | InterruptedException e) {
        logger.info("Could not connect to wiff2 server. Try %d/10".formatted(tryCount));
      }
      if (tryCount > 10) {
        throw new RuntimeException("Could not connect to wiff2 server after 10 tries.");
      }
      tryCount++;
    }
    channel = tempChannel;
    dataProvider = DataProviderGrpc.newBlockingStub(channel);
  }

  /**
   * Loads traces from analog channels. Differs from {@link #getAnalogTraces} in a way that these
   * traces are from another detector that could have also acquired spectral data.
   */
  public @NotNull List<OtherDataFile> getAnalogTracesFromSpectrumDetectors(@NotNull Sample sample,
      @NotNull List<Experiment> experiments, @NotNull RawDataFileImpl rawDataFile,
      MemoryMapStorage storage) {

    // wavelength spectra
    // if channel mode = true, its not a spectrum but a trace we recieve. But the only thing we have test data for at the moment.
    final GetWavelengthSpectraRequest spectraRequest = GetWavelengthSpectraRequest.newBuilder()
        .setSampleId(sample.getId()).setRange(getFullTimeRange())
        .setIsRequestingChannelModeData(true).build();

    final Map<Integer, AnalogWavelengthChannelDescription> channelNamesMap = getChannelWavelengths(
        sample);
    if (channelNamesMap.isEmpty()) {
      return List.of();
    }
    // traces since this is channel mode
    final Map<Integer, BuildingAnalogChannelTrace> channelTraceBuffer = channelNamesMap.values()
        .stream().map(
            awcd -> new BuildingAnalogChannelTrace(new FloatArrayList(), new DoubleArrayList(),
                awcd)).collect(Collectors.toMap(awcd -> awcd.description().index(), awcd -> awcd));
    if (channelTraceBuffer.isEmpty()) {
      return List.of();
    }

    final List<OtherDataFile> otherDataFiles = new ArrayList<>();
    Iterator<WavelengthSpectrum> wavelengthTraceIterator = dataProvider.getWavelengthSpectra(
        spectraRequest);
    while (wavelengthTraceIterator.hasNext()) {
      final WavelengthSpectrum wavelengthSpectrum = wavelengthTraceIterator.next();
      addChannelTraceDataForSpectrum(wavelengthSpectrum, channelTraceBuffer);
    }

    final OtherDataFileImpl wavelengthTraceFile = new OtherDataFileImpl(rawDataFile);
    final OtherTimeSeriesDataImpl otherTimeSeriesData = new OtherTimeSeriesDataImpl(
        wavelengthTraceFile);
    otherTimeSeriesData.setChromatogramType(ChromatogramType.ABSORPTION);
    wavelengthTraceFile.setDetectorType(DetectorType.UV_VIS);
    wavelengthTraceFile.setDescription("UV_VIS_CHANNELS");
    wavelengthTraceFile.setOtherTimeSeriesData(otherTimeSeriesData);
    otherTimeSeriesData.setTimeSeriesRangeLabel("Intensity");
    otherTimeSeriesData.setTimeSeriesDomainLabel("Retention time");
    otherTimeSeriesData.setTimeSeriesDomainUnit("min");

    channelTraceBuffer.values().forEach(channelTrace -> {
      SimpleOtherTimeSeries traceData = new SimpleOtherTimeSeries(storage,
          channelTrace.rts().toFloatArray(), channelTrace.intensities().toDoubleArray(),
          channelTrace.description().originalName(), otherTimeSeriesData);
      OtherFeatureImpl otherFeature = new OtherFeatureImpl(traceData);
      otherFeature.set(WavelengthType.class, channelTrace.description().wavelength());
      otherTimeSeriesData.addRawTrace(otherFeature);
    });
    otherDataFiles.add(wavelengthTraceFile);
    return otherDataFiles;
  }

  /**
   * DAD "channels" = traces are stored/parsed as wavelength spectra. An additional description
   * provides the mapping of channel index -> wavelength.
   *
   */
  private @NonNull Map<Integer, AnalogWavelengthChannelDescription> getChannelWavelengths(
      @NotNull Sample sample) {
    final AdcChannelsDescriptions adcChannelDescriptions = dataProvider.getAdcChannelDescriptions(
        GetAdcChannelDescriptionsRequest.newBuilder().setSampleId(sample.getId()).build());

    final Pattern wavelengthPattern = Pattern.compile(",\\s*(\\d+)\\s*nm");
    final Map<Integer, AnalogWavelengthChannelDescription> channelNamesMap = adcChannelDescriptions.getNamesMap()
        .entrySet().stream()
        .<Map.Entry<Integer, AnalogWavelengthChannelDescription>>mapMulti((e, c) -> {
          Matcher matcher = wavelengthPattern.matcher(e.getValue());
          if (matcher.find()) {
            c.accept(Map.entry(e.getKey(),
                new AnalogWavelengthChannelDescription(e.getKey(), e.getValue(),
                    Double.valueOf(matcher.group(1)))));
          } else {
            logger.fine(
                "Sample %s contains unknown channel data name %s".formatted(sample.toString(),
                    e.getValue()));
          }
        }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    return channelNamesMap;
  }

  private static @NotNull ChromatogramType getChromatogramTypeFromTrace(
      @NotNull ChannelTrace trace) {
    // todo: add more types. no example data yet.
    ChromatogramType chromatogramType = switch (trace.getTraceType()) {
      case "Pressure" -> ChromatogramType.PRESSURE;
      default -> ChromatogramType.UNKNOWN;
    };
    return chromatogramType;
  }

  private static @NotNull String getRangeAxisLabelFromTrace(@NotNull ChannelTrace trace) {
    return getChromatogramTypeFromTrace(trace).toString();
  }

  private static @NotNull String getRangeAxisUnitFromTrace(@NotNull ChannelTrace trace) {
    return switch (getChromatogramTypeFromTrace(trace)) {
      case TIC, UNKNOWN, FLOW_RATE, PRESSURE, ION_CURRENT, EMISSION, ABSORPTION,
           ELECTROMAGNETIC_RADIATION, BPC, SIC, SIM, MRM_SRM -> "Unknown";
    };
  }

  private static @NotNull TimeRange getFullTimeRange() {
    return TimeRange.newBuilder().setStart(0).setEnd(Double.MAX_VALUE).build();
  }

  private static @NotNull SimpleSpectralArrays getSimpleSpectralArrays(@NotNull Spectrum spectrum) {
    double[] mzs = null;
    double[] intensities = null;
    for (int i = 0; i < spectrum.getDataCount(); i++) {
      final BinaryData data = spectrum.getData(i);
      final ByteBuffer buffer = data.getValues().asReadOnlyByteBuffer();
      final MemorySegment segment = MemorySegment.ofBuffer(buffer);

      for (ControlledVocabularyParameter cv : data.getAttributesList()) {
        switch (cv.getAccession()) {
          case MzMLCV.cvMzArray -> {
            mzs = segment.toArray(doubleLayout);
          }
          case MzMLCV.cvIntensityArray -> {
            intensities = segment.toArray(doubleLayout);
          }
        }
      }
    }
    if (mzs == null || intensities == null) {
      throw new RuntimeException("mzs or intensities not set");
    }
    return new SimpleSpectralArrays(mzs, intensities);
  }

  private static @NotNull void addChannelTraceDataForSpectrum(@NotNull WavelengthSpectrum spectrum,
      Map<Integer, BuildingAnalogChannelTrace> channelTrace) {
    int[] wavelengthIndices = null;
    double[] intensities = null;
    for (int i = 0; i < spectrum.getDataCount(); i++) {
      final BinaryData data = spectrum.getData(i);
      final ByteBuffer buffer = data.getValues().asReadOnlyByteBuffer();
      final MemorySegment segment = MemorySegment.ofBuffer(buffer);

      for (ControlledVocabularyParameter cv : data.getAttributesList()) {
        switch (cv.getAccession()) {
          case MzMLCV.cvWavelengthArray -> {
            wavelengthIndices = segment.toArray(intLayout);
          }
          case MzMLCV.cvIntensityArray -> {
            intensities = segment.toArray(doubleLayout);
          }
        }
      }
    }

    for (int i = 0; i < wavelengthIndices.length; i++) {
      final int wavelengthIndex = wavelengthIndices[i];
      BuildingAnalogChannelTrace channelDescription = channelTrace.get(wavelengthIndex);
      channelDescription.rts().add((float) spectrum.getScanStartTime());
      channelDescription.intensities().add((float) intensities[i]);
    }
  }

  @Nullable
  private static MsMsInfo getMsMsInfo(@Nullable Precursor precursor,
      @NotNull Experiment experiment) {
    if (experiment.getMsLevel() < 2 || precursor == null) {
      return null;
    }

    final var isolationWindow = precursor.getIsolationWindow();
    final var ce = precursor.getCollisionEnergy();

    final ActivationMethod activationMethod = ActivationMethod.fromCvAccession(
        precursor.getDissociationMethod().getAccession());
    final float averageCe =
        (float) (ce.getCollisionEnergyRampStart() + ce.getCollisionEnergyRampEnd()) / 2;
    final double isolationTarget = isolationWindow.getIsolationWindowTarget();

    if (experiment.hasElectronKe()) {
      // this has never been the case until now, so we can ignore it for now
//      DoubleValue electronEnergy = experiment.getElectronKe();
    }

    if (Double.compare(isolationTarget, 0) == 0) {
      // ZT scan: no isolation window target set
      // Need to get range from experiment
      final ScanWindow isolationRange = experiment.getMassRanges(0).getIsolationWindow();
      final Range<Double> isolation = Range.closed(isolationRange.getStart(),
          isolationRange.getEnd());
      return new DIAMsMsInfoImpl(
          (float) (ce.getCollisionEnergyRampStart() + ce.getCollisionEnergyRampEnd()) / 2, null,
          experiment.getMsLevel(), activationMethod, isolation);
    }

    if (Double.compare(isolationTarget, 0) != 0
        && Double.compare(isolationWindow.getLowerOffset(), 0) == 0) {
      // DDA: isolation target set, offsets not set.
      // need to get offset from experiment and re-center around m/z. Is this actually correct?
      final ScanWindow isolationRange = experiment.getMassRanges(0).getIsolationWindow();
      final Range<Double> isolation = RangeUtils.rangeAround(isolationTarget,
          isolationRange.getEnd() - isolationRange.getStart());

      return new DDAMsMsInfoImpl(isolationTarget,
          precursor.getPrecursorChargeState() == 0 ? null : precursor.getPrecursorChargeState(),
          averageCe, null, null, experiment.getMsLevel(), activationMethod, isolation);
    }

    if (Double.compare(isolationTarget, 0) != 0
        && Double.compare(isolationWindow.getLowerOffset(), 0) != 0) {
      // ZENO SWATH: isolation offset and isolation target set.
      return new DIAMsMsInfoImpl(averageCe, null, experiment.getMsLevel(), activationMethod,
          Range.closed(isolationWindow.getLowerOffset(), isolationWindow.getUpperOffset()));
    }

    logger.info("Unkown MSMS type in sciex data.");
    return null;
  }

  public void loadAndAddMrms(@NotNull final Sample sample,
      @NotNull final RawDataFileImpl rawDataFile, @NotNull final List<Experiment> experiments) {
    final OtherDataFileImpl mrmsFile = new OtherDataFileImpl(rawDataFile);
    final OtherTimeSeriesDataImpl mrmsTimeSeriesData = new OtherTimeSeriesDataImpl(mrmsFile);
    mrmsTimeSeriesData.setTimeSeriesRangeUnit("counts");
    mrmsTimeSeriesData.setTimeSeriesRangeLabel("Intensity");
    mrmsTimeSeriesData.setChromatogramType(ChromatogramType.MRM_SRM);
    mrmsFile.setOtherTimeSeriesData(mrmsTimeSeriesData);
    // mrms
    for (Experiment experiment : experiments) {
      loadAndAddMrmXics(sample, experiment, mrmsTimeSeriesData);
    }
    if (mrmsTimeSeriesData.getNumberOfTimeSeries() > 0) {
      rawDataFile.addOtherDataFiles(List.of(mrmsFile));
    }
  }

  private void loadAndAddMrmXics(@NotNull final Sample sample, @NotNull final Experiment experiment,
      @NotNull final OtherTimeSeriesDataImpl timeSeriesData) {
    if (!experiment.getScanType().equals("MRM")) {
      return;
    }

    GetMrmXicRequest.Builder mrmXicRequest = GetMrmXicRequest.newBuilder()
        .setSampleId(sample.getId()).setExperimentId(experiment.getId())
        .setTimeRange(getFullTimeRange());
    for (int i = 0; i < experiment.getMassRangesCount(); i++) {
      mrmXicRequest.addMassIndexes(i);
    }

    final List<MrmXic> mrmXics = new ArrayList<>();

    Iterator<MrmXic> iterator = dataProvider.getMrmXics(mrmXicRequest.build());
    try {
      while (iterator.hasNext()) {
        mrmXics.add(iterator.next());
      }
    } catch (StatusRuntimeException e) {
      // internal sciex error, method not implemented
      logger.fine("Error while parsing MRM from experiment " + experiment.toString() + ": "
          + e.getMessage());
    }

    for (int i = 0; i < mrmXics.size(); i++) {
      final MassRangeConfiguration massRanges = experiment.getMassRanges(i);
      final ScanWindow selectionWindow = massRanges.getSelectionWindow();
      final ScanWindow isolationWindow = massRanges.getIsolationWindow();
      final double q3mass = (selectionWindow.getEnd() + selectionWindow.getStart()) / 2;
      final double q1mass = (isolationWindow.getEnd() + isolationWindow.getStart()) / 2;
      final MrmXic xic = mrmXics.get(i);

      final FloatArrayList rts = new FloatArrayList(xic.getXValuesCount());
      for (Double rt : xic.getXValuesList()) {
        rts.add(rt.floatValue());
      }
      SimpleOtherTimeSeries series = new SimpleOtherTimeSeries(
          timeSeriesData.getOtherDataFile().getCorrespondingRawDataFile().getMemoryMapStorage(),
          rts.toFloatArray(),
          xic.getYValuesList().stream().mapToDouble(Double::doubleValue).toArray(),
          "%.3f -> %.3f".formatted(q1mass, q3mass), timeSeriesData);
      final OtherFeatureImpl otherFeature = new OtherFeatureImpl(series);

      OtherFeatureUtils.applyMrmInfo(q1mass, q3mass, ActivationMethod.CID, null, otherFeature);
      timeSeriesData.addRawTrace(otherFeature);
    }

    return;
  }

  /**
   * Retrieves the analog traces for the specific sample. Does <emph>not</emph> add them to the
   * rawDataFile.
   *
   * @param sample      The sample
   * @param rawDataFile The data file to assoiciate them with. Traces are not added.
   * @return The loaded traces.
   */
  @NotNull List<@NotNull OtherDataFile> getAnalogTraces(Sample sample,
      RawDataFileImpl rawDataFile) {
    GetChannelTracesRequest tracesRequest = GetChannelTracesRequest.newBuilder()
        .setSampleId(sample.getId()).build();
    Iterator<ChannelTrace> tracesIterator = dataProvider.getChannelTraces(tracesRequest);

    try {
      if (!tracesIterator.hasNext()) {
        logger.info("File: %s\tSample: %s\tdoes not contain any analog traces.".formatted(
            rawDataFile.getName(), sample.getSampleName()));
        return List.of();
      }
    } catch (StatusRuntimeException e) {
      // has no .timeseries file. Skip.
      return List.of();
    }

    final Map<String, OtherDataFileImpl> traceTypeFileMap = new HashMap<>();
    while (tracesIterator.hasNext()) {
      final ChannelTrace trace = tracesIterator.next();
      final OtherDataFileImpl otherFile = traceTypeFileMap.computeIfAbsent(trace.getTraceType(),
          _ -> new OtherDataFileImpl(rawDataFile));
      final OtherTimeSeriesDataImpl timeSeriesData = otherFile.getOtherTimeSeriesData() != null
          ? (OtherTimeSeriesDataImpl) otherFile.getOtherTimeSeriesData()
          : new OtherTimeSeriesDataImpl(otherFile);
      otherFile.setOtherTimeSeriesData(timeSeriesData);

      final ChromatogramType chromatogramType = getChromatogramTypeFromTrace(trace);
      timeSeriesData.setTimeSeriesRangeLabel(getRangeAxisLabelFromTrace(trace));
      timeSeriesData.setTimeSeriesRangeUnit(getRangeAxisUnitFromTrace(trace));

      final SimpleOtherTimeSeries timeSeries = new SimpleOtherTimeSeries(
          rawDataFile.getMemoryMapStorage(), ConversionUtils.convertDoublesToFloats(
          trace.getXValuesList().stream().mapToDouble(Double::doubleValue).toArray()),
          trace.getYValuesList().stream().mapToDouble(Double::doubleValue).toArray(),
          trace.getName(), timeSeriesData);

      final OtherFeature otherFeature = new OtherFeatureImpl(timeSeries);
      otherFeature.set(ChromatogramTypeType.class, chromatogramType);
      timeSeriesData.addRawTrace(otherFeature);

//      logger.info(trace.toString());
    }
    return new ArrayList<>(traceTypeFileMap.values());
  }

  @NotNull List<Sample> getSamples() {

    if (samples == null) {
      final ListSamplesRequest samplesRequest = ListSamplesRequest.newBuilder()
          .setAbsolutePathToWiffFile(file.getAbsolutePath()).setSkipCorrupted(true).build();
      final Iterator<Sample> samplesDescriptions = dataProvider.getSamplesDescriptions(
          samplesRequest);
      samples = IteratorUtils.toList(samplesDescriptions);
    }

    return samples;
  }

  List<Experiment> getExperiments(Sample sample) {
    final GetExperimentsRequest r = GetExperimentsRequest.newBuilder().setSampleId(sample.getId())
        .build();
    final List<Experiment> experiments = IteratorUtils.toList(dataProvider.getExperiments(r));
//    logger.info(experiments.toString());
    return experiments;
  }

  Iterator<Spectrum> getSpectrumIterator(@NotNull final Sample sample,
      @NotNull final Experiment experiment) {
    if (experiment.getScanType().equals("MRM")) {
      return EmptyIterator.INSTANCE;
    }

    final TimeRange timeRange = getTimeRangeFromProcessor();

    GetSpectraRequest r = GetSpectraRequest.newBuilder() //
        .setSampleId(sample.getId()) //
        .setExperimentId(experiment.getId()) //
        .setRange(timeRange).setConvertToCentroid(centroid) //
        .setSmoothingOption(SmoothingOptions.Moderate) //
        .setCentroidOption(CentroidOptions.IntensitySumAbove50Percent) //
        .setIncludeIsolatedPointsAsPeaks(false) //
        .build();
    return dataProvider.getSpectra(r);
  }

  private @NotNull TimeRange getTimeRangeFromProcessor() {
    TimeRange timeRange;
    if (scanProcessorConfig.scanFilter().isActiveFilter()) {
      Range<Double> filterRtRange = scanProcessorConfig.scanFilter().getScanRTRange();
      if (filterRtRange != null) {
        timeRange = TimeRange.newBuilder().setStart(filterRtRange.lowerEndpoint())
            .setEnd(filterRtRange.upperEndpoint()).build();
      } else {
        timeRange = getFullTimeRange();
      }
    } else {
      timeRange = getFullTimeRange();
    }
    return timeRange;
  }

  /**
   *
   * @return Null if the spectrum does not match the {@link Wiff2DataAccess#scanProcessorConfig},
   * the scan otherwise.
   */
  @Nullable SimpleScan spectrumToMzmineScan(@NotNull final RawDataFile file, @NotNull Sample sample,
      @NotNull Experiment experiment, @NotNull final Spectrum spectrum) {

    final int scanId = Integer.parseInt(spectrum.getId());
    final int msLevel = experiment.getMsLevel();
    final float rt = (float) spectrum.getScanStartTime();
    final @Nullable Precursor precursor = spectrum.getPrecursor();

    final MsMsInfo msmsInfo = getMsMsInfo(precursor, experiment);

    final MetadataOnlyScan metadataScan = new SimpleBuildingScan(scanId, msLevel,
        experiment.getIsPositivePolarityScan() ? PolarityType.POSITIVE : PolarityType.NEGATIVE,
        !centroid && !experiment.getIsDataInCentroidFormat() ? MassSpectrumType.PROFILE
            : MassSpectrumType.CENTROIDED, rt, -1, 0);

    if (!scanProcessorConfig.scanFilter().matches(metadataScan)) {
      return null;
    }

    final SimpleSpectralArrays spectralData = scanProcessorConfig.processor()
        .processScan(metadataScan, getSimpleSpectralArrays(spectrum));
    if (spectralData.getNumberOfDataPoints() == 0 && msLevel >= 2 && msmsInfo != null
        && msmsInfo.getActivationEnergy() != null && msmsInfo.getActivationEnergy() == 0f) {
      // heuristic: for wiff1 data the ms2 scans may be empty if not enough precursors were found.
      // filter them out.
      return null;
    }
    final ScanWindow massRange = experiment.getMassRanges(0).getSelectionWindow();

    StringBuilder scanDesc = new StringBuilder();
    scanDesc.append("Scan=").append(scanId);
    scanDesc.append(" Exp=").append(experiment.getId());
    if (experiment.hasZenoMode()) {
      scanDesc.append(" Zeno=").append(experiment.getZenoMode().toString());
    }

    return new SimpleScan(file, metadataScan.getScanNumber(), metadataScan.getMSLevel(),
        metadataScan.getRetentionTime(), msmsInfo, spectralData.mzs(), spectralData.intensities(),
        metadataScan.getSpectrumType(), metadataScan.getPolarity(), scanDesc.toString(),
        Range.closed(massRange.getStart(), massRange.getEnd()));

  }

  @Override
  public void close() throws Exception {

    //dataProvider.closeFile(
    //    SourceFile.newBuilder().setLocation(file.getParentFile().toURI().toString())
    //        .setName(file.getName()).build());
    List<SourceFile> sources = samples.stream().flatMap(s -> s.getSourcesList().stream()).distinct()
        .toList();
    for (SourceFile source : sources) {
      dataProvider.closeFile(source);
    }
    channel.shutdown();
//    ClearcoreServer.terminateSeverIfRunning();
  }
}
