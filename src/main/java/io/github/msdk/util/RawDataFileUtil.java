/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.msdk.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.RawDataFile;

/**
 * <p>
 * RawDataFileUtil class.
 * </p>
 */
public class RawDataFileUtil {

  /**
   * <p>
   * getScans.
   * </p>
   *
   * @param rawDataFile a {@link RawDataFile} object.
   * @param msFunction a {@link io.github.msdk.datamodel.rawdata.MsFunction} object.
   * @return a {@link List} object.
   */
  @Nonnull
  static public List<MsScan> getScans(RawDataFile rawDataFile, String msFunction) {
    ArrayList<MsScan> msScanList = new ArrayList<MsScan>();
    List<MsScan> scans = rawDataFile.getScans();
    synchronized (scans) {
      for (MsScan scan : scans) {
        String scanMsFunction = scan.getMsFunction();
        if (scanMsFunction == null)
          continue;
        if (scanMsFunction.equals(msFunction))
          msScanList.add(scan);
      }
    }
    return msScanList;
  }

  /**
   * <p>
   * getScans.
   * </p>
   *
   * @param rawDataFile a {@link RawDataFile} object.
   * @param rtRange a {@link Range} object.
   * @return a {@link List} object.
   */
  @Nonnull
  static public List<MsScan> getScans(RawDataFile rawDataFile, Range<Float> rtRange) {
    ArrayList<MsScan> msScanList = new ArrayList<MsScan>();
    List<MsScan> scans = rawDataFile.getScans();
    synchronized (scans) {
      for (MsScan scan : scans) {
        Float scanRT = scan.getRetentionTime();
        if (scanRT != null) {
          if (rtRange.contains(scanRT))
            msScanList.add(scan);
        }
      }
    }
    return new ArrayList<MsScan>();
  }

  /**
   * <p>
   * getScans.
   * </p>
   *
   * @param rawDataFile a {@link RawDataFile} object.
   * @param msFunction a {@link io.github.msdk.datamodel.rawdata.MsFunction} object.
   * @param rtRange a {@link Range} object.
   * @return a {@link List} object.
   */
  @Nonnull
  static public List<MsScan> getScans(RawDataFile rawDataFile, String msFunction,
      Range<Float> rtRange) {
    ArrayList<MsScan> msScanList = new ArrayList<MsScan>();
    List<MsScan> scans = rawDataFile.getScans();
    synchronized (scans) {
      for (MsScan scan : scans) {
        Float scanRT = scan.getRetentionTime();
        String scanMsFunction = scan.getMsFunction();
        if (scanRT == null || scanMsFunction == null)
          continue;
        if (scanMsFunction.equals(msFunction) && rtRange.contains(scanRT))
          msScanList.add(scan);
      }
    }
    return new ArrayList<MsScan>();
  }

  /**
   * <p>
   * getNextChromatogramNumber.
   * </p>
   *
   * @param rawDataFile a {@link RawDataFile} object.
   * @return a {@link Integer} object.
   */
  @Nonnull
  static public Integer getNextChromatogramNumber(RawDataFile rawDataFile) {
    int chromatogramNumber = 1;
    List<Chromatogram> chromatograms = rawDataFile.getChromatograms();
    for (Chromatogram chromatogram : chromatograms) {
      int currentChromatogramNumber = chromatogram.getChromatogramNumber();
      if (currentChromatogramNumber > chromatogramNumber)
        chromatogramNumber = currentChromatogramNumber;
    }
    return chromatogramNumber;
  }
}
