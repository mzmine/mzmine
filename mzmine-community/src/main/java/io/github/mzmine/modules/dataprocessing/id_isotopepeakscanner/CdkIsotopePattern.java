/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import org.openscience.cdk.formula.IsotopeContainer;
import org.openscience.cdk.formula.IsotopePattern;

public record CdkIsotopePattern(org.openscience.cdk.formula.IsotopePattern isotopes,
                                int majorIsotopeIndex) {

  public CdkIsotopePattern(final IsotopePattern isotopes) {
    this(isotopes, findMajorIsotopeIndex(isotopes));
  }

  public double majorIsotopeMass() {
    return majorIsotopeIndex == -1 ? 0 : majorIsotope().getMass();
  }

  public double majorIsotopeIntensity() {
    return majorIsotopeIndex == -1 ? 0 : majorIsotope().getIntensity();
  }

  private IsotopeContainer majorIsotope() {
    return majorIsotopeIndex == -1 ? null : isotopes.getIsotope(majorIsotopeIndex);
  }

  public double getRelativeIntensity(int index) {
    double max = majorIsotopeIntensity();
    return max == 0 ? 0 : isotopes.getIsotope(index).getIntensity() / max;
  }

  public double getMassRelativeToMajor(int index) {
    double mass = majorIsotopeMass();
    return mass == 0 ? 0 : isotopes.getIsotope(index).getMass() - mass;
  }

  public double getMzRelativeToMajor(int index, int charge) {
    return getMassRelativeToMajor(index) / Math.abs(charge);
  }

  public int getNumberOfIsotopes() {
    return isotopes.getNumberOfIsotopes();
  }


  public static int findMajorIsotopeIndex(final IsotopePattern isotopes) {
    int maxI = -1;
    double maxIntensity = 0;
    for (int i = 0; i < isotopes.getIsotopes().size(); i++) {
      var isotope = isotopes.getIsotope(i);
      double intensity = isotope.getIntensity();
      if (intensity > maxIntensity) {
        maxI = i;
        maxIntensity = intensity;
      }
    }
    return maxI;
  }

  public DataPoint[] translateMajorIsotopeTo(final Double targetMz, final float targetIntensity,
      final int charge) {
    DataPoint[] dps = new DataPoint[getNumberOfIsotopes()];
    for (int i = 0; i < dps.length; i++) {
      dps[i] = new SimpleDataPoint(targetMz + getMzRelativeToMajor(i, charge),
          getRelativeIntensity(i) * targetIntensity);
    }
    return dps;
  }
}
