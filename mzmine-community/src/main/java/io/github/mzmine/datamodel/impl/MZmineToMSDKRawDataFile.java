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

package io.github.mzmine.datamodel.impl;

import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.FileType;
import io.github.msdk.datamodel.MsScan;
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
public class MZmineToMSDKRawDataFile implements io.github.msdk.datamodel.RawDataFile {

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
