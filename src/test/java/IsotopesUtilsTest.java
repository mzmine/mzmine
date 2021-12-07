/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.IsotopesUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Element;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
class IsotopesUtilsTest {

  /**
   * Test that all mz diffs are positive
   */
  @Test
  void testIsotopesMzDiffsNotNegative() {
    List<Element> elements = List
        .of(new Element("C"), new Element("Cl"), new Element("Fe"), new Element("Ca"),
            new Element("Cu"), new Element("Gd"), new Element("Sn"));
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
    List<Double> diffs = IsotopesUtils
        .getIsotopesMzDiffsCombined(elements, 1, new MZTolerance(0.006, 20));
    MZTolerance mzTol = new MZTolerance(0.002, 5);
    final List<DataPoint> isotopes = getBr3C10IsotopePatter();

    final SimpleMassList spec = toMassSpec(isotopes, true);

    // find all isotopes starting at dp 3 which is the max
    final List<DataPoint> detected = IsotopesUtils
        .findIsotopesInScan(diffs, mzTol, spec, isotopes.get(2));

    assertEquals(isotopes, detected);

    // find all isotopes starting at dp 7 which is very low
    final List<DataPoint> detected2 = IsotopesUtils
        .findIsotopesInScan(diffs, mzTol, spec, isotopes.get(6));

    assertEquals(isotopes, detected2);
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
}