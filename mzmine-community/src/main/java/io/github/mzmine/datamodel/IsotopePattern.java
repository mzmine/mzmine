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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import java.util.Comparator;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

/**
 * This interface defines an isotope pattern which can be attached to a peak
 */
public interface IsotopePattern extends MassSpectrum {

  /**
   * Comparator for sorting isotope patterns by their size in descending order, and then by charge
   * state in ascending order. Charge state -1 (not detected) is considered as the highest charge
   * state.
   */
  Comparator<IsotopePattern> patternSizeComparator = Comparator.comparingInt(
          IsotopePattern::getNumberOfDataPoints).reversed()
      .thenComparingInt(ip -> ip.getCharge() == -1 ? Integer.MAX_VALUE : ip.getCharge());
  /**
   * Sorts by {@link IsotopePattern#getScore() score}, best first. Unscored patterns
   * ({@link Double#NaN}, e.g. predicted ones) sort last and then fall back to
   * {@link #patternSizeComparator}, which preserves the legacy ordering when nothing is scored.
   */
  Comparator<IsotopePattern> patternScoreComparator = Comparator.comparingDouble(
          // NaN ranks as the worst score, so a scored pattern always outranks an unscored one
          (IsotopePattern ip) -> Double.isNaN(ip.getScore()) ? Double.NEGATIVE_INFINITY : ip.getScore())
      .reversed() // higher score first
      .thenComparing(patternSizeComparator);

  /**
   * The charge state for the detected pattern
   *
   * @return charge or -1 if not defined
   */
  int getCharge();

  /**
   * Returns the isotope pattern status.
   */
  @NotNull IsotopePatternStatus getStatus();

  /**
   * Returns a description of this isotope pattern (formula, etc.)
   */
  @NotNull String getDescription();

  /**
   * Quality score, higher is better, used to rank the charge-state hypotheses of a
   * {@link MultiChargeStateIsotopePattern}.
   *
   * @return the score, or {@link Double#NaN} for an unscored pattern such as a predicted one.
   */
  default double getScore() {
    return Double.NaN;
  }

  /**
   * Appends a new isotope pattern xml element to the current element.
   */
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException;

  /**
   * Create a copy with relative intensities between 0-1
   *
   * @return copy with relative intensities
   */
  default IsotopePattern getRelativeIntensityCopy() {
    final int size = getNumberOfDataPoints();
    double[] mzs = new double[size];
    double[] intensities = new double[size];
    double max = 0;
    for (int i = 0; i < size; i++) {
      final double v = getIntensityValue(i);
      if (v > max) {
        max = v;
      }
    }
    for (int i = 0; i < size; i++) {
      intensities[i] = getIntensityValue(i) / max;
      mzs[i] = getMzValue(i);
    }

    return new SimpleIsotopePattern(mzs, intensities, getCharge(), getScore(), getStatus(),
        getDescription());
  }

  public enum IsotopePatternStatus {

    /**
     * Isotope pattern was detected by isotope grouper
     */
    DETECTED,

    /**
     * Isotope pattern was predicted by Isotope pattern calculator
     */
    PREDICTED;

  }
}
