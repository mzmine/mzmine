/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import io.github.msdk.datamodel.MsSpectrum;
import io.github.msdk.datamodel.SimpleMsSpectrum;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

public class SpectrumProcessing {
  public static List<MsSpectrum> processRawScan(RawDataFile rawfile, int index) {
    LinkedList<MsSpectrum> spectra = null;
    if (index != -1) {
      spectra = new LinkedList<>();
      Scan scan = rawfile.getScan(index);
      DataPoint[] points = scan.getDataPoints();
      MsSpectrum ms = buildSpectrum(points);
      spectra.add(ms);
    }

    return spectra;
  }

  public static boolean rowContainsMsMs(int ms2index) {
    return ms2index != -1; // equals -1, if no ms2 spectra is found
  }

  private static MsSpectrum buildSpectrum(DataPoint[] points) {
    SimpleMsSpectrum spectrum = new SimpleMsSpectrum();
    double mz[] = new double[points.length];
    float intensity[] = new float[points.length];

    for (int i = 0; i < points.length; i++) {
      mz[i] = points[i].getMZ();
      intensity[i] = (float) points[i].getIntensity();
    }

    spectrum.setDataPoints(mz, intensity, points.length);
    return spectrum;
  }

  public static void saveSpectrum(RawDataFile rawfile, int index, String filename) {
    Scan scan = rawfile.getScan(index);
    DataPoint[] points = scan.getDataPoints();

    try {
      FileWriter fw = new FileWriter(new File(filename));
      for (DataPoint point: points)
        fw.write(String.format("%f %f\n", point.getMZ(), point.getIntensity()));
      fw.close();
    } catch (Exception e) {
      System.out.println("Suffering");
    }
  }

  private SpectrumProcessing() {}
}
