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

public class TDFMaldiFrameLaserInfoTable extends TDFDataTable<Long> {

  public static final String MALDI_FRAME_LASER_INFO_TABLE_NAME = "MaldiFrameLaserInfo";
  public static final String ID_COLUMN = "Id";

  public static final String LASER_APP_COLUMN = "LaserApplicationName";
  public static final String LASER_PARAM_COLUMN = "LaserParameterName";
  public static final String LASER_BOOST_COLUMN = "LaserBoost";
  public static final String LASER_FOCUS_COLUMN = "LaserFocus";
  public static final String LASER_BEAM_SCAN_COLUMN = "BeamScan";
  public static final String LASER_BEAM_SIZE_X_COLUMN = "BeamScanSizeX";
  public static final String LASER_BEAM_SIZE_Y_COLUMN = "BeamScanSizeY";
  public static final String WALK_ON_SPOT_MODE_COLUMN = "WalkOnSpotMode";
  public static final String WALK_ON_SPOT_SHOTS_COLUMN = "WalkOnSpotShots";
  public static final String SPOT_SIZE_COLUMN = "SpotSize";

  private final TDFDataColumn<Long> idColumn;
  private final TDFDataColumn<String> laserApplicationNameColumn;
  private final TDFDataColumn<String> laserParameterNameColumn;
  private final TDFDataColumn<Double> laserBoostColumn;
  private final TDFDataColumn<Double> laserFocusColumn;
  private final TDFDataColumn<Long> laserBeamScanColumn;
  private final TDFDataColumn<Double> laserBeamScanSizeXColumn;
  private final TDFDataColumn<Double> laserBeamScanSizeYColumn;
  private final TDFDataColumn<Long> walkOnSpotModeColumn;
  private final TDFDataColumn<Long> walkOnSpotShotsColumn;
  private final TDFDataColumn<Double> spotSizeColumn;

  public TDFMaldiFrameLaserInfoTable() {
    super(MALDI_FRAME_LASER_INFO_TABLE_NAME, ID_COLUMN);

    idColumn = (TDFDataColumn<Long>) getColumn(ID_COLUMN);

    laserApplicationNameColumn = new TDFDataColumn<>(LASER_APP_COLUMN);
    laserParameterNameColumn = new TDFDataColumn<>(LASER_PARAM_COLUMN);
    laserBoostColumn = new TDFDataColumn<>(LASER_BOOST_COLUMN);
    laserFocusColumn = new TDFDataColumn<>(LASER_FOCUS_COLUMN);
    laserBeamScanColumn = new TDFDataColumn<>(LASER_BEAM_SCAN_COLUMN);
    laserBeamScanSizeXColumn = new TDFDataColumn<>(LASER_BEAM_SIZE_X_COLUMN);
    laserBeamScanSizeYColumn = new TDFDataColumn<>(LASER_BEAM_SIZE_Y_COLUMN);
    walkOnSpotModeColumn = new TDFDataColumn<>(WALK_ON_SPOT_MODE_COLUMN);
    walkOnSpotShotsColumn = new TDFDataColumn<>(WALK_ON_SPOT_SHOTS_COLUMN);
    spotSizeColumn = new TDFDataColumn<>(SPOT_SIZE_COLUMN);

    columns.addAll(
        Arrays.asList(laserApplicationNameColumn, laserParameterNameColumn, laserBoostColumn,
            laserFocusColumn, laserBeamScanColumn, laserBeamScanSizeXColumn,
            laserBeamScanSizeYColumn,
            walkOnSpotModeColumn, walkOnSpotShotsColumn, spotSizeColumn));
  }

  public TDFDataColumn<Long> getIdColumn() {
    return idColumn;
  }

  public TDFDataColumn<String> getLaserApplicationNameColumn() {
    return laserApplicationNameColumn;
  }

  public TDFDataColumn<String> getLaserParameterNameColumn() {
    return laserParameterNameColumn;
  }

  public TDFDataColumn<Double> getLaserBoostColumn() {
    return laserBoostColumn;
  }

  public TDFDataColumn<Double> getLaserFocusColumn() {
    return laserFocusColumn;
  }

  public TDFDataColumn<Long> getLaserBeamScanColumn() {
    return laserBeamScanColumn;
  }

  public TDFDataColumn<Double> getLaserBeamScanSizeXColumn() {
    return laserBeamScanSizeXColumn;
  }

  public TDFDataColumn<Double> getLaserBeamScanSizeYColumn() {
    return laserBeamScanSizeYColumn;
  }

  public TDFDataColumn<Long> getWalkOnSpotModeColumn() {
    return walkOnSpotModeColumn;
  }

  public TDFDataColumn<Long> getWalkOnSpotShotsColumn() {
    return walkOnSpotShotsColumn;
  }

  public TDFDataColumn<Double> getSpotSizeColumn() {
    return spotSizeColumn;
  }
}
