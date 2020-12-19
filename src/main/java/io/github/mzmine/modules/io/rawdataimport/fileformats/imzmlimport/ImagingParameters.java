package io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport;

import com.alanmrace.jimzmlparser.imzml.ImzML;
import com.alanmrace.jimzmlparser.mzml.CVParam;
import com.alanmrace.jimzmlparser.mzml.ScanSettings;
import com.alanmrace.jimzmlparser.mzml.ScanSettingsList;

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

  public static enum VerticalStart {
    TOP, BOTTOM;
  }

  public static enum HorizontalStart {
    LEFT, RIGHT;
  }
  public static enum Pattern {
    MEANDER, FLY_BACK, RANDOM
  }
  public static enum ScanDirection {
    HORIZONTAL, VERTICAL
  }

  private double minMZ, maxMZ;
  /**
   * lateral width and height in mumeter
   */
  private double lateralWidth, lateralHeight;
  private double pixelWidth = 1, pixelShape = 1;
  // max number of pixels in x and y and z (depth)
  private int width, height, depth = 1;
  // vertical and horizontal start
  private VerticalStart vStart;
  private HorizontalStart hStart;

  private int spectraPerPixel;

  private Pattern pattern;
  private ScanDirection scanDirection;


  public ImagingParameters(ImzML imz) {
    width = imz.getWidth();
    height = imz.getHeight();
    depth = imz.getDepth();
    spectraPerPixel = imz.getNumberOfSpectraPerPixel();
    minMZ = imz.getMinimumDetectedmz();
    maxMZ = imz.getMaximumDetectedmz();
    // Check scan settings first
    ScanSettingsList scanSettingsList = imz.getScanSettingsList();

    if (scanSettingsList != null) {
      for (ScanSettings scanSettings : scanSettingsList) {
        CVParam p = scanSettings.getCVParam(ScanSettings.MAX_DIMENSION_X_ID);
        if (p != null)
          lateralWidth = p.getValueAsDouble();
        p = scanSettings.getCVParam(ScanSettings.MAX_DIMENSION_Y_ID);
        if (p != null)
          lateralHeight = p.getValueAsDouble();

        p = scanSettings.getCVParam(ScanSettings.LINE_SCAN_DIRECTION_BOTTOM_UP_ID);
        if (p != null)
          vStart = VerticalStart.BOTTOM;
        else
          vStart = VerticalStart.TOP;

        p = scanSettings.getCVParam(ScanSettings.LINE_SCAN_DIRECTION_RIGHT_LEFT_ID);
        if (p != null)
          hStart = HorizontalStart.RIGHT;
        else
          hStart = HorizontalStart.LEFT;

        p = scanSettings.getCVParam(ScanSettings.PIXEL_AREA_ID);
        if (p != null)
          pixelWidth = p.getValueAsDouble();
        pixelShape = pixelWidth;

        p = scanSettings.getCVParam(ScanSettings.SCAN_PATTERN_MEANDERING_ID);
        if (p != null)
          pattern = Pattern.MEANDER;
        p = scanSettings.getCVParam(ScanSettings.SCAN_PATTERN_FLYBACK_ID);
        if (p != null)
          pattern = Pattern.FLY_BACK;
        p = scanSettings.getCVParam(ScanSettings.SCAN_PATTERN_RANDOM_ACCESS_ID);
        if (p != null)
          pattern = Pattern.RANDOM;

        p = scanSettings.getCVParam(ScanSettings.SCAN_TYPE_VERTICAL_ID);
        if (p != null)
          scanDirection = ScanDirection.VERTICAL;
        else
          scanDirection = ScanDirection.HORIZONTAL;
      }
    }
  }


  public void print() {
    System.out.println("pixel dim " + width + " " + height + " " + depth);
    System.out.println("lateral " + lateralWidth + " " + lateralHeight);
    System.out.println("pixel size " + pixelWidth + " " + pixelShape);
    System.out.println(hStart + " " + vStart);
    System.out.println(pattern + " " + scanDirection);
    System.out.println("spectraPerPixel " + spectraPerPixel);
    System.out.println("mz range" + minMZ + " " + maxMZ);

  }


  public double getMinMZ() {
    return minMZ;
  }


  public double getMaxMZ() {
    return maxMZ;
  }


  public double getLateralWidth() {
    return lateralWidth;
  }


  public double getLateralHeight() {
    return lateralHeight;
  }


  public double getPixelWidth() {
    return pixelWidth;
  }


  public double getPixelShape() {
    return pixelShape;
  }


  public int getWidth() {
    return width;
  }


  public int getHeight() {
    return height;
  }


  public int getDepth() {
    return depth;
  }


  public VerticalStart getvStart() {
    return vStart;
  }


  public HorizontalStart gethStart() {
    return hStart;
  }


  public int getSpectraPerPixel() {
    return spectraPerPixel;
  }


  public Pattern getPattern() {
    return pattern;
  }


  public void setMinMZ(double minMZ) {
    this.minMZ = minMZ;
  }


  public void setMaxMZ(double maxMZ) {
    this.maxMZ = maxMZ;
  }


  public void setLateralWidth(double lateralWidth) {
    this.lateralWidth = lateralWidth;
  }


  public void setLateralHeight(double lateralHeight) {
    this.lateralHeight = lateralHeight;
  }


  public void setPixelWidth(double pixelWidth) {
    this.pixelWidth = pixelWidth;
  }


  public void setPixelShape(double pixelShape) {
    this.pixelShape = pixelShape;
  }


  public void setWidth(int width) {
    this.width = width;
  }


  public void setHeight(int height) {
    this.height = height;
  }


  public void setDepth(int depth) {
    this.depth = depth;
  }


  public void setvStart(VerticalStart vStart) {
    this.vStart = vStart;
  }


  public void sethStart(HorizontalStart hStart) {
    this.hStart = hStart;
  }


  public void setSpectraPerPixel(int spectraPerPixel) {
    this.spectraPerPixel = spectraPerPixel;
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

}
