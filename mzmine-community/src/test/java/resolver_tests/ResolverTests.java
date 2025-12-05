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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package resolver_tests;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;
import java.util.logging.Logger;
import javax.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

@Disabled
public class ResolverTests {

  static final MemoryMapStorage storage = MemoryMapStorage.forMassList();
  static final List<FilesToImport> filesToImport = List.of(FilesToImport.factor5(
      "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Thermo\\20 years mzmine\\171103_PMA_TK_PA14_04.mzML"));
  private static final Logger logger = Logger.getLogger(ResolverTests.class.getName());

  @BeforeAll
  static void importFiles() throws InterruptedException {
    MZmineTestUtil.clearProjectAndLibraries();
    for (FilesToImport toImport : filesToImport) {
      MZmineTestUtil.importFiles(toImport.filePaths(), 5_000, toImport.vendorParam(),
          toImport.advancedParam());
    }
  }

  @Test
  public void testHighDynamicRangeEic() {
    final Eic hdr = new Eic("171103_PMA_TK_PA14_04", Range.closed(260.1596, 260.1692),
        "High dynamic range EIC");
    IonTimeSeries<Scan> series = hdr.extract();

    List<Peak> expected = List.of(Peak.topRange(3.29010f, Range.closed(3.205130f, 3.439810f)),
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
          + additionalPositives + ", error=" + ((falseNegatives + additionalPositives)
          / (double) numExpected) + '}';
    }
  }
}
