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

package io.github.mzmine.project.impl;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;


public class ImagingRawDataFileImpl extends RawDataFileImpl implements ImagingRawDataFile {

  public static final String SAVE_IDENTIFIER = "Imaging Raw data file";

  // imaging parameters
  private ImagingParameters param;

  // scan numbers
  // TODO add ms level - one array for each level
  private Scan[][][] xyzScanNumbers;


  public ImagingRawDataFileImpl(String dataFileName, @Nullable final String absPath,
      MemoryMapStorage storage) {
    super(dataFileName, absPath, storage);
  }

  @Override
  public void setImagingParam(ImagingParameters imagingParameters) {
    param = imagingParameters;
  }

  @Override
  public ImagingParameters getImagingParam() {
    return param;
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
      xyzScanNumbers = new Scan[param.getMaxNumberOfPixelX()][param.getMaxNumberOfPixelY()][param
          .getMaxNumberOfPixelZ()];
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
