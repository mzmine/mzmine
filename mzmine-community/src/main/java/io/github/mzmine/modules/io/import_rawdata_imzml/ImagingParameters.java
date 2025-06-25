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

package io.github.mzmine.modules.io.import_rawdata_imzml;

import com.alanmrace.jimzmlparser.imzml.ImzML;
import com.alanmrace.jimzmlparser.mzml.CVParam;
import com.alanmrace.jimzmlparser.mzml.ScanSettings;
import com.alanmrace.jimzmlparser.mzml.ScanSettingsList;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameLaserInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMetaDataTable.Keys;
import java.util.logging.Logger;

/*
 * <scanSettingsList count="1"> <scanSettings id="scansettings1"> <cvParam cvRef="IMS"
 * accession="IMS:1000401" name="top down" value=""/> <cvParam cvRef="IMS" accession="IMS:1000413"
 * name="flyback" value=""/> <cvParam cvRef="IMS" accession="IMS:1000480"
 * name="horizontal line scan" value=""/> <cvParam cvRef="IMS" accession="IMS:1000491"
 * name="linescan left right" value=""/> <cvParam cvRef="IMS" accession="IMS:1000042"
 * name="max count of pixel x" value="3"/> <cvParam cvRef="IMS" accession="IMS:1000043"
 * name="max count of pixel y" value="3"/> <cvParam cvRef="IMS" accession="IMS:1000044"
 * name="max dimension x" value="300" unitCvRef="UO" unitAccession="UO:0000017"
 * unitName="micrometer"/> <cvParam cvRef="IMS" accession="IMS:1000045" name="max dimension y"
 * value="300" unitCvRef="UO" unitAccession="UO:0000017" unitName="micrometer"/> <cvParam
 * cvRef="IMS" accession="IMS:1000046" name="pixel size x" value="100" unitCvRef="UO"
 * unitAccession="UO:0000017" unitName="micrometer"/> <cvParam cvRef="IMS" accession="IMS:1000047"
 * name="pixel size y" value="100" unitCvRef="UO" unitAccession="UO:0000017" unitName="micrometer"/>
 */

public class ImagingParameters {

  private static final Logger logger = Logger.getLogger(ImagingParameters.class.getName());

  private double minMZ, maxMZ;
  /**
   * lateral width and height in mumeter
   */
  private double lateralWidth;
  private double lateralHeight;
  private double pixelWidth = 1;
  private double pixelHeight = 1;
  // max number of pixels in x and y and z (depth)
  private int maxNumberOfPixelX;
  private int maxNumberOfPixelY;
  private int maxNumberOfPixelZ;
  // vertical and horizontal start
  private VerticalStart vStart;
  private HorizontalStart hStart;
  private int spectraPerPixel;
  private Pattern pattern;
  private ScanDirection scanDirection;

  public ImagingParameters(TDFMetaDataTable metaDataTable,
      TDFMaldiFrameInfoTable maldiFrameInfoTable, TDFMaldiFrameLaserInfoTable laserInfoTable) {
    try {
      maxNumberOfPixelX =
          Integer.parseInt(metaDataTable.getValueForKey(Keys.ImagingAreaMaxXIndexPos)) - Integer
              .parseInt(metaDataTable.getValueForKey(Keys.ImagingAreaMinXIndexPos)) + 1;
      maxNumberOfPixelY =
          Integer.parseInt(metaDataTable.getValueForKey(Keys.ImagingAreaMaxYIndexPos)) - Integer
              .parseInt(metaDataTable.getValueForKey(Keys.ImagingAreaMinYIndexPos)) + 1;
    } catch (NumberFormatException e) {
      logger.info(() -> "Number format exception during tdf maldi import.");
      maxNumberOfPixelX = (int) (maldiFrameInfoTable.getxIndexPosColumn().stream().max(Long::compare).get()
                - maldiFrameInfoTable.getxIndexPosColumn().stream().min(Long::compare).get());
      maxNumberOfPixelY = (int) (maldiFrameInfoTable.getyIndexPosColumn().stream().max(Long::compare).get()
          - maldiFrameInfoTable.getyIndexPosColumn().stream().min(Long::compare).get());
    }
    maxNumberOfPixelZ = 1;
    spectraPerPixel = 1;

    vStart = VerticalStart.TOP;
    hStart = HorizontalStart.LEFT;
    pattern = Pattern.FLY_BACK;
    scanDirection = ScanDirection.HORIZONTAL;

    lateralWidth = Math
        .abs(maldiFrameInfoTable.getMotorPositionXColumn().stream().min(Double::compare).get()
            - maldiFrameInfoTable.getMotorPositionXColumn().stream().max(Double::compare).get());

    lateralHeight = Math
        .abs(maldiFrameInfoTable.getMotorPositionYColumn().stream().min(Double::compare).get()
            - maldiFrameInfoTable.getMotorPositionYColumn().stream().max(Double::compare).get());

    /*pixelWidth = getLateralWidth() / getMaxNumberOfPixelX();
    pixelHeight = getLateralHeight() / getMaxNumberOfPixelY();*/
    pixelWidth = laserInfoTable.getSpotSizeColumn().get(0);
    pixelHeight = laserInfoTable.getSpotSizeColumn().get(0);
  }

