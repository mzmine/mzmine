package resolver_tests;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
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
    IonTimeSeries<Scan> series = hdr.extract();

    List<Peak> expected = List.of(Peak.topRange(3.290150f, Range.closed(3.205130f, 3.439810f)),
        Peak.topRange(3.508047f, Range.closed(3.439810f, 3.827257f)));

    final var localMin = new MinimumSearchFeatureResolver(FeatureList.createDummy(),
        ResolvingDimension.RETENTION_TIME, 0.85, 0.04, 0d, 1E5, 1.7, Range.closed(0d, 10d), 5);
    List<IonTimeSeries<Scan>> result = localMin.resolve(series, storage);

    logger.info(test(hdr, expected, Peak.of(result), "Local min").toString());
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
          + additionalPositives + ", error =" + ((falseNegatives + additionalPositives)
          / (double) numExpected) + '}';
    }
  }
}
