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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql;

import java.util.List;

public class TDFSegmentTable extends TDFDataTable<Long> {

  public static final String SEGMENT_TABLE_NAME = "Segments";
  public static final String SEGMENT_ID_COLUMN = "Id";
  public static final String FIRST_FRAME_COLUMN = "FirstFrame";
  public static final String LAST_FRAME_COLUMN = "LastFrame";
  public static final String IS_CALI_SEGMENT_COLUMN = "IsCalibrationSegment";

  private final TDFDataColumn<Long> idColumn;
  private final TDFDataColumn<Long> firstFrameColumn = new TDFDataColumn<>(FIRST_FRAME_COLUMN);
  private final TDFDataColumn<Long> lastFrameColumn = new TDFDataColumn<>(LAST_FRAME_COLUMN);
  private final TDFDataColumn<Long> isCalibrationColumn = new TDFDataColumn<>(IS_CALI_SEGMENT_COLUMN);

  public TDFSegmentTable() {
    super(SEGMENT_TABLE_NAME, SEGMENT_ID_COLUMN);

    idColumn = (TDFDataColumn<Long>) getColumn(SEGMENT_ID_COLUMN);
    columns.addAll(List.of(firstFrameColumn, lastFrameColumn, isCalibrationColumn));
  }

  public int getNumberOfSegments() {
    return idColumn.size();
  }

  public TDFDataColumn<Long> getIdColumn() {
    return idColumn;
  }

  public TDFDataColumn<Long> getFirstFrameColumn() {
    return firstFrameColumn;
  }

  public TDFDataColumn<Long> getLastFrameColumn() {
    return lastFrameColumn;
  }

  public TDFDataColumn<Long> getIsCalibrationColumn() {
    return isCalibrationColumn;
  }
}
