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
