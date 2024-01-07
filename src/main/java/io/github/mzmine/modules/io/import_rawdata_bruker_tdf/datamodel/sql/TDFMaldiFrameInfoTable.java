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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql;

import java.util.Arrays;
import org.jetbrains.annotations.Nullable;

public class TDFMaldiFrameInfoTable extends TDFDataTable<Long> {

  public static final String MALDI_FRAME_INFO_TABLE_NAME = "MaldiFrameInfo";

  public static final String FRAME_ID = "Frame";
  public static final String CHIP = "Chip";
  public static final String SPOT_NAME = "SpotName";
  public static final String REGION_NUMBER = "RegionNumber";
  public static final String X_INDEX_POS = "XIndexPos";
  public static final String Y_INDEX_POS = "YIndexPos";
  public static final String LASER_POWER = "LaserPower";
  public static final String NUM_LASER_SHOTS = "NumLaserShots";
  public static final String LASER_REP_RATE = "LaserRepRate";
  public static final String MOTOR_POSITON_X = "MotorPositionX";
  public static final String MOTOR_POSITON_Y = "MotorPositionY";
  public static final String MOTOR_POSITON_Z = "MotorPositionZ";
  public static final String LASER_INFO = "LaserInfo";

  private final TDFDataColumn<Long> frameIdColumn;
  private final TDFDataColumn<Long> chipColumn;
  private final TDFDataColumn<String> spotNameColumn;
  private final TDFDataColumn<Long> regionNumberColumn;
  private final TDFDataColumn<Long> xIndexPosColumn;
  private final TDFDataColumn<Long> yIndexPosColumn;
  private final TDFDataColumn<Double> laserPowerColumn;
  private final TDFDataColumn<Long> numLaserShotsColumn;
  private final TDFDataColumn<Double> laserRepRateColumn;
  private final TDFDataColumn<Double> motorPositionXColumn;
  private final TDFDataColumn<Double> motorPositionYColumn;
  private final TDFDataColumn<Double> motorPositionZColumn;
  private final TDFDataColumn<Long> laserInfoColumn;

  private int minXIndex;
  private int minYIndex;


  public TDFMaldiFrameInfoTable() {
    super(MALDI_FRAME_INFO_TABLE_NAME, FRAME_ID);

    frameIdColumn = (TDFDataColumn<Long>) getColumn(FRAME_ID);
    chipColumn = new TDFDataColumn<>(CHIP);
    spotNameColumn = new TDFDataColumn<>(SPOT_NAME);
    regionNumberColumn = new TDFDataColumn<>(REGION_NUMBER);
    xIndexPosColumn = new TDFDataColumn<>(X_INDEX_POS);
    yIndexPosColumn = new TDFDataColumn<>(Y_INDEX_POS);
    laserPowerColumn = new TDFDataColumn<>(LASER_POWER);
    numLaserShotsColumn = new TDFDataColumn<>(NUM_LASER_SHOTS);
    laserRepRateColumn = new TDFDataColumn<>(LASER_REP_RATE);
    motorPositionXColumn = new TDFDataColumn<>(MOTOR_POSITON_X);
    motorPositionYColumn = new TDFDataColumn<>(MOTOR_POSITON_Y);
    motorPositionZColumn = new TDFDataColumn<>(MOTOR_POSITON_Z);
    laserInfoColumn = new TDFDataColumn<>(LASER_INFO);

    columns.addAll(Arrays.asList(chipColumn, spotNameColumn, regionNumberColumn,
        xIndexPosColumn, yIndexPosColumn, laserPowerColumn, numLaserShotsColumn, laserRepRateColumn,
        motorPositionXColumn, motorPositionYColumn, motorPositionZColumn, laserInfoColumn));
  }

  public TDFDataColumn<Long> getFrameIdColumn() {
    return frameIdColumn;
  }

  public TDFDataColumn<Long> getChipColumn() {
    return chipColumn;
  }

  public TDFDataColumn<String> getSpotNameColumn() {
    return spotNameColumn;
  }

  public TDFDataColumn<Long> getRegionNumberColumn() {
    return regionNumberColumn;
  }

  public TDFDataColumn<Long> getxIndexPosColumn() {
    return xIndexPosColumn;
  }

  public TDFDataColumn<Long> getyIndexPosColumn() {
    return yIndexPosColumn;
  }

  public TDFDataColumn<Double> getLaserPowerColumn() {
    return laserPowerColumn;
  }

  public TDFDataColumn<Long> getNumLaserShotsColumn() {
    return numLaserShotsColumn;
  }

  public TDFDataColumn<Double> getLaserRepRateColumn() {
    return laserRepRateColumn;
  }

  public TDFDataColumn<Double> getMotorPositionXColumn() {
    return motorPositionXColumn;
  }

  public TDFDataColumn<Double> getMotorPositionYColumn() {
    return motorPositionYColumn;
  }

  public TDFDataColumn<Double> getMotorPositionZColumn() {
    return motorPositionZColumn;
  }

  public TDFDataColumn<Long> getLaserInfoColumn() {
    return laserInfoColumn;
  }


  public void process() {
    minXIndex = getxIndexPosColumn().stream().min(Long::compare).get().intValue();
    minYIndex = getyIndexPosColumn().stream().min(Long::compare).get().intValue();
  }

  public int getTransformedXIndexPos(int index) {
    return getxIndexPosColumn().get(index).intValue() - minXIndex;
  }

  public int getTransformedYIndexPos(int index) {
    return getyIndexPosColumn().get(index).intValue() - minYIndex;
  }

  @Nullable
  public MaldiSpotInfo getMaldiSpotInfo(int frameNum) {
    int index = frameIdColumn.indexOf((long) frameNum);

    if (index == -1) {
      return null;
    }

    return new MaldiSpotInfo(frameNum, chipColumn.get(index).intValue(), spotNameColumn.get(index),
        regionNumberColumn.get(index).intValue(), xIndexPosColumn.get(index).intValue(),
        yIndexPosColumn.get(index).intValue(), motorPositionXColumn.get(index),
        motorPositionYColumn.get(index), motorPositionZColumn.get(index));
  }
}
