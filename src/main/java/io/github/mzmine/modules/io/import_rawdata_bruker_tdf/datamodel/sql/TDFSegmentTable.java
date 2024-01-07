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
