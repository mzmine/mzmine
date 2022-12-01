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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.Isotope;
import io.github.mzmine.util.IsotopePatternUtils;
import io.github.mzmine.util.IsotopesUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Element;
import org.openscience.cdk.interfaces.IIsotope;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
class IsotopesUtilsTest {

  /**
   * Test that all mz diffs are positive
   */
  @Test
  void testIsotopesMzDiffsNotNegative() {
    List<Element> elements = List.of(new Element("C"), new Element("Cl"), new Element("Fe"),
        new Element("Ca"), new Element("Cu"), new Element("Gd"), new Element("Sn"));
    List<Double> diffs = IsotopesUtils.getIsotopesMzDiffs(elements, 1);

    for (double d : diffs) {
      assertTrue(d > 0, "all mz diffs are supposed to be positive");
    }
  }

  /**
   * Check the number of mz diffs
   */
  @Test
  void testIsotopesMzDiffs() {
    List<Element> elements = List.of(new Element("C"), new Element("Cl"));
    List<Double> diffs = IsotopesUtils.getIsotopesMzDiffs(elements, 1);
    assertEquals(2, diffs.size());

    elements = List.of(new Element("C"));
    diffs = IsotopesUtils.getIsotopesMzDiffs(elements, 1);
    assertEquals(1, diffs.size());

    elements = List.of(new Element("C"), new Element("Cl"));
    diffs = IsotopesUtils.getIsotopesMzDiffs(elements, 2);
    assertEquals(4, diffs.size());
  }

  /**
   * Check that all signals in a complex isotope pattern are marked as isotopes
   */
  @Test
  void isPossibleIsotopeMz() {
    List<Element> elements = List.of(new Element("C"), new Element("Br"));
    List<Double> diffs = IsotopesUtils.getIsotopesMzDiffs(elements, 1);
    MZTolerance mzTol = new MZTolerance(0.002, 5);
    final List<DataPoint> isotopes = getBr3C10IsotopePatter();

    // add first to known and check if rest is a possible isotope
    List<DataPoint> knownMzs = new ArrayList<>();
    knownMzs.add(isotopes.remove(0));

    for (DataPoint dp : isotopes) {
      assertTrue(IsotopesUtils.isPossibleIsotopeMz(dp.getMZ(), knownMzs, diffs, mzTol),
          "Isotope was not detected as possible isotope");
      knownMzs.add(dp);
    }
  }

  @Test
  void findsIsotopePatternStartingAtPeak3() {
    List<Element> elements = List.of(new Element("C"), new Element("Br"));
    List<Double> diffs = IsotopesUtils.getIsotopesMzDiffsCombined(elements, 1,
        new MZTolerance(0.006, 20));
    MZTolerance mzTol = new MZTolerance(0.002, 5);
    final List<DataPoint> isotopes = getBr3C10IsotopePatter();

    final SimpleMassList spec = toMassSpec(isotopes, true);

    // find all isotopes starting at dp 3 which is the max
    final List<DataPoint> detected = IsotopesUtils.findIsotopesInScan(diffs, mzTol, spec,
        isotopes.get(2));

    assertEquals(isotopes, detected);

    // find all isotopes starting at dp 7 which is very low
    final List<DataPoint> detected2 = IsotopesUtils.findIsotopesInScan(diffs, mzTol, spec,
        isotopes.get(6));

    assertEquals(isotopes, detected2);
  }

  public List<DataPoint> getC300H600IsotopePattern() {
    return new ArrayList<>(
        List.of(new SimpleDataPoint(4204.6945, 17.74), new SimpleDataPoint(4205.6978, 57.56),
            new SimpleDataPoint(4205.7007, 1.22), new SimpleDataPoint(4206.7012, 93.08),
            new SimpleDataPoint(4206.7041, 3.97), new SimpleDataPoint(4207.7045, 100.00),
            new SimpleDataPoint(4207.7075, 6.42), new SimpleDataPoint(4208.7079, 80.31),
            new SimpleDataPoint(4208.7108, 6.90), new SimpleDataPoint(4209.7112, 51.42),
            new SimpleDataPoint(4209.7142, 5.54), new SimpleDataPoint(4210.7146, 27.34),
            new SimpleDataPoint(4210.7175, 3.55), new SimpleDataPoint(4211.7180, 12.42),
            new SimpleDataPoint(4211.7209, 1.89), new SimpleDataPoint(4212.7213, 4.92),
            new SimpleDataPoint(4213.7247, 1.73)));

  }

