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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

/**
 * This interface defines an isotope pattern which can be attached to a peak
 */
public interface IsotopePattern extends MassSpectrum {


  /**
   * Returns the isotope pattern status.
   */
  @NotNull IsotopePatternStatus getStatus();

  /**
   * Returns a description of this isotope pattern (formula, etc.)
   */
  @NotNull String getDescription();

  /**
   * Appends a new isotope pattern xml element to the current element.
   */
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException;

  /**
   * Create a copy with relative intensities between 0-1
   *
   * @return copy with relative intensities
   */
  default IsotopePattern getRelativeIntensity() {
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

    return new SimpleIsotopePattern(mzs, intensities, getStatus(), getDescription());
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
