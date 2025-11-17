package resolver_tests;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet.AdvancedWaveletParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet.WaveletResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet.WaveletResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet.WaveletResolverParameters.NoiseCalculation;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;
import testutils.TaskResult;

@Disabled
public class ResolverTests {

  private static final Logger logger = Logger.getLogger(ResolverTests.class.getName());

  static final MemoryMapStorage storage = MemoryMapStorage.forMassList();
  static final List<FileToImport> filesToImport = List.of(FileToImport.factor5(
      "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Thermo\\20 years mzmine\\171103_PMA_TK_PA14_04.mzML"));
  private ModularFeatureList flist = FeatureList.createDummy();

  @BeforeAll
  static void importFiles() throws InterruptedException {
    MZmineTestUtil.clearProjectAndLibraries();
    final Map<AdvancedSpectraImportParameters, List<FileToImport>> filesByImportParam = filesToImport.stream()
        .collect(Collectors.groupingBy(FileToImport::importParam));

    for (final var paramToFiles : filesByImportParam.entrySet()) {
      final TaskResult _ = MZmineTestUtil.importFiles(
          paramToFiles.getValue().stream().map(FileToImport::filePath).distinct().toList(), 10_000,
          paramToFiles.getKey());
    }
  }

  @Test
  public void testHighDynamicRangeEic() {
    final Eic hdr = new Eic("171103_PMA_TK_PA14_04", Range.closed(260.1596, 260.1692),
        "High dynamic range EIC");
    final IonTimeSeries<Scan> series = hdr.extract();

    List<Peak> expected = List.of(Peak.topRange(0.572587f, Range.closed(0.552128f, 0.606720f)),
        Peak.topRange(1.659160f, Range.closed(1.637599f, 1.716732f)),
        Peak.topRange(1.752401f, Range.closed(1.723761f, 1.782590f)),
        Peak.topRange(1.862740f, Range.closed(1.840021f, 1.915189f)),
        Peak.topRange(1.931146f, Range.closed(1.915189f, 1.990487f)),
        Peak.topRange(2.029678f, Range.closed(2.005051f, 2.120809f)),
        Peak.topRange(2.254448f, Range.closed(2.223635f, 2.294700f)),
        Peak.topRange(2.388845f, Range.closed(2.350951f, 2.476528f)),
        Peak.topRange(2.789076f, Range.closed(2.741272f, 2.874640f)),
        Peak.topRange(2.913065f, Range.closed(2.874640f, 2.983014f)),
        Peak.topRange(3.227963f, Range.closed(3.121264f, 3.251300f)),
        Peak.topRange(3.290150f, Range.closed(3.251300f, 3.439810f)),
        Peak.topRange(3.508047f, Range.closed(3.439810f, 3.783683f)));

    final var localMin = new MinimumSearchFeatureResolver(flist, ResolvingDimension.RETENTION_TIME,
        0.85, 0.04, 0d, 1E5, 1.7, Range.closed(0d, 10d), 5);

    final var wavelet = new WaveletResolver(flist, WaveletResolverParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS),
        ResolvingDimension.RETENTION_TIME, false, new GroupMS2SubParameters(), 5, "r",
        OriginalFeatureListOption.REMOVE, 8, null, 1E5, NoiseCalculation.STANDARD_DEVIATION, true,
        false, AdvancedWaveletParameters.createLcDefault()));

    final List<Peak> localMinPeaks = Peak.of(localMin.resolve(series, storage));
    final List<Peak> waveletPeaks = Peak.of(wavelet.resolve(series, storage));

    final EicResult localMinResult = test(hdr, expected, localMinPeaks, "Local min");
    final EicResult waveletResult = test(hdr, expected, waveletPeaks, "Wavelet");
    logger.info(localMinResult.toString());
    logger.info(waveletResult.toString());

    Assertions.assertEquals(13, waveletResult.truePositives());
    Assertions.assertEquals(3, waveletResult.additionalPositives());

    Assertions.assertEquals(2, localMinResult.additionalPositives());
    Assertions.assertEquals(2, localMinResult.additionalPositives());
  }

  private EicResult test(Eic eic, @NotNull List<Peak> expected, @NotNull List<Peak> actual,
      @NotNull String testName) {
    int falseNegatives = 0;
    for (int i = 0; i < expected.size(); i++) {
      final Peak peak = expected.get(i);
      if (!peak.find(actual)) {
        logger.info(
            "%s:\tDid not find expected peak %d (%s)".formatted(testName, i, peak.toString()));
        falseNegatives++;
      }
    }
    final int truePositives = expected.size() - falseNegatives;
    final int additionalPositives = actual.size() - truePositives;

    return new EicResult(eic, testName, expected.size(), actual.size(), truePositives,
        falseNegatives, additionalPositives);
  }

  record EicResult(@NotNull Eic eic, @NotNull String testName, int numExpected, int numFound,
                   int truePositives, int falseNegatives, int additionalPositives) {

    boolean allPeaksFound() {
      return numExpected == truePositives && falseNegatives == 0;
    }

    @Override
    public String toString() {
      return "EicResult{" + "eic=" + eic.desc() + ", testName='" + testName + '\''
          + ", numExpected=" + numExpected + ", numFound=" + numFound + ", truePositives="
          + truePositives + ", falseNegatives=" + falseNegatives + ", additionalPositives="
          + additionalPositives + ", error=" + ((falseNegatives + additionalPositives)
          / (double) numExpected) + '}';
    }
  }
}