  public ImagingParameters(ImzML imz) {
    maxNumberOfPixelX = imz.getWidth();
    maxNumberOfPixelY = imz.getHeight();
    maxNumberOfPixelZ = imz.getDepth();
    spectraPerPixel = imz.getNumberOfSpectraPerPixel();
    minMZ = imz.getMinimumDetectedmz();
    maxMZ = imz.getMaximumDetectedmz();
    // Check scan settings first
    ScanSettingsList scanSettingsList = imz.getScanSettingsList();

    if (scanSettingsList != null) {
      for (ScanSettings scanSettings : scanSettingsList) {
        CVParam p = scanSettings.getCVParam(ScanSettings.MAX_DIMENSION_X_ID);
        if (p != null) {
          lateralWidth = p.getValueAsDouble();
        }
        p = scanSettings.getCVParam(ScanSettings.MAX_DIMENSION_Y_ID);
        if (p != null) {
          lateralHeight = p.getValueAsDouble();
        }

        p = scanSettings.getCVParam(ScanSettings.LINE_SCAN_DIRECTION_BOTTOM_UP_ID);
        if (p != null) {
          vStart = VerticalStart.BOTTOM;
        } else {
          vStart = VerticalStart.TOP;
        }

        p = scanSettings.getCVParam(ScanSettings.LINE_SCAN_DIRECTION_RIGHT_LEFT_ID);
        if (p != null) {
          hStart = HorizontalStart.RIGHT;
        } else {
          hStart = HorizontalStart.LEFT;
        }

        p = scanSettings.getCVParam(ScanSettings.PIXEL_AREA_ID);
        if (p != null) {
          pixelWidth = p.getValueAsDouble();
        }
        pixelHeight = pixelWidth;

        p = scanSettings.getCVParam(ScanSettings.SCAN_PATTERN_MEANDERING_ID);
        if (p != null) {
          pattern = Pattern.MEANDER;
        }
        p = scanSettings.getCVParam(ScanSettings.SCAN_PATTERN_FLYBACK_ID);
        if (p != null) {
          pattern = Pattern.FLY_BACK;
        }
        p = scanSettings.getCVParam(ScanSettings.SCAN_PATTERN_RANDOM_ACCESS_ID);
        if (p != null) {
          pattern = Pattern.RANDOM;
        }

        p = scanSettings.getCVParam(ScanSettings.SCAN_TYPE_VERTICAL_ID);
        if (p != null) {
          scanDirection = ScanDirection.VERTICAL;
        } else {
          scanDirection = ScanDirection.HORIZONTAL;
        }
      }

      if(Double.compare(lateralHeight, 0d) == 0) {
        lateralHeight = maxNumberOfPixelY * pixelHeight;
      }
      if(Double.compare(lateralWidth, 0d) == 0) {
        lateralWidth = maxNumberOfPixelX * pixelWidth;
      }
    }
    if(pattern == null) {
      pattern = Pattern.UNKNOWN;
    }
  }

  public double getMinMZ() {
    return minMZ;
  }

  public void setMinMZ(double minMZ) {
    this.minMZ = minMZ;
  }

  public double getMaxMZ() {
    return maxMZ;
  }

  public void setMaxMZ(double maxMZ) {
    this.maxMZ = maxMZ;
  }

  public double getLateralWidth() {
    return lateralWidth;
  }

  public void setLateralWidth(double lateralWidth) {
    this.lateralWidth = lateralWidth;
  }

  public double getLateralHeight() {
    return lateralHeight;
  }

  public void setLateralHeight(double lateralHeight) {
    this.lateralHeight = lateralHeight;
  }

  public double getPixelWidth() {
    return pixelWidth;
  }

