/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.TimsTOFAcquisitionUtils;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.ImagingSpot;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.Ms2ImagingMode;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.TopNSelectionModule;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TimsTofAcquisitionUtilsTest {

  private static final Logger logger = Logger.getLogger(
      TimsTofAcquisitionUtilsTest.class.getName());

  final ImagingSpot spot1_1 = new ImagingSpot(new MaldiSpotInfo(0, 0, "", 1, 1, 1, 0, 0, 0),
      Ms2ImagingMode.SINGLE, 50d);
  final ImagingSpot spot25_25 = new ImagingSpot(new MaldiSpotInfo(0, 0, "", 1, 25, 25, 0, 0, 0),
      Ms2ImagingMode.SINGLE, 30d);
  final ImagingSpot spot50_50 = new ImagingSpot(new MaldiSpotInfo(0, 0, "", 1, 50, 50, 0, 0, 0),
      Ms2ImagingMode.SINGLE, 30d);
  final ImagingSpot spot50_0 = new ImagingSpot(new MaldiSpotInfo(0, 0, "", 1, 50, 0, 0, 0, 0),
      Ms2ImagingMode.SINGLE, 40d);
  final ImagingSpot spot0_50 = new ImagingSpot(new MaldiSpotInfo(0, 0, "", 1, 0, 50, 0, 0, 0),
      Ms2ImagingMode.SINGLE, 30d);
  final ImagingSpot spot23_34 = new ImagingSpot(new MaldiSpotInfo(0, 0, "", 1, 23, 34, 0, 0, 0),
      Ms2ImagingMode.SINGLE, 40d);
  final List<ImagingSpot> allSpots = List.of(spot1_1, spot25_25, spot50_50, spot50_0, spot0_50,
      spot23_34);

  @Test
  void testOffsets() {
    Assertions.assertTrue(Arrays.equals(new int[]{20, 0},
        TimsTOFAcquisitionUtils.getOffsetsForIncrementCounter(1, 4, 20, 20)));

    Assertions.assertTrue(Arrays.equals(new int[]{3 * 20, 1 * 20},
        TimsTOFAcquisitionUtils.getOffsetsForIncrementCounter(7, 4, 20, 20)));

    Assertions.assertTrue(Arrays.equals(new int[]{2 * 20, 2 * 20},
        TimsTOFAcquisitionUtils.getOffsetsForIncrementCounter(10, 4, 20, 20)));
  }

  @Test
  void testDistance() {
    Assertions.assertEquals(69.29646455628166,
        TimsTOFAcquisitionUtils.getDistanceForSpots(spot1_1.spotInfo(), spot50_50.spotInfo()));
    Assertions.assertEquals(33.94112549695428,
        TimsTOFAcquisitionUtils.getDistanceForSpots(spot1_1.spotInfo(), spot25_25.spotInfo()));

    Assertions.assertFalse(
        TimsTOFAcquisitionUtils.areSpotsWithinDistance(spot1_1.spotInfo(), spot50_50.spotInfo(),
            60));
    Assertions.assertTrue(
        TimsTOFAcquisitionUtils.areSpotsWithinDistance(spot1_1.spotInfo(), spot25_25.spotInfo(),
            60));

    Assertions.assertFalse(TimsTOFAcquisitionUtils.areSpotsWithinDistance(spot1_1.spotInfo(),
        List.of(spot25_25, spot50_50, spot50_0, spot0_50), 30));
    Assertions.assertTrue(TimsTOFAcquisitionUtils.areSpotsWithinDistance(spot1_1.spotInfo(),
        List.of(spot25_25, spot50_50, spot50_0, spot0_50), 35));
    Assertions.assertTrue(TimsTOFAcquisitionUtils.areSpotsWithinDistance(spot23_34.spotInfo(),
        List.of(spot1_1, spot25_25, spot50_50, spot50_0, spot0_50), 30));
  }

  @Test
  public void testGetSpotsWithinDistance() {
    final MaldiSpotInfo testSpot = new MaldiSpotInfo(0, 0, "", 0, 15, 40, 0, 0, 0);
    final List<ImagingSpot> spotsWithinDistance = TimsTOFAcquisitionUtils.getSpotsWithinDistance(30,
        allSpots, testSpot);
    Assertions.assertEquals(List.of(spot25_25, spot0_50, spot23_34), spotsWithinDistance);
  }

  @Test
  void testGetBestCollisionEnergyForSpot() {
    final List<Double> energies = List.of(30d, 40d, 50d);
    final MaldiTimsPrecursor precursor = new MaldiTimsPrecursor(null, 500d,
        Range.closed(0.85f, 0.88f), energies);

    spot50_0.addPrecursor(precursor, 0.01);
    spot0_50.addPrecursor(precursor, 0.01);
    precursor.incrementSpotCounterForCollisionEnergy(30);
    precursor.incrementSpotCounterForCollisionEnergy(50);
    precursor.incrementSpotCounterForCollisionEnergy(50);
    // CE usage: 30 -> 2, 40 -> 1, 50 -> 2

    final MaldiSpotInfo testSpot = new MaldiSpotInfo(0, 0, "", 0, 15, 40, 0, 0, 0);

    final List<ImagingSpot> spotsWithinDistance = TimsTOFAcquisitionUtils.getSpotsWithinDistance(30,
        allSpots, testSpot);
    final List<ImagingSpot> spotsOutsideDistance = allSpots.stream()
        .filter(s -> !spotsWithinDistance.contains(s)).toList();

    // test with all spots, only 50 is not used
    Assertions.assertEquals(50d,
        TimsTOFAcquisitionUtils.getBestCollisionEnergyForSpot(30, precursor, allSpots, testSpot,
            energies, 3));

    // spot outside must use lowest CE (= 40, only one spot)
    Assertions.assertEquals(40d,
        TimsTOFAcquisitionUtils.getBestCollisionEnergyForSpot(30, precursor, spotsOutsideDistance,
            testSpot, energies, 3));

    // Test with only spots in range, so get the CE that is not used in range
    Assertions.assertEquals(50d,
        TimsTOFAcquisitionUtils.getBestCollisionEnergyForSpot(30, precursor, spotsWithinDistance,
            testSpot, energies, 3));

    // all CEs used, must return null
    Assertions.assertEquals(null,
        TimsTOFAcquisitionUtils.getBestCollisionEnergyForSpot(70, precursor,
            List.of(spot0_50, spot50_0,
                new ImagingSpot(new MaldiSpotInfo(0, 0, "", 0, 10, 10, 0, 0, 0),
                    Ms2ImagingMode.SINGLE, 50)), testSpot, energies, 3));

    // no spot given, must return CE with fewest MS/MS
    Assertions.assertEquals(40d,
        TimsTOFAcquisitionUtils.getBestCollisionEnergyForSpot(30, precursor, List.of(), testSpot,
            energies, 3));
  }

  @Test
  void testGetPossibleCollisionEnergiesForSpot() {
    final List<Double> energies = List.of(30d, 40d, 50d);
    var usedSpots = List.of(spot50_0, spot0_50);

    final MaldiTimsPrecursor precursor = new MaldiTimsPrecursor(null, 500d,
        Range.closed(0.85f, 0.88f), energies);

    spot50_0.addPrecursor(precursor, 0.01);
    spot0_50.addPrecursor(precursor, 0.01);
    precursor.incrementSpotCounterForCollisionEnergy(30);
    precursor.incrementSpotCounterForCollisionEnergy(50);
    precursor.incrementSpotCounterForCollisionEnergy(50);

    final MaldiSpotInfo testSpot = new MaldiSpotInfo(0, 0, "", 0, 15, 40, 0, 0, 0);

    Assertions.assertEquals(List.of(40.0d, 50.0d),
        TimsTOFAcquisitionUtils.getPossibleCollisionEnergiesForSpot(30, precursor,
            List.of(spot25_25), testSpot, energies, 3));

    Assertions.assertEquals(List.of(50.0d, 30.0d),
        TimsTOFAcquisitionUtils.getPossibleCollisionEnergiesForSpot(30, precursor,
            List.of(spot23_34), testSpot, energies, 3));

    Assertions.assertEquals(List.of(50.0d),
        TimsTOFAcquisitionUtils.getPossibleCollisionEnergiesForSpot(30, precursor,
            List.of(spot23_34, spot25_25), testSpot, energies, 3));

    Assertions.assertEquals(List.of(),
        TimsTOFAcquisitionUtils.getPossibleCollisionEnergiesForSpot(60, precursor, allSpots,
            testSpot, energies, 3));
  }

  @Test
  void testIllegalCollisionEnergy() {
    MaldiTimsPrecursor precursor = new MaldiTimsPrecursor(null, 500d, Range.closed(3f, 4f),
        List.of(30d, 40d, 50d));

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> precursor.incrementSpotCounterForCollisionEnergy(25));
  }

  @Test
  void testOverlaps() {
    final List<Double> ce = List.of(20d);
    final var p1 = new MaldiTimsPrecursor(null, 0d, Range.closed(0.846f, 0.883f), ce);
    final var p2 = new MaldiTimsPrecursor(null, 0d, Range.closed(0.886f, 0.912f), ce);
    Assertions.assertFalse(TopNSelectionModule.overlaps(p1, p2, 0.002));
    Assertions.assertFalse(TopNSelectionModule.overlaps(p2, p1, 0.002));
    Assertions.assertTrue(TopNSelectionModule.overlaps(p2, p1, 0.004));

    final var p3 = new MaldiTimsPrecursor(null, 0d, Range.closed(0.887f, 0.912f), ce);
    Assertions.assertTrue(TopNSelectionModule.overlaps(p2, p3));
    Assertions.assertFalse(TopNSelectionModule.overlaps(p1, p3, 0.003f));

    final var p4 = new MaldiTimsPrecursor(null, 0d, Range.closed(1.503f, 1.543f), ce);
    final var p5 = new MaldiTimsPrecursor(null, 0d, Range.closed(1.541f, 1.581f), ce);
    Assertions.assertTrue(TopNSelectionModule.overlaps(p4, p5));

    final var p7 = new MaldiTimsPrecursor(null, 0d, Range.closed(1.437f, 1.477f), ce);
    final var p8 = new MaldiTimsPrecursor(null, 0d, Range.closed(1.481f, 1.521f), ce);
    Assertions.assertTrue(TopNSelectionModule.overlaps(p7, p8, 0.01));
    Assertions.assertTrue(TopNSelectionModule.overlaps(p8, p7, 0.01));
    Assertions.assertFalse(TopNSelectionModule.overlaps(p7, p8, 0.003));
    Assertions.assertFalse(TopNSelectionModule.overlaps(p8, p7, 0.003));
  }

  @Test
  public void testGetSwitchTime() {
    Frame frame = Mockito.mock(Frame.class);
    Mockito.when(frame.getMobilityRange()).thenReturn(Range.closed(0.8, 1.9));
    Mockito.when(frame.getNumberOfMobilityScans()).thenReturn(981);
    logger.info(() -> "" + TimsTOFAcquisitionUtils.getOneOverK0DistanceForSwitchTime(frame, 1.65));
    logger.info(() -> "" + TimsTOFAcquisitionUtils.getOneOverK0DistanceForSwitchTime(frame, 3.3));
    logger.info(() -> "" + TimsTOFAcquisitionUtils.getOneOverK0DistanceForSwitchTime(frame, 0.825));
  }
}
