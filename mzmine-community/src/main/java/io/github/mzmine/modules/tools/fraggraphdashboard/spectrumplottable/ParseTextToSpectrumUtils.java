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

package io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;

public class ParseTextToSpectrumUtils {

  private static final Logger logger = Logger.getLogger(ParseTextToSpectrumUtils.class.getName());

  public static MassSpectrum parseStringToSpectrum(String signalList) {
    if (StringUtils.isBlank(signalList)) {
      return MassSpectrum.EMPTY;
    }

    final List<DataPoint> dataPoints = new ArrayList<>();

    final String[] lines = signalList.split("\n");
    for (String line : lines) {
      String[] mzIntensity = StringUtils.splitAnyCommaTabSpace(line);
      if (mzIntensity.length < 2) {
        continue;
      }

      try {
        final double mz = Double.parseDouble(mzIntensity[0]);
        final double intensity = Double.parseDouble(mzIntensity[1]);
        dataPoints.add(new SimpleDataPoint(mz, intensity));
      } catch (Exception e) {
        logger.log(Level.WARNING, "Error parsing line, expecting tab or comma delimiter: " + line,
            e);
      }
    }

    final double[][] dps = DataPointUtils.getDataPointsAsDoubleArray(dataPoints);
    return new SimpleMassSpectrum(dps[0], dps[1]);
  }

  public static String spectrumToString(MassSpectrum spectrum) {
    if (spectrum == null) {
      return "";
    }

    return IntStream.range(0, spectrum.getNumberOfDataPoints())
        .mapToObj(index -> concatDataPoint(spectrum, index)).collect(Collectors.joining("\n"));
  }

  private static @NotNull String concatDataPoint(final MassSpectrum spectrum, final int i) {
    final NumberFormats formats = ConfigService.getExportFormats();
    return formats.mz(spectrum.getMzValue(i)) + "\t" + formats.intensity(
        spectrum.getIntensityValue(i));
  }

}
