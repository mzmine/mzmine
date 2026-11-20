package import_data;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.preferences.AgilentCentroidingOption;
import io.github.mzmine.gui.preferences.AgilentImportOptions;
import io.github.mzmine.gui.preferences.MassLynxImportOptions;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.gui.preferences.WatersLockmassParameters;
import io.github.mzmine.modules.io.import_rawdata_agilent_d.AgilentDataAccess;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithComboInputValue;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import testutils.MZmineTestUtil;

/**
 * Scan filter of the advanced import with the AgilentReader: for every combination of file, reader
 * option and centroiding, and every filter criterion, a filtered import must keep exactly the
 * spectra that the same filter accepts when applied to the unfiltered import. The filter therefore
 * has to see the real scan number, spectrum type, scan definition and mobility range.
 * <p>
 * Each data access launches a bridge process, so only the first {@link #NUM_SPECTRA} spectra of a
 * file are read.
 */
@TestInstance(Lifecycle.PER_CLASS)
@DisabledOnOs({OS.MAC, OS.LINUX})
public class AgilentScanFilterImportTest {

  private static final String DIR = "src/test/resources/rawdatafiles/additional/agilent/";
  private static final String LCMS_FILE = "pepmix-with-variable-transients.d";
  private static final String IMS_FILE = "ImsSynth_AllIons.d";
  private static final int NUM_SPECTRA = 40;

  /**
   * @param option            the native reader
   * @param vendorCentroiding the "Try vendor centroiding" toggle
   * @param source            the centroid source, only used with vendor centroiding
   */
  record ReaderConfig(@NotNull AgilentImportOptions option, boolean vendorCentroiding,
                      @NotNull AgilentCentroidingOption source) {

    @Override
    public @NotNull String toString() {
      return option + (vendorCentroiding ? " centroid " + source : " profile");
    }
  }

  @BeforeAll
  void init() {
    MZmineTestUtil.startMzmineCore();
  }

