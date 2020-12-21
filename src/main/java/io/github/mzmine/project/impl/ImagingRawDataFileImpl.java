package io.github.mzmine.project.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.Coordinates;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.ImagingParameters;


public class ImagingRawDataFileImpl extends RawDataFileImpl implements ImagingRawDataFile {

  // imaging parameters
  private ImagingParameters param;

  // scan numbers
  // TODO add ms level - one array for each level
  private int[][][] xyzScanNumbers;


  public ImagingRawDataFileImpl(String dataFileName) throws IOException {
    super(dataFileName);
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
  public Scan getScan(float x, float y) {
    //
    int[][][] numbers = getXYZScanNumbers();
    // yline:
    int iy = (int) ((y) / param.getPixelShape());
    int ix = (int) (x / param.getPixelWidth());

    if (ix >= 0 && ix < numbers.length && iy >= 0 && iy < numbers[ix].length
        && numbers[ix][iy][0] != -1)
      return getScan(numbers[ix][iy][0]);
    else
      return null;
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

    int[][][] numbers = getXYZScanNumbers();
    int iy = (int) ((y) / param.getPixelShape());
    int ix = (int) (x / param.getPixelWidth());

    int iy2 = (int) ((y2) / param.getPixelShape());
    int ix2 = (int) (x2 / param.getPixelWidth());

    List<Scan> list = new ArrayList<>();

    if (ix >= 0 && ix2 < numbers.length && iy >= 0 && iy2 < numbers[ix2].length) {
      for (int i = ix; i <= ix2; i++) {
        for (int k = iy; k <= iy2; k++) {
          if (numbers[i][k][0] != -1)
            list.add(getScan(numbers[i][k][0]));
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
  public int[][][] getXYZScanNumbers() {
    if (xyzScanNumbers == null) {
      // sort all scan numbers to xyz location
      xyzScanNumbers = new int[param.getWidth()][param.getHeight()][param.getDepth()];
      for (int x = 0; x < xyzScanNumbers.length; x++) {
        for (int y = 0; y < xyzScanNumbers[x].length; y++) {
          for (int z = 0; z < xyzScanNumbers[x][y].length; z++) {
            xyzScanNumbers[x][y][z] = -1;
          }
        }
      }
      // add all scans
      int[] numbers = getScanNumbers();
      for (int i = 0; i < numbers.length; i++) {
        if (getScan(numbers[i]) instanceof ImagingScan) {
          Coordinates c = ((ImagingScan) getScan(numbers[i])).getCoordinates();
          if (c.getX() < param.getWidth() && c.getY() < param.getHeight()
              && c.getZ() < param.getDepth())
            xyzScanNumbers[c.getX()][c.getY()][c.getZ()] = numbers[i];
        }
      }
    }
    return xyzScanNumbers;
  }

}
