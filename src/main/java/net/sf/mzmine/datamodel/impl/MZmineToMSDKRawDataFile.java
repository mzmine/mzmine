/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.datamodel.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.FileType;
import io.github.msdk.datamodel.MsScan;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

/**
 * Simple implementation of the Scan interface.
 */
public class MZmineToMSDKRawDataFile implements io.github.msdk.datamodel.RawDataFile {

  private final RawDataFile mzmineRawdataFile;
  private final List<MsScan> scans = new ArrayList<>();
  private final List<Chromatogram> chromatograms = new ArrayList<>();

  /**
   * Clone constructor
   */
  public MZmineToMSDKRawDataFile(RawDataFile mzmineRawdataFile) {
    this.mzmineRawdataFile = mzmineRawdataFile;

    int scanNumbers[] = mzmineRawdataFile.getScanNumbers();
    for (int scanNum : scanNumbers) {
      Scan mzmineScan = mzmineRawdataFile.getScan(scanNum);
      MsScan msdkScan = new MZmineToMSDKMsScan(mzmineScan);
      scans.add(msdkScan);
    }

  }


  @Override
  public String getName() {
    return mzmineRawdataFile.getName();
  }


  @Override
  public Optional<File> getOriginalFile() {
    return Optional.empty();
  }


  @Override
  public FileType getRawDataFileType() {
    return FileType.UNKNOWN;
  }


  @Override
  public List<String> getMsFunctions() {
    return Arrays.asList(new String[] {"ms"});
  }


  @Override
  public List<MsScan> getScans() {
    return scans;
  }


  @Override
  public List<Chromatogram> getChromatograms() {
    return chromatograms;
  }


  @Override
  public void dispose() {
    mzmineRawdataFile.close();
  }

}