  public List<DataPoint> getC12H24O12IsotopePattern() {
    return new ArrayList<>(
        List.of(new SimpleDataPoint(360.1262, 100.00), new SimpleDataPoint(361.1296, 13.44),
            new SimpleDataPoint(361.1325, 0.28), new SimpleDataPoint(362.1305, 2.47),
            new SimpleDataPoint(362.1330, 0.83), new SimpleDataPoint(363.1339, 0.33)));
  }

  public List<DataPoint> getC120H200IsotopePattern() {
    return new ArrayList<>(List.of(new SimpleDataPoint(1641.5645, 77.05), // %	[12]C120[1]H200
        new SimpleDataPoint(1642.5678, 100.00), // %	[12]C119[13]C[1]H200
        new SimpleDataPoint(1642.5707, 1.77), // %	[12]C120[1]H199[2]H
        new SimpleDataPoint(1643.5712, 64.35), // %	[12]C118[13]C2[1]H200
        new SimpleDataPoint(1643.5741, 2.30), // %	[12]C119[13]C[1]H199[2]H
        new SimpleDataPoint(1644.5745, 27.38), // %	[12]C117[13]C3[1]H200
        new SimpleDataPoint(1644.5774, 1.48), // %	[12]C118[13]C2[1]H199[2]H
        new SimpleDataPoint(1645.5779, 8.66), // %	[12]C116[13]C4[1]H200
        new SimpleDataPoint(1646.5812, 2.17)));// %	[12]C115[13]C5[1]H200
  }

  public List<DataPoint> getBr3C10IsotopePatter() {
    return new ArrayList<>(List.of(new SimpleDataPoint(356.7545, 34.27), //	[12]C10[79]Br3
        new SimpleDataPoint(357.7578, 3.71), //	[12]C9[13]C[79]Br3
        new SimpleDataPoint(358.7524, 100.00), //	[12]C10[79]Br2[81]Br
        new SimpleDataPoint(358.7612, 0.18), //	[12]C8[13]C2[79]Br3
        new SimpleDataPoint(359.7558, 10.82), //	[12]C9[13]C[79]Br2[81]Br
        new SimpleDataPoint(360.7504, 97.28), //	[12]C10[79]Br[81]Br2
        new SimpleDataPoint(360.7591, 0.53), //	[12]C8[13]C2[79]Br2[81]Br
        new SimpleDataPoint(361.7537, 10.52), //	[12]C9[13]C[79]Br[81]Br2
        new SimpleDataPoint(362.7483, 31.54), //	[12]C10[81]Br3
        new SimpleDataPoint(362.7571, 0.51), //	[12]C8[13]C2[79]Br[81]Br2
        new SimpleDataPoint(363.7517, 3.41), //	[12]C9[13]C[81]Br3
        new SimpleDataPoint(364.7550, 0.17)));  //	[12]C8[13]C2[81]Br3
  }

  public SimpleMassList toMassSpec(List<DataPoint> isotopes, boolean addDecoy) {
    if (addDecoy) {
      isotopes = new ArrayList<>(isotopes);
      isotopes.add(7, new SimpleDataPoint(361.7, 20));
      isotopes.add(7, new SimpleDataPoint(360.9, 20));
      isotopes.add(2, new SimpleDataPoint(358.5, 20));

      isotopes.add(0, new SimpleDataPoint(350.7, 20));
      isotopes.add(1, new SimpleDataPoint(354.2, 20));
      isotopes.add(2, new SimpleDataPoint(354.6, 20));

      isotopes.add(new SimpleDataPoint(364.85, 20));
      isotopes.add(new SimpleDataPoint(365.1, 20));
      isotopes.add(new SimpleDataPoint(365.6, 20));
      isotopes.add(new SimpleDataPoint(365.92, 20));
      isotopes.add(new SimpleDataPoint(366.4, 20));
      isotopes.add(new SimpleDataPoint(366.85, 20));
      isotopes.add(new SimpleDataPoint(366.85, 20));
      isotopes.sort(new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending));
    }

