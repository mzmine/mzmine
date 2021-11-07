/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TdfImsRawDataFileImpl extends IMSRawDataFileImpl {

  private TDFUtils tdfUtils = null;
  private long handle;
  private final RangeMap<Integer, double[]> segmentMobilities = TreeRangeMap.create();

  public TdfImsRawDataFileImpl(String dataFileName, @Nullable String absolutePath,
      MemoryMapStorage storage) throws IOException {
    super(dataFileName, absolutePath, storage);
  }

  public TdfImsRawDataFileImpl(String dataFileName, @Nullable String absolutePath,
      MemoryMapStorage storage, Color color) throws IOException {
    super(dataFileName, absolutePath, storage, color);
  }

  @NotNull
  public TDFUtils getTdfUtils() {
    if(tdfUtils == null) {
      tdfUtils = new TDFUtils();
      handle = tdfUtils.openFile(new File(getAbsolutePath()), 1);
    }
    return tdfUtils;
  }

  public long getHandle() {
    if(tdfUtils == null) {
      getTdfUtils();
    }
    return handle;
  }

  public void addSegmentMobility(Range<Integer> frameIdRange, double[] mobilities) {
    segmentMobilities.put(frameIdRange, mobilities);
  }

  public double[] getMobilitiesForFrame(int frameId) {
    return segmentMobilities.get(frameId);
  }

  @Override
  public synchronized void close() {
    super.close();
    tdfUtils.close();
  }
}
