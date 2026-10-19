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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.TdfPressureCompensation;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.IonMobilityUtils;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * Characterises {@link BinningMobilogramDataAccess} on a real, "regular" timsTOF file, i.e. a file
 * that only contains a single mobility segment. Complements
 * {@link BinningMobilogramDataAccessTest}, which uses synthetic files.
 * <p>
 * These tests are the baseline for upcoming changes that have to deal with files containing
 * multiple largely differing mobility segments or per-frame pressure recalibration.
 */
@TestInstance(Lifecycle.PER_CLASS)
@DisabledOnOs(OS.MAC)
public class BinningMobilogramDataAccessTdfTest {

  private static final Logger logger = Logger.getLogger(
      BinningMobilogramDataAccessTdfTest.class.getName());

  /**
   * All 60 frames of this file share the exact same mobility values, so it only contains a single
   * mobility segment. Note that {@code lc-tims-ms-pasef-a.d} is deliberately not used here: it
   * switches the mobility range at frame #33 and therefore contains two segments.
   */
  private static final String TEST_FILE = "rawdatafiles/additional/lc-tims-ms-pasef-b.d";

  private static final double DELTA = 1E-9;

  private IMSRawDataFile file;

  /**
   * The frame the bins are derived from: the first frame of the single unique mobility range.
   */
  private Frame referenceFrame;

  @BeforeAll
  public void initialize() throws InterruptedException {
    MZmineTestUtil.startMzmineCore();
    MZmineTestUtil.cleanProject();
    // import without pressure compensation, which would give every frame block its own mobility
    // grid and remove the single-segment premise of every test below
    MZmineTestUtil.importFiles(List.of(TEST_FILE), 360,
        VendorImportParametersTestUtils.withPressureCompensation(TdfPressureCompensation.NONE),
        null);

    file = (IMSRawDataFile) ProjectService.getProject().getCurrentRawDataFiles().stream()
        .filter(IMSRawDataFile.class::isInstance).findFirst()
        .orElseThrow(() -> new IllegalStateException("Did not import an IMS raw data file."));

    referenceFrame = IonMobilityUtils.getUniqueMobilityRanges(file).keySet().iterator().next();
    logger.info(() -> "Using frame #" + referenceFrame.getFrameId() + " with "
        + referenceFrame.getNumberOfMobilityScans() + " mobility scans as reference.");
  }

  @AfterAll
  public void tearDown() {
    MZmineTestUtil.cleanProject();
  }

  /**
   * @return The mobility values of {@link #referenceFrame}, sorted ascending.
   */
  @NotNull
  private double[] sortedReferenceMobilities() {
    final double[] mobilities = referenceFrame.getMobilities().toDoubleArray();
    Arrays.sort(mobilities);
    return mobilities;
  }

  /**
   * Creates a mobilogram over all mobility scans of {@link #referenceFrame}.
   *
   * @param intensities One intensity per mobility scan, in mobility scan order.
   */
  @NotNull
  private IonMobilitySeries createFullFrameMobilogram(@NotNull final double[] intensities) {
    final List<MobilityScan> scans = new ArrayList<>(referenceFrame.getMobilityScans());
    final double[] mzs = new double[scans.size()];
    Arrays.fill(mzs, 500d);
    return new SimpleIonMobilitySeries(null, mzs, intensities, scans);
  }

  // ---------------------------------------------------------------------------------------------
  // preconditions: this is a "regular" file
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("The test file is a regular TIMS file with a single mobility segment")
  void testFileHasSingleMobilitySegment() {
    Assertions.assertEquals(MobilityType.TIMS, file.getMobilityType());
    Assertions.assertEquals(1, IonMobilityUtils.getUniqueMobilityRanges(file).size(),
        "The test file is expected to contain exactly one unique mobility range.");

    // every frame shares the exact same mobility values -> a single segment
    final DoubleImmutableList reference = referenceFrame.getMobilities();
    Assertions.assertNotNull(reference);
    for (final Frame frame : file.getFrames()) {
      Assertions.assertEquals(reference, frame.getMobilities(),
          "Frame #" + frame.getFrameId() + " uses different mobility values.");
    }
  }

  // ---------------------------------------------------------------------------------------------
  // bin creation
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("Bin width 1 creates one ascending bin per raw mobility value")
  void binWidthOneMirrorsTheRawMobilityValues() {
    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    final double[] expected = sortedReferenceMobilities();

    Assertions.assertEquals(expected.length, access.getNumberOfValues());
    Assertions.assertArrayEquals(expected, access.getMobilityValues(), DELTA);
  }

  // ---------------------------------------------------------------------------------------------
  // binning of mobilograms
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("Every bin receives signal and no intensity is lost if all mobility scans contribute")
  void noEmptyBinsAndNoIntensityLoss() {
    final int numMobilities = referenceFrame.getMobilities().size();
    final double[] intensities = new double[numMobilities];
    Arrays.fill(intensities, 1d);
    final IonMobilitySeries mobilogram = createFullFrameMobilogram(intensities);

    for (final int binWidth : new int[]{1, 2, 3, 5, 7, 10, 16,
        BinningMobilogramDataAccess.getRecommendedBinWidth(file)}) {
      final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, binWidth);
      access.setMobilogram(List.of(mobilogram));

      final double[] binned = access.getIntensityValues();
      Assertions.assertEquals(numMobilities, Arrays.stream(binned).sum(), DELTA,
          "intensity lost for bin width " + binWidth);
      for (int i = 0; i < binned.length; i++) {
        Assertions.assertTrue(binned[i] > 0d,
            "bin " + i + "/" + binned.length + " is empty for bin width " + binWidth
                + " although every mobility value carries intensity");
      }
    }
  }

  @Test
  @DisplayName("The raw TIC of a frame is preserved by the binning")
  void rawFrameTicIsPreserved() {
    final List<MobilityScan> scans = referenceFrame.getMobilityScans();
    final double[] intensities = new double[scans.size()];
    for (int i = 0; i < scans.size(); i++) {
      final MobilityScan scan = scans.get(i);
      double tic = 0d;
      for (int j = 0; j < scan.getNumberOfDataPoints(); j++) {
        tic += scan.getIntensityValue(j);
      }
      intensities[i] = tic;
    }
    final double expectedSum = Arrays.stream(intensities).sum();
    Assertions.assertTrue(expectedSum > 0d, "the reference frame does not contain any signal");

    final IonMobilitySeries mobilogram = createFullFrameMobilogram(intensities);
    for (final int binWidth : new int[]{1, 2, 5,
        BinningMobilogramDataAccess.getRecommendedBinWidth(file)}) {
      final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, binWidth);
      access.setMobilogram(List.of(mobilogram));

      Assertions.assertEquals(expectedSum, Arrays.stream(access.getIntensityValues()).sum(),
          expectedSum * 1E-12, "TIC not preserved for bin width " + binWidth);
    }
  }
}
