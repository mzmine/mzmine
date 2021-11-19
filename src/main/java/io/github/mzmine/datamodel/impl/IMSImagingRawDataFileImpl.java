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

import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

public class IMSImagingRawDataFileImpl extends IMSRawDataFileImpl implements IMSImagingRawDataFile {

  //imaging parameters
  private ImagingParameters param;

  // scan numbers
  // TODO add ms level - one array for each level
  private Scan[][][] xyzScanNumbers;


  public IMSImagingRawDataFileImpl(String dataFileName, @Nullable final String absPath,
      @Nullable MemoryMapStorage storage) throws IOException {
    super(dataFileName, absPath, storage);
  }

  @Override
  public ImagingParameters getImagingParam() {
    return param;
  }

  @Override
  public void setImagingParam(ImagingParameters imagingParameters) {
    param = imagingParameters;
  }

  @Override
  public Scan getScan(double x, double y) {
    //
    Scan[][][] numbers = getXYZScanNumbers();
    // yline:
    int iy = (int) ((y) / param.getPixelHeight());
    int ix = (int) (x / param.getPixelWidth());

    if (ix >= 0 && ix < numbers.length && iy >= 0 && iy < numbers[ix].length
        && numbers[ix][iy][0] != null) {
      return numbers[ix][iy][0];
    } else {
      return null;
    }
  }

  @Override
  public List<Scan> getScansInArea(float x, float y, float x2, float y2) {
    //
    float tmp = x;
    x = Math.min(x, x2);
    x2 = Math.max(tmp, x2);
    tmp = y;
    y = Math.min(y, y2);
    y2 = Math.max(tmp, y2);

    Scan[][][] numbers = getXYZScanNumbers();
    int iy = (int) ((y) / param.getPixelHeight());
    int ix = (int) (x / param.getPixelWidth());

    int iy2 = (int) ((y2) / param.getPixelHeight());
    int ix2 = (int) (x2 / param.getPixelWidth());

    List<Scan> list = new ArrayList<>();

    if (ix >= 0 && ix2 < numbers.length && iy >= 0 && iy2 < numbers[ix2].length) {
      for (int i = ix; i <= ix2; i++) {
        for (int k = iy; k <= iy2; k++) {
          if (numbers[i][k][0] != null) {
            list.add(numbers[i][k][0]);
          }
        }
      }
    }
    return list;
  }

  /**
   * xyz array of all scan numbers -1 if there is no scan at this specific position
   *
   * @return
   */
  public Scan[][][] getXYZScanNumbers() {
    if (xyzScanNumbers == null) {
      // sort all scan numbers to xyz location
      xyzScanNumbers = new Scan[param.getMaxNumberOfPixelX()][param.getMaxNumberOfPixelY()][param.getMaxNumberOfPixelZ()];
      for (int x = 0; x < xyzScanNumbers.length; x++) {
        for (int y = 0; y < xyzScanNumbers[x].length; y++) {
          for (int z = 0; z < xyzScanNumbers[x][y].length; z++) {
            xyzScanNumbers[x][y][z] = null;
          }
        }
      }
      // add all scans
      ObservableList<Scan> numbers = getScans();
      for (int i = 0; i < numbers.size(); i++) {
        if (numbers.get(i) instanceof ImagingScan) {
          Coordinates c = ((ImagingScan) numbers.get(i)).getCoordinates();
          if (c.getX() < param.getMaxNumberOfPixelX() && c.getY() < param.getMaxNumberOfPixelY()
              && c.getZ() < param.getMaxNumberOfPixelZ()) {
            xyzScanNumbers[c.getX()][c.getY()][c.getZ()] = numbers.get(i);
          }
        }
      }
    }
    return xyzScanNumbers;
  }
}
