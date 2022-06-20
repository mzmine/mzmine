/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.util.io;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class CSVUtils {

  /**
   * @param input     the input string to be escaped
   * @param separator if the separator is found in the input string, quotes are added surrounding
   *                  the field
   * @return escaped, csv safe string or empty string for null
   */
  public static String escape(String input, String separator) {
    if (input == null) {
      return "";
    } else {
      // Remove all special characters (particularly \n would mess up our CSV
      // format).
      String res = input.replaceAll("[\\p{Cntrl}]", " ");

      // Skip too long strings (see Excel 2007 specifications)
      if (res.length() >= 32766) {
        res = res.substring(0, 32765);
      }

      // "" for quotes in text
      if (input.contains(separator) || input.contains("\"")) {
        return "\"" + input.replaceAll("\"", "\"\"") + "\"";
      } else {
        return res;
      }
    }
  }

  /**
   * Get formatted list of mz
   *
   * @param spec spectrum to extract the information from
   * @param sep  separator
   * @return [a, b, c] with formatted numbers based on the configuration
   */
  public static String getMzListFormatted(MassSpectrum spec, String sep) {
    return getListString(spec.getMzValues(new double[spec.getNumberOfDataPoints()]), sep,
        MZmineCore.getConfiguration().getMZFormat());
  }

  /**
   * Get formatted list of intensities
   *
   * @param spec spectrum to extract the information from
   * @param sep  separator
   * @return [a, b, c] with formatted numbers based on the configuration
   */
  public static String getIntensityListFormatted(MassSpectrum spec, String sep) {
    return getListString(spec.getIntensityValues(new double[spec.getNumberOfDataPoints()]), sep,
        MZmineCore.getConfiguration().getIntensityFormat());
  }

  /**
   * Get formatted list of mz
   *
   * @param list   the list of values to be formatted
   * @param format the number format
   * @param sep    separator
   * @return [a, b, c] with formatted numbers based on the configuration
   */
  public static String getListString(double[] list, String sep, NumberFormat format) {
    return String.format("[%s]",
        Arrays.stream(list).mapToObj(format::format).collect(Collectors.joining(sep)));
  }
}
