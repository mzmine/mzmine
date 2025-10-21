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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * As this is not a "real" table of the tdf format, it does not share the TDF prefix.
 * <p>
 * Maps precursor info (isolation m/z, precursor id and charge) to the respective scan numbers.
 * Returns {@link PasefMsMsInfo} for each frame.
 *
 * @author https://github.com/SteffenHeu
 */
public class FramePrecursorTable extends TDFDataTable<Long> {

  /**
   * This is not actually used in the query, we just have it for consistency purposes.
   */
  public static final String FRAME_PRECURSOR_TABLE = "FramePrecursorTable";

  private final TDFDataColumn<Long> frameIdColumn;
  private final TDFDataColumn<Long> precursorIdColumn;
  private final TDFDataColumn<Long> scanNumBeginColumn;
  private final TDFDataColumn<Long> scanNumEndColumn;
  private final TDFDataColumn<Double> collisionEnergyColumn;
  private final TDFDataColumn<Double> isolationWidthColumn;
  private final TDFDataColumn<Double> largestPeakMzColumn;
  private final TDFDataColumn<Double> isolationMzColumn;
  private final TDFDataColumn<Long> chargeColumn;
  private final TDFDataColumn<Long> parentIdColumn;

  /**
   * Key = FrameId of the MS2 Frame
   * <p></p>
   * Value = Collection of PasefMsMsInfo on all precursors in the frame.
   */
  private final Map<Integer, Set<BuildingPASEFMsMsInfo>> info;


  public FramePrecursorTable() {
    super(FRAME_PRECURSOR_TABLE, TDFPasefFrameMsMsInfoTable.FRAME_ID);

    // added by constructor
    frameIdColumn = (TDFDataColumn<Long>) getColumn(TDFPasefFrameMsMsInfoTable.FRAME_ID);

    // add manually
    precursorIdColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.PRECURSOR_ID);
    scanNumBeginColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.SCAN_NUM_BEGIN);
    scanNumEndColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.SCAN_NUM_END);
    collisionEnergyColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.COLLISION_ENERGY);
    isolationWidthColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.ISOLATION_WIDTH);
    isolationMzColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.ISOLATION_MZ);
    largestPeakMzColumn = new TDFDataColumn<>(TDFPrecursorTable.LARGEST_PEAK_MZ);
    chargeColumn = new TDFDataColumn<>(TDFPrecursorTable.CHARGE);
    parentIdColumn = new TDFDataColumn<>(TDFPrecursorTable.PARENT_ID);

    columns.addAll(Arrays.asList(precursorIdColumn, scanNumBeginColumn, scanNumEndColumn,
        collisionEnergyColumn, isolationWidthColumn, isolationMzColumn, largestPeakMzColumn,
        chargeColumn, parentIdColumn));

    info = new HashMap<>();
  }

  @Override
  public boolean executeQuery(Connection connection) {
    boolean query = super.executeQuery(connection);
    if (query) {
      collapseInfo();
    }
    return query;
  }

  @Override
  protected String getColumnHeadersForQuery() {
    final String msmstable = TDFPasefFrameMsMsInfoTable.PASEF_FRAME_MSMS_TABLE_NAME;
    final String precursorstable = TDFPrecursorTable.PRECURSOR_TABLE_NAME;

    return msmstable + "." + TDFPasefFrameMsMsInfoTable.FRAME_ID + ", " + msmstable + "."
        + TDFPasefFrameMsMsInfoTable.PRECURSOR_ID + ", " + msmstable + "."
        + TDFPasefFrameMsMsInfoTable.SCAN_NUM_BEGIN + ", " + msmstable + "."
        + TDFPasefFrameMsMsInfoTable.SCAN_NUM_END + ", " + msmstable + "."
        + TDFPasefFrameMsMsInfoTable.COLLISION_ENERGY + ", " + msmstable + "."
        + TDFPasefFrameMsMsInfoTable.ISOLATION_WIDTH + ", " + msmstable + "."
        + TDFPasefFrameMsMsInfoTable.ISOLATION_MZ + ", " + precursorstable + "."
        + TDFPrecursorTable.LARGEST_PEAK_MZ + ", " + precursorstable + "."
        + TDFPrecursorTable.CHARGE + ", " + precursorstable + "." + TDFPrecursorTable.PARENT_ID;
  }

  @Override
  protected String getQueryText(String columnHeadersForQuery) {
    String msmstable = TDFPasefFrameMsMsInfoTable.PASEF_FRAME_MSMS_TABLE_NAME;
    String precursorstable = TDFPrecursorTable.PRECURSOR_TABLE_NAME;

    return "SELECT " + columnHeadersForQuery + " " + "FROM " + msmstable + " " + "LEFT JOIN "
        + precursorstable + " ON " + msmstable + "." + TDFPasefFrameMsMsInfoTable.PRECURSOR_ID + "="
        + precursorstable + "." + TDFPrecursorTable.PRECURSOR_ID + " "
        //
        + "ORDER BY " + msmstable + "." + TDFPasefFrameMsMsInfoTable.FRAME_ID;
  }

  /**
   * Summarises
   */
  private void collapseInfo() {
    for (int i = 0; i < frameIdColumn.size(); i++) {
      final int frameId = frameIdColumn.get(i).intValue();

      Set<BuildingPASEFMsMsInfo> entry = info.computeIfAbsent(frameId, k -> new HashSet<>());

      /**
       * for some special cases the largest peak m/z might be 0 (usually corresponds with
       * monoisotopic although monoisotopic is just an estimate). In that case, use the isolation
       * m/z because it is always set.
       */
      final double precursorMz =
          Double.compare(largestPeakMzColumn.get(i), 0d) != 0 ? largestPeakMzColumn.get(i)
              : isolationMzColumn.get(i);

      entry.add(new BuildingPASEFMsMsInfo(precursorMz,
          // Spectrum range as listed in the bruker table. (scan numbers start at 1 and are end-exclusive)
          Range.closedOpen(scanNumBeginColumn.get(i).intValue(),
              scanNumEndColumn.get(i).intValue()), collisionEnergyColumn.get(i).floatValue(),
          chargeColumn.get(i).intValue(), parentIdColumn.get(i).intValue(), frameId,
          isolationWidthColumn.get(i)));
    }
  }

  @Nullable
  public Set<BuildingPASEFMsMsInfo> getMsMsInfoForFrame(int frameNum) {
    return info.get(frameNum);
  }

}