  public void setPixelWidth(double pixelWidth) {
    this.pixelWidth = pixelWidth;
  }

  public double getPixelHeight() {
    return pixelHeight;
  }

  public void setPixelHeight(double pixelHeight) {
    this.pixelHeight = pixelHeight;
  }

  public int getMaxNumberOfPixelX() {
    return maxNumberOfPixelX;
  }

  public void setMaxNumberOfPixelX(int maxNumberOfPixelX) {
    this.maxNumberOfPixelX = maxNumberOfPixelX;
  }

  public int getMaxNumberOfPixelY() {
    return maxNumberOfPixelY;
  }

  public void setMaxNumberOfPixelY(int maxNumberOfPixelY) {
    this.maxNumberOfPixelY = maxNumberOfPixelY;
  }

  public int getMaxNumberOfPixelZ() {
    return maxNumberOfPixelZ;
  }

  public void setMaxNumberOfPixelZ(int maxNumberOfPixelZ) {
    this.maxNumberOfPixelZ = maxNumberOfPixelZ;
  }

  public VerticalStart getvStart() {
    return vStart;
  }

  public void setvStart(VerticalStart vStart) {
    this.vStart = vStart;
  }

  public HorizontalStart gethStart() {
    return hStart;
  }

  public void sethStart(HorizontalStart hStart) {
    this.hStart = hStart;
  }

  public int getSpectraPerPixel() {
    return spectraPerPixel;
  }

  public void setSpectraPerPixel(int spectraPerPixel) {
    this.spectraPerPixel = spectraPerPixel;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public void setPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  public ScanDirection getScanDirection() {
    return scanDirection;
  }

  public void setScanDirection(ScanDirection scanDirection) {
    this.scanDirection = scanDirection;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((hStart == null) ? 0 : hStart.hashCode());
    long temp;
    temp = Double.doubleToLongBits(lateralHeight);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lateralWidth);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(maxMZ);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + maxNumberOfPixelX;
    result = prime * result + maxNumberOfPixelY;
    result = prime * result + maxNumberOfPixelZ;
    temp = Double.doubleToLongBits(minMZ);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
    temp = Double.doubleToLongBits(pixelHeight);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(pixelWidth);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((scanDirection == null) ? 0 : scanDirection.hashCode());
    result = prime * result + spectraPerPixel;
    result = prime * result + ((vStart == null) ? 0 : vStart.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ImagingParameters other = (ImagingParameters) obj;
    if (hStart != other.hStart) {
      return false;
    }
    if (Double.doubleToLongBits(lateralHeight) != Double.doubleToLongBits(other.lateralHeight)) {
      return false;
    }
    if (Double.doubleToLongBits(lateralWidth) != Double.doubleToLongBits(other.lateralWidth)) {
      return false;
    }
    if (Double.doubleToLongBits(maxMZ) != Double.doubleToLongBits(other.maxMZ)) {
      return false;
    }
    if (maxNumberOfPixelX != other.maxNumberOfPixelX) {
      return false;
    }
    if (maxNumberOfPixelY != other.maxNumberOfPixelY) {
      return false;
    }
    if (maxNumberOfPixelZ != other.maxNumberOfPixelZ) {
      return false;
    }
    if (Double.doubleToLongBits(minMZ) != Double.doubleToLongBits(other.minMZ)) {
      return false;
    }
    if (pattern != other.pattern) {
      return false;
    }
    if (Double.doubleToLongBits(pixelHeight) != Double.doubleToLongBits(other.pixelHeight)) {
      return false;
    }
    if (Double.doubleToLongBits(pixelWidth) != Double.doubleToLongBits(other.pixelWidth)) {
      return false;
    }
    if (scanDirection != other.scanDirection) {
      return false;
    }
    if (spectraPerPixel != other.spectraPerPixel) {
      return false;
    }
    if (vStart != other.vStart) {
      return false;
    }
    return true;
  }


  public enum VerticalStart {
    TOP("Top"), BOTTOM("Bottom");
    final String name;

    VerticalStart(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }


  public enum HorizontalStart {
    LEFT("Left"), RIGHT("Right");
    final String name;

    HorizontalStart(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }


  public enum Pattern {
    MEANDER("Meander"), FLY_BACK("Fly Back"), RANDOM("Random"), UNKNOWN("Unknown");

    final String name;

    Pattern(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }


  public enum ScanDirection {


    HORIZONTAL("Horizontal"), VERTICAL("Vertical");
    final String name;

    ScanDirection(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

}
