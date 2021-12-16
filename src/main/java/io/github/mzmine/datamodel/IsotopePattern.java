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

import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.main.MZmineCore;
import java.io.IOException;
import java.io.StringReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface defines an isotope pattern which can be attached to a peak
 */
public interface IsotopePattern extends MassSpectrum {

  static final Logger logger = Logger.getLogger(IsotopePattern.class.getName());

  public static @Nullable IsotopePattern fromCSV(String isotopes) {
    if (isotopes == null || isotopes.isBlank()) {
      return null;
    }
    try {
      // try spaces
      CSVParser reader = new CSVParser(new StringReader(isotopes), CSVFormat.DEFAULT);

      List<DataPoint> dps = new ArrayList<>();
      for (CSVRecord record : reader) {
        if (record.size() >= 2) {
          try {
            double mz = Double.parseDouble(record.get(0));
            double intensity = Double.parseDouble(record.get(1));
            dps.add(new SimpleDataPoint(mz, intensity));
          } catch (Exception ex) {
            if (dps.size() > 0) {
              throw new IllegalArgumentException("Cannot parse isotopes from " + isotopes, ex);
            }
          }
        }
      }
      return dps.isEmpty() ? null
          : new SimpleIsotopePattern(dps.toArray(new DataPoint[0]), IsotopePatternStatus.DETECTED,
              "detected pattern");
    } catch (IOException e) {
      // should not be of interest in a string reader
      logger.log(Level.WARNING, e.getMessage(), e);
      return null;
    }
  }

  /**
   * Create comma separated string
   *
   * @return
   */
  default String toCSV() {
    final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < getNumberOfDataPoints(); i++) {
      b.append(mzFormat.format(getMzValue(i)));
      b.append(",");
      b.append(intensityFormat.format(getIntensityValue(i)));
      b.append("\n");
    }
    return b.toString();
  }

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
