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

import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.MsSpectrum;
import io.github.msdk.datamodel.SimpleMsSpectrum;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

/**
 * Class SpectrumScanner
 * Allows to process Feature objects (peaks) and return appropriate objects for SiriusIdentificationMethod
 */
public class SpectrumScanner {
  private final PeakListRow row;
  private final HashMap<String, int[]> fragmentScans;
  private final String massListName; // Used the value from ExportSirius module.

  private List<MsSpectrum> ms1list;
  private List<MsSpectrum> ms2list;

  private boolean includeMs1 = true;

  /**
   * Constructor for SpectrumScanner
   * @param row
   */
  public SpectrumScanner(@Nonnull PeakListRow row, String massListName) throws MSDKException {
    this.row = row;
    this.massListName = massListName;
    ms1list = new LinkedList<>();
    ms2list = new LinkedList<>();

    fragmentScans = getFragmentScans(row.getRawDataFiles());
    processRow();
  }

  /**
   * Function taken from SiriusExportTask class, getFragmentScans()
   * @param rawDataFiles
   * @return
   */
  private HashMap<String,int[]> getFragmentScans(RawDataFile[] rawDataFiles) {
    final HashMap<String, int[]> fragmentScans = new HashMap<>();
    for (RawDataFile r : rawDataFiles) {
      int[] scans = new int[0];
      for (int msLevel : r.getMSLevels()) {
        if (msLevel > 1) {
          int[] concat = r.getScanNumbers(msLevel);
          int offset = scans.length;
          scans = Arrays.copyOf(scans, scans.length + concat.length);
          System.arraycopy(concat, 0, scans, offset, concat.length);
        }
      }
      Arrays.sort(scans);
      fragmentScans.put(r.getName(), scans);
    }
    return fragmentScans;
  }


  /**
   * Method processes a row and construct MS1 MS2 lists
   */
  private void processRow() throws MSDKException {
    // Specify the Isotope Pattern (in form of MS1 spectrum)
    IsotopePattern pattern = row.getBestPeak().getIsotopePattern();
    if (pattern != null) {
      MsSpectrum isotopePattern = buildSpectrum(pattern.getDataPoints());
      ms1list.add(isotopePattern);
    }

    /*
      Process features, retrieve scans and write spectra. Only MS level 2.
      Code taken from SiriusExportTask -> exportPeakListRow(...)
     */
    for (Feature f : row.getPeaks()) {
      final int[] scanNumbers = f.getScanNumbers().clone();
      Arrays.sort(scanNumbers);
      int[] fs = fragmentScans.get(f.getDataFile().getName());
      int startWith = scanNumbers[0];
      int j = Arrays.binarySearch(fs, startWith);
      if (j < 0) j = (-j - 1);
      for (int k = j; k < fs.length; ++k) {
        final Scan scan = f.getDataFile().getScan(fs[k]);
        if (scan.getMSLevel() > 1 && Math.abs(scan.getPrecursorMZ() - f.getMZ()) < 0.1) { //todo: ask about scan.getMSLevel()...
          if (includeMs1) {
            // find precursor scan
            int prec = Arrays.binarySearch(scanNumbers, fs[k]);
            if (prec < 0) prec = -prec - 1;
            prec = Math.max(0, prec - 1);
            for (; prec < scanNumbers.length && scanNumbers[prec] < fs[k]; ++prec) {
              final Scan precursorScan = f.getDataFile().getScan(scanNumbers[prec]);
              if (precursorScan.getMSLevel() == 1) {
                MassList massList = precursorScan.getMassList(massListName);
                if (massList != null) {
                  DataPoint[] points = massList.getDataPoints();
                  if (points.length == 0) {
                    throw new MSDKException("There are no scans for this Mass List");
                  }
                  ms1list.add(buildSpectrum(points));
                }
              }
            }
          }

          // Do not include MS1 scans (except for isotope pattern)
          MassList massList = scan.getMassList(massListName);
          DataPoint[] points = massList.getDataPoints();
          if (points.length == 0) {
            throw new MSDKException("There are no scans for this Mass List");
          }
          ms2list.add(buildSpectrum(points));
        }
      }

//    for (Feature peak: row.getPeaks()) {
//      int ms1index = peak.getRepresentativeScanNumber();
//      int ms2index = peak.getMostIntenseFragmentScanNumber();
//      if (indexExists(ms1index))
//        ms1list.add(buildSpectrum(peak, ms1index));
//
//      if (indexExists(ms2index))
//        ms2list.add(buildSpectrum(peak, ms2index));
//    }
    }
  }

  /**
   * Process RawDataFile and return list with one MsSpectrum of level 1.
   * @return MS spectra list
   */
  public List<MsSpectrum> getMsList() {
    if (ms1list.size() == 0)
      return null;
    return ms1list;
  }

  /**
   * Process RawDataFile and return list with one MsSpectrum of level 2.
   * @return MSMS spectra list
   */
  public List<MsSpectrum> getMsMsList() {
    if (ms2list.size() == 0)
      return null;
    return ms2list;
  }

  public boolean peakContainsMsMs() {
    return ms2list.size() > 0;
  }

  /**
   * Check the existence of spectrum
   * @param index - equals to -1, if no ms or ms/ms spectra is found
   * @return
   */
  private boolean indexExists(int index) {
    return index >= 0;
  }

  /**
   * Construct MsSpectrum object from Feature object and index of its Scan
   * @param peak - to retrieve data points from
   * @param index - index of the scan in the Data File from Feature object
   * @return new MsSpectrum object
   */
  private MsSpectrum buildSpectrum(Feature peak, int index) {
    Scan scan = peak.getDataFile().getScan(index);
    DataPoint[] points = scan.getDataPoints();

    return buildSpectrum(points);
  }

  /**
   * Construct MsSpectrum object from DataPoint array
   * @param points MZ/Intensity pairs
   * @return new MsSpectrum
   */
  private MsSpectrum buildSpectrum(DataPoint[] points) {
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
}
