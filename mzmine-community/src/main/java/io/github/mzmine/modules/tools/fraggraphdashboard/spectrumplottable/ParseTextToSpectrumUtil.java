/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.DataPointUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParseTextToSpectrumUtil {

  private static Logger logger = Logger.getLogger(ParseTextToSpectrumUtil.class.getName());

  public static MassSpectrum parseStringToSpectrum(String singalList) {
    if (singalList.isBlank()) {
      return MassSpectrum.EMPTY;
    }

    final List<DataPoint> dataPoints = new ArrayList<>();

    final String[] lines = singalList.split("\n");
    for (String line : lines) {
      final String[] mzIntensity = line.split("\t");
      if (mzIntensity.length < 2) {
        continue;
      }

      try {
        final double mz = Double.parseDouble(mzIntensity[0]);
        final double intensity = Double.parseDouble(mzIntensity[1]);
        dataPoints.add(new SimpleDataPoint(mz, intensity));
      } catch (Exception e) {
        logger.log(Level.WARNING, STR."Error parsing line \{line}", e);
      }
    }

    final double[][] dps = DataPointUtils.getDataPointsAsDoubleArray(dataPoints);
    return new SimpleMassSpectrum(dps[0], dps[1]);
  }

  public static String spectrumToString(MassSpectrum spectrum) {
    if (spectrum == null) {
      return "";
    }
    final NumberFormats formats = ConfigService.getExportFormats();
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < spectrum.getNumberOfDataPoints(); i++) {
      b.append(formats.mz(spectrum.getMzValue(i))).append("\t")
          .append(formats.intensity(spectrum.getIntensityValue(i)));
      if (i < spectrum.getNumberOfDataPoints() - 1) {
        b.append("\n");
      }
    }
    return b.toString();
  }
}
