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

package io.github.mzmine.datamodel.data_access;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.TdfPressureCompensation;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.IonMobilityUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import testutils.MZmineTestUtil;

/**
 * Tests {@link BinningMobilogramDataAccess} on a file with multiple mobility segments.
 * {@code lc-tims-ms-pasef-a.d} changes its mobility window at frame #33, so it has at least two
 * segments regardless of whether per-frame pressure compensation is enabled during import.
 * <p>
 * The central guarantee here: every mobility value of every frame must be assigned to a bin. Values
 * that fall outside all bins lose their intensity silently, which shows up as a signal drop at the
 * edge of a mobilogram.
 */
@TestInstance(Lifecycle.PER_CLASS)
@DisabledOnOs(OS.MAC)
public class BinningMobilogramDataAccessMultiSegmentTest {

  private static final Logger logger = Logger.getLogger(
      BinningMobilogramDataAccessMultiSegmentTest.class.getName());

  private static final String TEST_FILE = "rawdatafiles/additional/lc-tims-ms-pasef-a.d";

  private IMSRawDataFile file;

  /**
   * One representative frame per mobility segment.
   */
  private List<Frame> segmentFrames;

  @BeforeAll
  public void initialize() throws InterruptedException {
    MZmineTestUtil.startMzmineCore();
    MZmineTestUtil.cleanProject();
    // import without pressure compensation, so the file has exactly the two segments of its own
    // acquisition and the segment count does not depend on the vendor option default
    MZmineTestUtil.importFiles(List.of(TEST_FILE), 360,
        VendorImportParametersTestUtils.withPressureCompensation(TdfPressureCompensation.NONE),
        null);

    file = (IMSRawDataFile) ProjectService.getProject().getCurrentRawDataFiles().stream()
        .filter(IMSRawDataFile.class::isInstance).findFirst()
        .orElseThrow(() -> new IllegalStateException("Did not import an IMS raw data file."));

    segmentFrames = new ArrayList<>(IonMobilityUtils.getUniqueMobilityRanges(file).keySet());
    logger.info(() -> "Segments: " + segmentFrames.size() + " " + segmentFrames.stream()
        .map(f -> "#" + f.getFrameId() + f.getMobilityRange()).toList());
  }

  @AfterAll
  public void tearDown() {
    MZmineTestUtil.cleanProject();
  }

  @NotNull
  private static IonMobilitySeries unitMobilogram(@NotNull final Frame frame) {
    final List<MobilityScan> scans = new ArrayList<>(frame.getMobilityScans());
    final double[] mzs = new double[scans.size()];
    Arrays.fill(mzs, 500d);
    final double[] intensities = new double[scans.size()];
    Arrays.fill(intensities, 1d);
    return new SimpleIonMobilitySeries(null, mzs, intensities, scans);
  }

  @Test
  @DisplayName("The test file really has more than one mobility segment")
  void fileHasMultipleSegments() {
    Assertions.assertTrue(segmentFrames.size() > 1,
        "Expected " + TEST_FILE + " to contain multiple mobility segments, found "
            + segmentFrames.size());
  }

  @Test
  @DisplayName("The bins span the mobility range of every segment")
  void binsSpanEverySegmentRange() {
    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    final double[] bins = access.getMobilityValues();
    final Range<Double> binned = Range.closed(bins[0], bins[bins.length - 1]);

    for (final Frame frame : segmentFrames) {
      final Range<Double> segment = frame.getMobilityRange();
      Assertions.assertTrue(binned.encloses(segment),
          "bins " + binned + " do not enclose segment of frame #" + frame.getFrameId() + " "
              + segment);
    }
  }

  @Test
  @DisplayName("No intensity is lost for any segment, at any bin width")
  void noIntensityIsLostForAnySegment() {
    for (final int binWidth : new int[]{1, 2, 3, 5, 7, 10,
        BinningMobilogramDataAccess.getRecommendedBinWidth(file)}) {
      final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, binWidth);

      for (final Frame frame : segmentFrames) {
        access.setMobilogram(List.of(unitMobilogram(frame)));
        Assertions.assertEquals(frame.getNumberOfMobilityScans(),
            Arrays.stream(access.getIntensityValues()).sum(), 1E-9,
            "intensity lost for frame #" + frame.getFrameId() + " at bin width " + binWidth);
      }
    }
  }

  @Test
  @DisplayName("Mobilograms from different segments are all summed without loss")
  void mobilogramsFromDifferentSegmentsAreSummedWithoutLoss() {
    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file,
        BinningMobilogramDataAccess.getRecommendedBinWidth(file));

    final List<IonMobilitySeries> mobilograms = segmentFrames.stream()
        .map(BinningMobilogramDataAccessMultiSegmentTest::unitMobilogram).toList();
    access.setMobilogram(mobilograms);

    final double expected = mobilograms.stream().mapToDouble(IonMobilitySeries::getNumberOfValues)
        .sum();
    Assertions.assertEquals(expected, Arrays.stream(access.getIntensityValues()).sum(), 1E-9);
  }

  @Test
  @DisplayName("characterisation: interior mis-bins from segment drift stay bounded")
  void interiorMisBinsStayBounded() {
    // A bin that receives a different number of values than the bin width, although it lies between
    // two filled bins of the same mobilogram, is an intensity artifact: a bin with one value less
    // than its neighbours is a visible drop. Segments that are calibrated slightly differently drift
    // against each other, so a shared grid cannot avoid a small number of these. Measured on this
    // file: worst 9/533 (1.7%) at bin width 2, with at most 3 interior zeros; no interior zeros from
    // bin width 5 upwards. The bounds below guard the grid construction against regressions.
    final double maxMisBinFraction = 0.05;
    final double maxZeroFraction = 0.02;
    final Map<Integer, String> report = new LinkedHashMap<>();

    for (final int binWidth : new int[]{2, 3, 5,
        BinningMobilogramDataAccess.getRecommendedBinWidth(file)}) {
      final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, binWidth);

      for (final Frame frame : segmentFrames) {
        access.setMobilogram(List.of(unitMobilogram(frame)));
        final double[] binned = access.getIntensityValues();

        int first = -1;
        int last = -1;
        for (int i = 0; i < binned.length; i++) {
          if (binned[i] > 0d) {
            if (first == -1) {
              first = i;
            }
            last = i;
          }
        }
        int interiorZeros = 0;
        for (int i = first + 1; i < last; i++) {
          if (binned[i] == 0d) {
            interiorZeros++;
          }
        }
        int misBins = 0;
        for (int i = first + 1; i < last; i++) {
          if (Math.abs(binned[i] - binWidth) > 1E-9) {
            misBins++;
          }
        }
        final int interiorBins = Math.max(1, last - first - 1);
        report.put(frame.getFrameId() * 100 + binWidth,
            "frame #" + frame.getFrameId() + " w=" + binWidth + ": " + interiorZeros + " zeros, "
                + misBins + "/" + interiorBins + " mis-bins");

        final String where =
            "frame #" + frame.getFrameId() + " at bin width " + binWidth + " (" + misBins
                + " mis-bins, " + interiorZeros + " zeros of " + interiorBins + " interior bins)";
        Assertions.assertTrue(misBins <= interiorBins * maxMisBinFraction,
            "too many interior mis-bins for " + where);
        Assertions.assertTrue(interiorZeros <= interiorBins * maxZeroFraction,
            "too many interior empty bins for " + where);
      }
    }
    logger.info(() -> "interior mis-bins per segment: " + report.values());
  }
}