  static Stream<Arguments> fileAndReader() {
    final List<ReaderConfig> configs = new ArrayList<>();
    for (final AgilentImportOptions option : List.of(AgilentImportOptions.AGILENT_READER,
        AgilentImportOptions.AGILENT_READER_AUTO_CENTROID)) {
      configs.add(new ReaderConfig(option, false, AgilentCentroidingOption.PREFER_STORED));
      for (final AgilentCentroidingOption source : AgilentCentroidingOption.values()) {
        configs.add(new ReaderConfig(option, true, source));
      }
    }
    return Stream.of(LCMS_FILE, IMS_FILE)
        .flatMap(file -> configs.stream().map(config -> Arguments.of(file, config)));
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("fileAndReader")
  void filteredImportKeepsMatchingSpectra(@NotNull String fileName,
      @NotNull ReaderConfig config) {
    final File file = new File(DIR + fileName);
    Assumptions.assumeTrue(file.exists(), "example file missing");

    final List<@Nullable Scan> unfiltered = readSpectra(file, config, null);
    final List<Executable> checks = new ArrayList<>();
    for (final Entry<String, ScanSelection> filter : filters(unfiltered).entrySet()) {
      final ScanSelection selection = filter.getValue();
      final List<Integer> expected = IntStream.range(0, unfiltered.size())
          .filter(i -> unfiltered.get(i) != null && selection.matches(unfiltered.get(i))).boxed()
          .toList();
      final List<@Nullable Scan> filtered = readSpectra(file, config, processor(selection));
      final List<Integer> actual = IntStream.range(0, filtered.size())
          .filter(i -> filtered.get(i) != null).boxed().toList();
      checks.add(() -> Assertions.assertEquals(expected, actual,
          "spectra kept by filter '%s'".formatted(filter.getKey())));
    }
    Assertions.assertAll(checks);
  }

  /**
   * @return one filter per criterion, with values taken from the unfiltered spectra so that each
   * filter keeps only a part of them
   */
  private static @NotNull Map<String, ScanSelection> filters(
      @NotNull List<@Nullable Scan> unfiltered) {
    final List<Scan> spectra = unfiltered.stream().filter(Objects::nonNull).toList();
    final Range<Integer> scanNumbers = Range.closed(spectra.get(5).getScanNumber(),
        spectra.get(15).getScanNumber());
    final Range<Double> rtRange = Range.closed((double) spectra.get(10).getRetentionTime(),
        (double) spectra.get(25).getRetentionTime());

    final Map<String, ScanSelection> filters = new LinkedHashMap<>();
    filters.put("MS1", selection(null, null, null, null, PolarityType.ANY, MassSpectrumType.ANY,
        MsLevelFilter.of(1), null));
    filters.put("MS2", selection(null, null, null, null, PolarityType.ANY, MassSpectrumType.ANY,
        MsLevelFilter.of(2), null));
    filters.put("positive", selection(null, null, null, null, PolarityType.POSITIVE,
        MassSpectrumType.ANY, MsLevelFilter.ALL_LEVELS, null));
    filters.put("negative", selection(null, null, null, null, PolarityType.NEGATIVE,
        MassSpectrumType.ANY, MsLevelFilter.ALL_LEVELS, null));
    filters.put("RT " + rtRange,
        selection(null, null, rtRange, null, PolarityType.ANY, MassSpectrumType.ANY,
            MsLevelFilter.ALL_LEVELS, null));
    filters.put("scan numbers " + scanNumbers,
        selection(scanNumbers, null, null, null, PolarityType.ANY, MassSpectrumType.ANY,
            MsLevelFilter.ALL_LEVELS, null));
    filters.put("every 3rd scan",
        selection(null, 3, null, null, PolarityType.ANY, MassSpectrumType.ANY,
            MsLevelFilter.ALL_LEVELS, null));
    filters.put("profile", selection(null, null, null, null, PolarityType.ANY,
        MassSpectrumType.PROFILE, MsLevelFilter.ALL_LEVELS, null));
    filters.put("centroid", selection(null, null, null, null, PolarityType.ANY,
        MassSpectrumType.CENTROIDED, MsLevelFilter.ALL_LEVELS, null));
    filters.put("scan definition *=1*",
        selection(null, null, null, null, PolarityType.ANY, MassSpectrumType.ANY,
            MsLevelFilter.ALL_LEVELS, "*=1*"));
    if (spectra.getFirst() instanceof Frame frame) {
      final Range<Double> mobility = frame.getMobilityRange();
      filters.put("mobility within frames", selection(null, null, null, mobility, PolarityType.ANY,
          MassSpectrumType.ANY, MsLevelFilter.ALL_LEVELS, null));
      final Range<Double> outside = Range.closed(mobility.upperEndpoint() + 1,
          mobility.upperEndpoint() + 2);
      filters.put("mobility outside frames", selection(null, null, null, outside,
          PolarityType.ANY, MassSpectrumType.ANY, MsLevelFilter.ALL_LEVELS, null));
    }
    filters.put("combined MS1, positive, RT, every 2nd scan",
        selection(null, 2, rtRange, null, PolarityType.POSITIVE, MassSpectrumType.ANY,
            MsLevelFilter.of(1), null));
    return filters;
  }

  private static @NotNull ScanSelection selection(@Nullable Range<Integer> scanNumbers,
      @Nullable Integer everyNth, @Nullable Range<Double> rt, @Nullable Range<Double> mobility,
      @NotNull PolarityType polarity, @NotNull MassSpectrumType type,
      @NotNull MsLevelFilter msLevel, @Nullable String scanDefinition) {
    return new ScanSelection(scanNumbers, everyNth, rt, mobility, polarity, type, msLevel,
        scanDefinition);
  }

  private static @NotNull ScanImportProcessorConfig processor(@NotNull ScanSelection selection) {
    return AllSpectralDataImportModule.createSpectralProcessors(
        AdvancedSpectraImportParameters.create(null, null, null, null, null, selection, false));
  }

  /**
   * @return the first {@link #NUM_SPECTRA} spectra in file order, null where the import skips one
   */
  private static @NotNull List<@Nullable Scan> readSpectra(@NotNull File file,
      @NotNull ReaderConfig config, @Nullable ScanImportProcessorConfig processor) {
    try (final AgilentDataAccess access = new AgilentDataAccess(file, vendorParams(config), null,
        processor)) {
      final List<@Nullable Scan> spectra = new ArrayList<>();
      final RawDataFileImpl dataFile = access.createDataFile();
      if (access.isIms()) {
        for (int frameId = 1; frameId <= Math.min(NUM_SPECTRA, access.getFrameCount());
            frameId++) {
          spectra.add(access.readFrame((IMSRawDataFileImpl) dataFile, frameId));
        }
      } else {
        for (int id = 0; id < Math.min(NUM_SPECTRA, access.getScanCount()); id++) {
          spectra.add(access.readScan(dataFile, id));
        }
      }
      return spectra;
    }
  }

  private static @NotNull VendorImportParameters vendorParams(@NotNull ReaderConfig config) {
    return VendorImportParameters.create(config.vendorCentroiding(),
        MassLynxImportOptions.NATIVE_MZMINE_CENTROIDING, true,
        WatersLockmassParameters.createDefault(), true,
        new ComboWithComboInputValue<>(config.option(), config.source()),
        VendorImportParameters.DEFAULT_PRESSURE_COMPENSATION);
  }
}
