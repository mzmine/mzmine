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
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.util.IonMobilityUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Characterises the binning of {@link BinningMobilogramDataAccess} for "regular" ion mobility
 * files, i.e. files in which every frame shares the very same set of mobility values (a single
 * mobility segment). These tests are the baseline for upcoming changes that have to deal with files
 * containing multiple, largely differing mobility segments or per-frame pressure recalibration.
 * <p>
 * Tests marked as "characterisation" lock in the behaviour as it is implemented today, including
 * known quirks. They are expected to be revisited together with the implementation.
 */
public class BinningMobilogramDataAccessTest {

  /**
   * Mobility values of a synthetic drift tube file, ascending with mobility scan number. Chosen so
   * that all expected bin centers are exactly writable decimals.
   */
  private static final double[] DTIMS_MOBILITIES = new double[]{0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1,
      1.2, 1.3, 1.4, 1.5, 1.6};

  /**
   * The same values as {@link #DTIMS_MOBILITIES}, but descending with mobility scan number as it is
   * the case for TIMS data.
   */
  private static final double[] TIMS_MOBILITIES = reversed(DTIMS_MOBILITIES);

  private static final double DELTA = 1E-9;

  private static double[] reversed(@NotNull final double[] values) {
    final double[] result = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = values[values.length - 1 - i];
    }
    return result;
  }

  /**
   * Creates a file in which every frame uses the exact same mobility values, so only a single
   * mobility segment and a single unique mobility range exist.
   *
   * @param mobilityType The mobility type of all frames.
   * @param mobilities   The mobility values in mobility scan order.
   * @param numFrames    The number of frames to create.
   */
  @NotNull
  private static IMSRawDataFile createSingleSegmentFile(@NotNull final MobilityType mobilityType,
      @NotNull final double[] mobilities, final int numFrames) {
    final IMSRawDataFile file = new IMSRawDataFileImpl("single segment test file", null, null,
        Color.BLACK);

    for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
      final SimpleFrame frame = new SimpleFrame(file, frameIndex + 1, 1, frameIndex,
          new double[]{100d}, new double[]{1d}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE,
          "", Range.closed(100d, 1000d), mobilityType, null, null);

      // mobilities first, so the mobility scan count is validated against them
      frame.setMobilities(mobilities);

      final List<BuildingMobilityScan> scans = new ArrayList<>();
      for (int i = 0; i < mobilities.length; i++) {
        scans.add(new BuildingMobilityScan(i, new double[]{100d, 200d}, new double[]{1d, 2d},
            MassSpectrumType.CENTROIDED));
      }
      frame.setMobilityScans(scans, false);

      file.addScan(frame);
    }
    return file;
  }

  /**
   * Creates a file with one frame per given mobility array, i.e. one mobility segment per frame, in
   * exactly the given order.
   *
   * @param mobilityType       The mobility type of all frames.
   * @param mobilitiesPerFrame The mobility values of each frame, in mobility scan order.
   */
  @NotNull
  private static IMSRawDataFile createMultiSegmentFile(@NotNull final MobilityType mobilityType,
      @NotNull final List<double[]> mobilitiesPerFrame) {
    final IMSRawDataFile file = new IMSRawDataFileImpl("multi segment test file", null, null,
        Color.BLACK);

    for (int frameIndex = 0; frameIndex < mobilitiesPerFrame.size(); frameIndex++) {
      final double[] mobilities = mobilitiesPerFrame.get(frameIndex);
      final SimpleFrame frame = new SimpleFrame(file, frameIndex + 1, 1, frameIndex,
          new double[]{100d}, new double[]{1d}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE,
          "", Range.closed(100d, 1000d), mobilityType, null, null);

      frame.setMobilities(mobilities);

      final List<BuildingMobilityScan> scans = new ArrayList<>();
      for (int i = 0; i < mobilities.length; i++) {
        scans.add(new BuildingMobilityScan(i, new double[]{100d, 200d}, new double[]{1d, 2d},
            MassSpectrumType.CENTROIDED));
      }
      frame.setMobilityScans(scans, false);

      file.addScan(frame);
    }
    return file;
  }

  /**
   * @param intensities One intensity per mobility scan of the frame, in mobility scan order.
   */
  @NotNull
  private static IonMobilitySeries createMobilogram(@NotNull final Frame frame,
      @NotNull final double[] intensities) {
    return createMobilogram(frame, 0, frame.getNumberOfMobilityScans(), intensities);
  }

  /**
   * @param fromScan    First mobility scan index (inclusive) the mobilogram covers.
   * @param toScan      Last mobility scan index (exclusive) the mobilogram covers.
   * @param intensities One intensity per covered mobility scan.
   */
  @NotNull
  private static IonMobilitySeries createMobilogram(@NotNull final Frame frame, final int fromScan,
      final int toScan, @NotNull final double[] intensities) {
    final List<MobilityScan> scans = new ArrayList<>(
        frame.getMobilityScans().subList(fromScan, toScan));
    final double[] mzs = new double[scans.size()];
    Arrays.fill(mzs, 500d);
    return new SimpleIonMobilitySeries(null, mzs, intensities, scans);
  }

  private static double[] ascendingIntensities(final int numValues) {
    final double[] intensities = new double[numValues];
    for (int i = 0; i < numValues; i++) {
      intensities[i] = i + 1;
    }
    return intensities;
  }

  // ---------------------------------------------------------------------------------------------
  // preconditions of a "regular" file
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("A file with identical mobilities in all frames has exactly one mobility segment")
  void singleSegmentFileHasOneUniqueMobilityRange() {
    final IMSRawDataFile dtims = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        5);
    final IMSRawDataFile tims = createSingleSegmentFile(MobilityType.TIMS, TIMS_MOBILITIES, 5);

    Assertions.assertEquals(1, IonMobilityUtils.getUniqueMobilityRanges(dtims).size());
    Assertions.assertEquals(1, IonMobilityUtils.getUniqueMobilityRanges(tims).size());
  }

  @Test
  void illegalBinWidthThrows() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new BinningMobilogramDataAccess(file, 0));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new BinningMobilogramDataAccess(file, -1));
  }

  // ---------------------------------------------------------------------------------------------
  // bin creation
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("Bin width 1 keeps every raw mobility value as its own bin (DTIMS)")
  void binWidthOneKeepsRawMobilitiesDtims() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        3);
    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);

    Assertions.assertEquals(DTIMS_MOBILITIES.length, access.getNumberOfValues());
    Assertions.assertArrayEquals(DTIMS_MOBILITIES, access.getMobilityValues(), DELTA);
  }

  @Test
  @DisplayName("Bin width 1 sorts the descending TIMS mobilities ascending")
  void binWidthOneKeepsRawMobilitiesTims() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.TIMS, TIMS_MOBILITIES, 3);
    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);

    Assertions.assertEquals(TIMS_MOBILITIES.length, access.getNumberOfValues());
    // bins are always ascending, independent of the raw acquisition order
    Assertions.assertArrayEquals(DTIMS_MOBILITIES, access.getMobilityValues(), DELTA);
  }

  @Test
  @DisplayName("Bin centers are the arithmetic mean of the contained raw mobilities")
  void binCentersAreMeanOfContainedMobilities() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        3);

    final BinningMobilogramDataAccess width2 = new BinningMobilogramDataAccess(file, 2);
    Assertions.assertArrayEquals(new double[]{0.55, 0.75, 0.95, 1.15, 1.35, 1.55},
        width2.getMobilityValues(), DELTA);

    final BinningMobilogramDataAccess width3 = new BinningMobilogramDataAccess(file, 3);
    Assertions.assertArrayEquals(new double[]{0.6, 0.9, 1.2, 1.5}, width3.getMobilityValues(),
        DELTA);
  }

  @Test
  @DisplayName("A trailing bin that is not completely filled only averages the values it contains")
  void trailingPartialBinAveragesOnlyItsOwnValues() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    // 12 values / 5 -> bins of 5, 5 and 2 values
    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 5);

    Assertions.assertEquals(3, access.getNumberOfValues());
    Assertions.assertArrayEquals(new double[]{0.7, 1.2, 1.55}, access.getMobilityValues(), DELTA);
  }

  @Test
  @DisplayName("A bin width exceeding the number of mobility values collapses everything into one bin")
  void binWidthLargerThanMobilityCountYieldsSingleBin() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 100);

    Assertions.assertEquals(1, access.getNumberOfValues());
    Assertions.assertArrayEquals(new double[]{1.05}, access.getMobilityValues(), DELTA);
  }

  @Test
  @DisplayName("The number of bins is ceil(numMobilityValues / binWidth) for a single segment")
  void binCountIsCeilDivisionOfMobilityCount() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);

    for (int binWidth = 1; binWidth <= DTIMS_MOBILITIES.length + 3; binWidth++) {
      final int expected = (DTIMS_MOBILITIES.length + binWidth - 1) / binWidth;
      Assertions.assertEquals(expected,
          new BinningMobilogramDataAccess(file, binWidth).getNumberOfValues(),
          "unexpected bin count for bin width " + binWidth);
    }
  }

  @Test
  @DisplayName("The reported bin width is the one passed to the constructor")
  void binWidthIsReportedAsGiven() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    Assertions.assertEquals(4d, new BinningMobilogramDataAccess(file, 4).getBinWidth(), DELTA);
  }

  @Test
  @DisplayName("characterisation: approximate bin size divides the covered range by numBins - 2")
  void approximateBinSizeCurrentlyDividesByBinCountMinusTwo() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);

    // 12 bins spanning 1.6 - 0.5 = 1.1 -> 1.1 / (12 - 2) instead of 1.1 / (12 - 1) = 0.1
    Assertions.assertEquals(1.1 / 10d,
        new BinningMobilogramDataAccess(file, 1).getApproximateBinSize(), DELTA);
    // 6 bins spanning 1.55 - 0.55 = 1.0 -> 1.0 / (6 - 2) instead of 1.0 / (6 - 1) = 0.2
    Assertions.assertEquals(1.0 / 4d,
        new BinningMobilogramDataAccess(file, 2).getApproximateBinSize(), DELTA);
  }

  // ---------------------------------------------------------------------------------------------
  // setMobilogram(List<IonMobilitySeries>)
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("Bin width 1 keeps every raw intensity in its own bin, no bin is dropped (DTIMS)")
  void mobilogramWithBinWidthOneIsUnchangedDtims() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    final Frame frame = file.getFrame(0);
    final double[] intensities = ascendingIntensities(DTIMS_MOBILITIES.length);

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    access.setMobilogram(List.of(createMobilogram(frame, intensities)));

    Assertions.assertArrayEquals(intensities, access.getIntensityValues(), DELTA);
  }

  @Test
  @DisplayName("Bin width 1 reverses the descending TIMS mobilogram into ascending bins")
  void mobilogramWithBinWidthOneIsReversedForTims() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.TIMS, TIMS_MOBILITIES, 1);
    final Frame frame = file.getFrame(0);
    // intensities in mobility scan order, i.e. from high to low mobility
    final double[] intensities = ascendingIntensities(TIMS_MOBILITIES.length);

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    access.setMobilogram(List.of(createMobilogram(frame, intensities)));

    Assertions.assertArrayEquals(reversed(intensities), access.getIntensityValues(), DELTA);
  }

  @Test
  @DisplayName("Intensities of all raw values within a bin are summed")
  void intensitiesAreSummedWithinBins() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    final Frame frame = file.getFrame(0);
    final double[] intensities = ascendingIntensities(DTIMS_MOBILITIES.length);

    final BinningMobilogramDataAccess width2 = new BinningMobilogramDataAccess(file, 2);
    width2.setMobilogram(List.of(createMobilogram(frame, intensities)));
    Assertions.assertArrayEquals(new double[]{3, 7, 11, 15, 19, 23}, width2.getIntensityValues(),
        DELTA);

    final BinningMobilogramDataAccess width3 = new BinningMobilogramDataAccess(file, 3);
    width3.setMobilogram(List.of(createMobilogram(frame, intensities)));
    Assertions.assertArrayEquals(new double[]{6, 15, 24, 33}, width3.getIntensityValues(), DELTA);
  }

  @Test
  @DisplayName("No intensity is lost and no bin in between stays empty for a single segment")
  void noIntensityIsLostAndNoInnerBinStaysEmpty() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    final Frame frame = file.getFrame(0);
    final double[] intensities = new double[DTIMS_MOBILITIES.length];
    Arrays.fill(intensities, 1d);
    final double expectedSum = DTIMS_MOBILITIES.length;

    for (int binWidth = 1; binWidth <= 5; binWidth++) {
      final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, binWidth);
      access.setMobilogram(List.of(createMobilogram(frame, intensities)));

      final double[] binned = access.getIntensityValues();
      Assertions.assertEquals(expectedSum, Arrays.stream(binned).sum(), DELTA,
          "intensity lost for bin width " + binWidth);
      for (int i = 0; i < binned.length; i++) {
        Assertions.assertTrue(binned[i] > 0d,
            "empty bin " + i + " for bin width " + binWidth + " although every mobility value"
                + " carries intensity");
      }
    }
  }

  @Test
  @DisplayName("Bins outside the covered mobility range stay empty")
  void binsOutsideTheMobilogramStayEmpty() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    final Frame frame = file.getFrame(0);

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    // mobility scans 3, 4, 5 -> mobilities 0.8, 0.9, 1.0
    access.setMobilogram(List.of(createMobilogram(frame, 3, 6, new double[]{10d, 20d, 30d})));

    Assertions.assertArrayEquals(new double[]{0, 0, 0, 10, 20, 30, 0, 0, 0, 0, 0, 0},
        access.getIntensityValues(), DELTA);
  }

  @Test
  @DisplayName("Mobilograms of multiple frames are summed into the same bins")
  void mobilogramsOfMultipleFramesAreSummed() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        3);
    final double[] intensities = new double[DTIMS_MOBILITIES.length];
    Arrays.fill(intensities, 2d);

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 2);
    access.setMobilogram(List.of(createMobilogram(file.getFrame(0), intensities), //
        createMobilogram(file.getFrame(1), intensities), //
        createMobilogram(file.getFrame(2), intensities)));

    // 3 frames * 2 values per bin * intensity 2
    final double[] expected = new double[6];
    Arrays.fill(expected, 12d);
    Assertions.assertArrayEquals(expected, access.getIntensityValues(), DELTA);
  }

  @Test
  @DisplayName("Setting a new mobilogram clears the intensities of the previous one")
  void settingANewMobilogramClearsPreviousIntensities() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    final Frame frame = file.getFrame(0);

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    access.setMobilogram(
        List.of(createMobilogram(frame, ascendingIntensities(DTIMS_MOBILITIES.length))));
    access.setMobilogram(List.of(createMobilogram(frame, 3, 6, new double[]{10d, 20d, 30d})));

    Assertions.assertArrayEquals(new double[]{0, 0, 0, 10, 20, 30, 0, 0, 0, 0, 0, 0},
        access.getIntensityValues(), DELTA);
  }

  @Test
  @DisplayName("An empty list of mobilograms results in an empty mobilogram")
  void emptyMobilogramListYieldsZeroIntensities() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    access.setMobilogram(List.of());

    Assertions.assertArrayEquals(new double[DTIMS_MOBILITIES.length], access.getIntensityValues(),
        DELTA);
  }

  // ---------------------------------------------------------------------------------------------
  // multiple mobility segments: the order the segments are collected in must not matter
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("TIMS: a later segment reaching higher than the first loses no intensity")
  void timsSegmentReachingHigherThanTheFirstLosesNoIntensity() {
    // frame 1 holds the lower window, frame 2 the higher one. Collecting in frame order would cap
    // the bins at frame 1's maximum and drop everything of frame 2 above it.
    final double[] lowerWindow = new double[]{1.00, 0.90, 0.80, 0.70, 0.60, 0.50};
    final double[] higherWindow = new double[]{1.25, 1.15, 1.05, 0.95, 0.85, 0.75};
    final IMSRawDataFile file = createMultiSegmentFile(MobilityType.TIMS,
        List.of(lowerWindow, higherWindow));

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);

    // the bins must span both windows
    final double[] bins = access.getMobilityValues();
    Assertions.assertEquals(0.50, bins[0], DELTA);
    Assertions.assertEquals(1.25, bins[bins.length - 1], DELTA);

    for (final Frame frame : file.getFrames()) {
      final double[] intensities = new double[frame.getNumberOfMobilityScans()];
      Arrays.fill(intensities, 1d);
      access.setMobilogram(List.of(createMobilogram(frame, intensities)));

      Assertions.assertEquals(intensities.length, Arrays.stream(access.getIntensityValues()).sum(),
          DELTA, "intensity lost for frame #" + frame.getFrameId());
    }
  }

  @Test
  @DisplayName("DTIMS: a later segment reaching lower than the first is not piled into the first bin")
  void dtimsSegmentReachingLowerThanTheFirstIsNotPiledIntoTheFirstBin() {
    // frame 1 holds the higher window, frame 2 the lower one. Collecting in frame order would start
    // the bins at frame 1's minimum; everything of frame 2 below it then falls into bin 0, whose
    // lower edge is -MOBILITY_EPSILON, producing a spike instead of a dropped value.
    final double[] higherWindow = new double[]{0.80, 0.90, 1.00, 1.10, 1.20, 1.30};
    final double[] lowerWindow = new double[]{0.55, 0.65, 0.75, 0.85, 0.95, 1.05};
    final IMSRawDataFile file = createMultiSegmentFile(MobilityType.DRIFT_TUBE,
        List.of(higherWindow, lowerWindow));

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);

    final double[] bins = access.getMobilityValues();
    Assertions.assertEquals(0.55, bins[0], DELTA);
    Assertions.assertEquals(1.30, bins[bins.length - 1], DELTA);

    for (final Frame frame : file.getFrames()) {
      final double[] intensities = new double[frame.getNumberOfMobilityScans()];
      Arrays.fill(intensities, 1d);
      access.setMobilogram(List.of(createMobilogram(frame, intensities)));

      final double[] binned = access.getIntensityValues();
      Assertions.assertEquals(intensities.length, Arrays.stream(binned).sum(), DELTA,
          "intensity lost for frame #" + frame.getFrameId());
      // at bin width 1 every value must get its own bin, none may pile up
      Assertions.assertEquals(1d, Arrays.stream(binned).max().orElseThrow(), DELTA,
          "values piled into a single bin for frame #" + frame.getFrameId());
    }
  }

  // ---------------------------------------------------------------------------------------------
  // setMobilogram(SummedIntensityMobilitySeries)
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("Re-binning a summed mobilogram with the same bin width is an identity operation")
  void reBinningWithSameBinWidthIsIdentity() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    final double[] intensities = ascendingIntensities(DTIMS_MOBILITIES.length);
    final SummedIntensityMobilitySeries summed = new SummedIntensityMobilitySeries(null,
        DTIMS_MOBILITIES, intensities);

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    access.setMobilogram(summed);

    Assertions.assertArrayEquals(intensities, access.getIntensityValues(), DELTA);
  }

  @Test
  @DisplayName("Re-binning a summed mobilogram to a larger bin width sums the intensities")
  void reBinningToLargerBinWidthSumsIntensities() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    final SummedIntensityMobilitySeries summed = new SummedIntensityMobilitySeries(null,
        DTIMS_MOBILITIES, ascendingIntensities(DTIMS_MOBILITIES.length));

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 2);
    access.setMobilogram(summed);

    Assertions.assertArrayEquals(new double[]{3, 7, 11, 15, 19, 23}, access.getIntensityValues(),
        DELTA);
  }

  @Test
  @DisplayName("Re-binning keeps mobility values that are exactly on a bin border in the lower bin")
  void valuesOnBinBorderStayInTheLowerBin() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    // 0.6 is the upper limit of bin 0 (0.5, 0.6) for bin width 2
    final SummedIntensityMobilitySeries summed = new SummedIntensityMobilitySeries(null,
        new double[]{0.6, 0.7}, new double[]{5d, 7d});

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 2);
    access.setMobilogram(summed);

    Assertions.assertArrayEquals(new double[]{5, 7, 0, 0, 0, 0}, access.getIntensityValues(),
        DELTA);
  }

  // ---------------------------------------------------------------------------------------------
  // toSummedMobilogram
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("toSummedMobilogram trims to the signal and keeps one zero on each side")
  void toSummedMobilogramKeepsOneZeroOnEachSide() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    final Frame frame = file.getFrame(0);

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    // mobility scans 3, 4, 5 -> mobilities 0.8, 0.9, 1.0
    access.setMobilogram(List.of(createMobilogram(frame, 3, 6, new double[]{10d, 20d, 30d})));

    final SummedIntensityMobilitySeries summed = access.toSummedMobilogram(null);

    Assertions.assertEquals(5, summed.getNumberOfValues());
    Assertions.assertArrayEquals(new double[]{0.7, 0.8, 0.9, 1.0, 1.1},
        summed.getMobilityValues(new double[5]), DELTA);
    Assertions.assertArrayEquals(new double[]{0d, 10d, 20d, 30d, 0d},
        summed.getIntensityValues(new double[5]), DELTA);
  }

  @Test
  @DisplayName("toSummedMobilogram keeps signal that reaches the very last bin")
  void toSummedMobilogramKeepsSignalInTheLastBin() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);
    final Frame frame = file.getFrame(0);

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    // last two mobility scans -> bins 10 (1.5) and 11 (1.6), so there is no trailing zero to keep
    access.setMobilogram(List.of(createMobilogram(frame, 10, 12, new double[]{10d, 20d})));

    final SummedIntensityMobilitySeries summed = access.toSummedMobilogram(null);

    Assertions.assertEquals(3, summed.getNumberOfValues());
    Assertions.assertArrayEquals(new double[]{1.4, 1.5, 1.6},
        summed.getMobilityValues(new double[summed.getNumberOfValues()]), DELTA);
    Assertions.assertArrayEquals(new double[]{0d, 10d, 20d},
        summed.getIntensityValues(new double[summed.getNumberOfValues()]), DELTA);
  }

  @Test
  @DisplayName("characterisation: an empty mobilogram is trimmed to the first two bins")
  void toSummedMobilogramOfEmptyMobilogram() {
    final IMSRawDataFile file = createSingleSegmentFile(MobilityType.DRIFT_TUBE, DTIMS_MOBILITIES,
        1);

    final BinningMobilogramDataAccess access = new BinningMobilogramDataAccess(file, 1);
    access.setMobilogram(List.of());

    final SummedIntensityMobilitySeries summed = access.toSummedMobilogram(null);

    Assertions.assertEquals(1, summed.getNumberOfValues());
    Assertions.assertEquals(DTIMS_MOBILITIES[0], summed.getMobility(0), DELTA);
    Assertions.assertEquals(0d, summed.getIntensity(0), DELTA);
  }
}
