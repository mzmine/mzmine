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

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.msdk.Chromatogram;
import io.github.mzmine.datamodel.msdk.FileType;
import io.github.mzmine.datamodel.msdk.MsScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javafx.collections.ObservableList;

/**
 * Simple implementation of the Scan interface.
 */
public class MZmineToMSDKRawDataFile implements io.github.mzmine.datamodel.msdk.RawDataFile {

  private final RawDataFile mzmineRawdataFile;
  private final List<MsScan> scans = new ArrayList<>();
  private final List<Chromatogram> chromatograms = new ArrayList<>();

  /**
   * Clone constructor
   */
  public MZmineToMSDKRawDataFile(RawDataFile mzmineRawdataFile) {
    this.mzmineRawdataFile = mzmineRawdataFile;

    ObservableList<Scan> scanNumbers = mzmineRawdataFile.getScans();
    for (Scan mzmineScan : scanNumbers) {
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
