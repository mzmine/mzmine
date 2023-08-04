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
