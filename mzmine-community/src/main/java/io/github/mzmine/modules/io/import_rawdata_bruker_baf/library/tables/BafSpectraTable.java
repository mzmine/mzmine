/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables;

import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFDataColumn;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFDataTable;
import java.util.Arrays;

public class BafSpectraTable extends TDFDataTable<Long> {

  public static final String NAME = "Spectra";

  public static final String ID_COL = "Id";
  public static final String RT_COL = "RT";
  public static final String SEGMENT_COL = "Segment";
  public static final String AQUISITION_KEY_COL = "AcquisitionKey";
  //  public static final String PARENT_COL = "Parent";
  public static final String MZ_ACQ_RANGE_LOWER_COL = "MzAcqRangeLower";
  public static final String MZ_ACQ_RANGE_UPPER_COL = "MzAcqRangeUpper";
  public static final String SUM_INTENSITY_COL = "SumIntensity";
  public static final String MAX_INTENSITY_COL = "MaxIntensity";
  public static final String PROFILE_MZ_ID_COL = "ProfileMzId";
  public static final String PROFILE_INTENSITY_ID_COL = "ProfileIntensityId";
  public static final String LINE_MZ_ID_COL = "LineMzId";
  public static final String LINE_INTENSITY_ID_COL = "LineIntensityId";
  public static final String LINE_AREA_ID_COL = "LinePeakAreaId";

  private final TDFDataColumn<Long> idCol;
  private final TDFDataColumn<Double> rtCol = new TDFDataColumn<>(RT_COL);
  private final TDFDataColumn<Long> segment = new TDFDataColumn<>(SEGMENT_COL);
  private final TDFDataColumn<Long> acquisitionKey = new TDFDataColumn<>(AQUISITION_KEY_COL);
  private final TDFDataColumn<Long> mzAcqRangeLowerCol = new TDFDataColumn<>(
      MZ_ACQ_RANGE_LOWER_COL);
  private final TDFDataColumn<Long> mzAcqRangeUpper = new TDFDataColumn<>(MZ_ACQ_RANGE_UPPER_COL);
  private final TDFDataColumn<Double> sumIntensityCol = new TDFDataColumn<>(SUM_INTENSITY_COL);
  private final TDFDataColumn<Double> maxIntensityCol = new TDFDataColumn<>(MAX_INTENSITY_COL);
  private final TDFDataColumn<Long> profileMzIdCol = new TDFDataColumn<>(PROFILE_MZ_ID_COL);
  private final TDFDataColumn<Long> profileIntensityCol = new TDFDataColumn<>(
      PROFILE_INTENSITY_ID_COL);
  private final TDFDataColumn<Long> lineMzIdCol = new TDFDataColumn<>(LINE_MZ_ID_COL);
  private final TDFDataColumn<Long> lineIntensityCol = new TDFDataColumn<>(LINE_INTENSITY_ID_COL);
  private final TDFDataColumn<Long> lineAreaIdCol = new TDFDataColumn<>(LINE_AREA_ID_COL);

  public BafSpectraTable() {
    super(NAME, ID_COL);

    idCol = (TDFDataColumn<Long>) getColumn(ID_COL);
    columns.addAll(
        Arrays.asList(rtCol, segment, acquisitionKey, mzAcqRangeLowerCol, mzAcqRangeUpper,
            sumIntensityCol, maxIntensityCol, profileMzIdCol, profileIntensityCol, lineMzIdCol,
            lineIntensityCol, lineAreaIdCol));
  }

  public boolean containsCentroidData() {
    return lineMzIdCol.stream().noneMatch(id -> id == null || id == 0) && lineIntensityCol.stream()
        .noneMatch(id -> id == null || id == 0);
  }

  public boolean containsProfileData() {
    return profileMzIdCol.stream().noneMatch(id -> id == null || id == 0)
        && profileIntensityCol.stream().noneMatch(id -> id == null || id == 0);
  }
}
