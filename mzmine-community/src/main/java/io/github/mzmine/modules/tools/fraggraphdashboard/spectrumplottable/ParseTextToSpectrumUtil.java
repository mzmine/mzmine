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
 *
 */

package io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.util.DataPointUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParseTextToSpectrumUtil {

  private static Logger logger = Logger.getLogger(ParseTextToSpectrumUtil.class.getName());
  private SimpleMassSpectrum spectrum;

  public static MassSpectrum parseStringToSpectrum(String singalList) {
    if (singalList.isBlank()) {
      return new SimpleMassSpectrum(new double[0], new double[0]);
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
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < spectrum.getNumberOfDataPoints(); i++) {
      b.append(spectrum.getMzValue(i)).append("\t").append(spectrum.getIntensityValue(i));
      if (i < spectrum.getNumberOfDataPoints() - 1) {
        b.append("\n");
      }
    }
    return b.toString();
  }
}