    double[][] mzIntensity = DataPointUtils.getDataPointsAsDoubleArray(isotopes);
    return new SimpleMassList(null, mzIntensity[0], mzIntensity[1]);
  }

  private IsotopePattern getPattern(List<DataPoint> dataPoints, int charge) {
    return new SimpleIsotopePattern(dataPoints.toArray(DataPoint[]::new), charge,
        IsotopePatternStatus.DETECTED, "");
  }

  @Test
  void testFilter13CMultiChargePattern() {
    final MZTolerance mzTol = new MZTolerance(0.0015, 10);

    final Isotope[] isotope18O = new Isotope[]{IsotopesUtils.getIsotopeRecord("O", 18),
        IsotopesUtils.getIsotopeRecord("H", 2)};
    final List<List<DataPoint>> patterns = List.of(getC12H24O12IsotopePattern(),
        getC120H200IsotopePattern(), getC300H600IsotopePattern());

    // test the multi charge isotope pattern
    for (int p = 0; p < patterns.size(); p++) {
      final List<DataPoint> dataPoints = patterns.get(p);

      // we only add one pattern here just to test basic functionality
      MultiChargeStateIsotopePattern multi = new MultiChargeStateIsotopePattern(
          getPattern(dataPoints, 1));

      assertTrue(IsotopePatternUtils.check13CPattern(multi, multi.getMzValue(0), mzTol, 4),
          String.format(
              "Monoisotopic peak of pattern p=%d was falsely identified as potential isotope signal in multi charge pattern",
              p));
      assertTrue(IsotopePatternUtils.check13CPattern(multi, multi.getMzValue(0) + 0.001, mzTol, 4),
          String.format(
              "Monoisotopic peak of pattern p=%d was falsely identified as potential isotope signal in multi charge pattern",
              p));
      assertTrue(IsotopePatternUtils.check13CPattern(multi, multi.getMzValue(0) - 0.001, mzTol, 4),
          String.format(
              "Monoisotopic peak of pattern p=%d was falsely identified as potential isotope signal in multi charge pattern",
              p));
      for (int i = 1; i < multi.getNumberOfDataPoints(); i++) {
        // all other should find a previous signal
        assertFalse(IsotopePatternUtils.check13CPattern(multi, multi.getMzValue(i), mzTol, 4, true,
            isotope18O, true), String.format(
            "Isotope peak %d of pattern p=%d was falsely identified as potential monoisotopic signal in multi charge pattern",
            i, p));
      }
    }
  }

  @Test
  void testFilter13C() {
    final MZTolerance mzTol = new MZTolerance(0.0015, 10);

    final Isotope[] isotope18O = new Isotope[]{IsotopesUtils.getIsotopeRecord("O", 18),
        IsotopesUtils.getIsotopeRecord("H", 2)};
    final List<List<DataPoint>> patterns = List.of(getC12H24O12IsotopePattern(),
        getC120H200IsotopePattern(), getC300H600IsotopePattern());

    // test each individual isotope pattern
    for (int p = 0; p < patterns.size(); p++) {
      final List<DataPoint> dataPoints = patterns.get(p);
      for (int charge = 1; charge <= 4; charge++) {
        IsotopePattern pattern = getPattern(
            charge > 1 ? applyCharge(dataPoints, charge) : dataPoints, charge);
        assertTrue(IsotopePatternUtils.check13CPattern(pattern, pattern.getMzValue(0), mzTol, 4),
            String.format(
                "Monoisotopic peak of pattern p=%d was falsely identified as potential isotope signal at charge %d",
                p, charge));
        assertTrue(
            IsotopePatternUtils.check13CPattern(pattern, pattern.getMzValue(0) + 0.001, mzTol, 4),
            String.format(
                "Monoisotopic peak of pattern p=%d was falsely identified as potential isotope signal at charge %d",
                p, charge));
        assertTrue(
            IsotopePatternUtils.check13CPattern(pattern, pattern.getMzValue(0) - 0.001, mzTol, 4),
            String.format(
                "Monoisotopic peak of pattern p=%d was falsely identified as potential isotope signal at charge %d",
                p, charge));
        for (int i = 1; i < pattern.getNumberOfDataPoints(); i++) {
          // all other should find a previous signal
          assertFalse(
              IsotopePatternUtils.check13CPattern(pattern, pattern.getMzValue(i), mzTol, 4, true,
                  isotope18O, true), String.format(
                  "Isotope peak %d of pattern p=%d was falsely identified as potential monoisotopic signal at charge %d",
                  i, p, charge));
        }
      }
    }

    final Isotope[] br = new Isotope[]{IsotopesUtils.getIsotopeRecord("Br", 81)};
    final IsotopePattern br3C10 = getPattern(getBr3C10IsotopePatter(), 1);
    // check different main m/z (owuld be row m/z)
    double mz1 = br3C10.getMzValue(0);
    assertTrue(IsotopePatternUtils.check13CPattern(br3C10, mz1, mzTol, 3));
    // for the Br containing pattern the +2 peak is at 100 % currently it cannot be destiguished from the monoisotopic peak - it returns true
    for (int i = 1; i < br3C10.getNumberOfDataPoints(); i++) {
      // all other should find a previous signal
      assertFalse(
          IsotopePatternUtils.check13CPattern(br3C10, br3C10.getMzValue(i), mzTol, 2, true, br, true), String.format("Br3C10 isotope signal i=%d detected as main", i));
    }

    // change up some of the intensities - should be false in the end
    // first and second data point similar intensity
    final List<DataPoint> dps = getC12H24O12IsotopePattern();
    final DataPoint old = dps.remove(0);
    dps.add(0, new SimpleDataPoint(old.getMZ(), dps.get(0).getIntensity() * 1.2));
    IsotopePattern pattern = getPattern(dps, 1);
    assertFalse(IsotopePatternUtils.check13CPattern(pattern, pattern.getMzValue(0), mzTol, 2, true,
        isotope18O, true));

  }

  @Test
  void testBinarySearchMassSpectrum() {
    final IsotopePattern isotopes = getPattern(getBr3C10IsotopePatter(), 1);
    final double mz3 = isotopes.getMzValue(3);

    assertEquals(3, isotopes.binarySearch(mz3, true));
    assertEquals(3, isotopes.binarySearch(mz3, false));
    assertEquals(3, isotopes.binarySearch(mz3 + 0.00001, true));
    assertEquals(-5, isotopes.binarySearch(mz3 + 0.00001, false));
    assertEquals(3, isotopes.binarySearch(mz3 - 0.00001, true));
    assertEquals(-4, isotopes.binarySearch(mz3 - 0.00001, false));

    assertEquals(0, isotopes.binarySearch(isotopes.getMzValue(0), true));
    assertEquals(0, isotopes.binarySearch(isotopes.getMzValue(0), false));
    // out of bounds
    assertEquals(0, isotopes.binarySearch(isotopes.getMzValue(0) - 0.0001, true));
    assertEquals(-1, isotopes.binarySearch(isotopes.getMzValue(0) - 0.0001, false));

    final int size = isotopes.getNumberOfDataPoints();
    assertEquals(size - 1, isotopes.binarySearch(isotopes.getMzValue(size - 1), true));
    assertEquals(size - 1, isotopes.binarySearch(isotopes.getMzValue(size - 1), false));
    // out of bounds
    assertEquals(size - 1, isotopes.binarySearch(isotopes.getMzValue(size - 1) + 0.001, true));
    assertEquals(-(size + 1), isotopes.binarySearch(isotopes.getMzValue(size - 1) + 0.001, false));
  }

  @Test
  void testGetIsotope() {
    final IIsotope o18 = IsotopesUtils.getIsotopes("O", 18);
    assertTrue(o18.getNaturalAbundance() > 0);
    assertEquals(8, o18.getAtomicNumber());
    assertEquals(18, o18.getMassNumber());
    final IIsotope c13 = IsotopesUtils.getIsotopes("C", 13);
    assertTrue(c13.getNaturalAbundance() > 1);

    // nonsense
    final IIsotope[] nothing = IsotopesUtils.getIsotopes("CBr");
    assertEquals(0, nothing.length);
  }

  private List<DataPoint> applyCharge(List<DataPoint> dps, int charge) {
    return dps.stream()
        .map(dp -> (DataPoint) new SimpleDataPoint(dp.getMZ() / charge, dp.getIntensity()))
        .toList();
  }


}