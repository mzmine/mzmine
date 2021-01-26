/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.impl.ImsMsMsInfoImpl;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * As this is not a "real" table of the tdf format, it does not share the TDF prefix.
 * <p>
 * Maps precursor info (isolation m/z, precursor id and charge) to the respective scan numbers.
 * Returns {@link ImsMsMsInfo} for each frame.
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
  private final TDFDataColumn<Double> largestPeakMzColumn;
  private final TDFDataColumn<Long> chargeColumn;
  private final TDFDataColumn<Long> parentIdColumn;

  /**
   * Key = FrameId of the MS2 Frame
   * <p></p>
   * Value = Collection of ImsMsMsInfo on all precursors in the frame.
   */
  private final Map<Integer, Set<ImsMsMsInfo>> info;

  private final TDFFrameTable frameTable;

  public FramePrecursorTable(TDFFrameTable frameTable) {
    super(FRAME_PRECURSOR_TABLE, TDFPasefFrameMsMsInfoTable.FRAME_ID);

    this.frameTable = frameTable;

    // added by constructor
    frameIdColumn = (TDFDataColumn<Long>) getColumn(TDFPasefFrameMsMsInfoTable.FRAME_ID);

    // add manually
    precursorIdColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.PRECURSOR_ID);
    scanNumBeginColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.SCAN_NUM_BEGIN);
    scanNumEndColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.SCAN_NUM_END);
    collisionEnergyColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.COLLISION_ENERGY);
    largestPeakMzColumn = new TDFDataColumn<>(TDFPrecursorTable.LARGEST_PEAK_MZ);
    chargeColumn = new TDFDataColumn<>(TDFPrecursorTable.CHARGE);
    parentIdColumn = new TDFDataColumn<>(TDFPrecursorTable.PARENT_ID);

    columns.addAll(Arrays.asList(
        precursorIdColumn,
        scanNumBeginColumn,
        scanNumEndColumn,
        collisionEnergyColumn,
        largestPeakMzColumn,
        chargeColumn,
        parentIdColumn
    ));

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

    return msmstable + "." + TDFPasefFrameMsMsInfoTable.FRAME_ID + ", "
        + msmstable + "." + TDFPasefFrameMsMsInfoTable.PRECURSOR_ID + ", "
        + msmstable + "." + TDFPasefFrameMsMsInfoTable.SCAN_NUM_BEGIN + ", "
        + msmstable + "." + TDFPasefFrameMsMsInfoTable.SCAN_NUM_END + ", "
        + msmstable + "." + TDFPasefFrameMsMsInfoTable.COLLISION_ENERGY + ", "
        + precursorstable + "." + TDFPrecursorTable.LARGEST_PEAK_MZ + ", "
        + precursorstable + "." + TDFPrecursorTable.CHARGE + ", "
        + precursorstable + "." + TDFPrecursorTable.PARENT_ID;
  }

  @Override
  protected String getQueryText(String columnHeadersForQuery) {
    String msmstable = TDFPasefFrameMsMsInfoTable.PASEF_FRAME_MSMS_TABLE_NAME;
    String precursorstable = TDFPrecursorTable.PRECURSOR_TABLE_NAME;

    return "SELECT " + columnHeadersForQuery + " "
        + "FROM " + msmstable + " "
        + "LEFT JOIN " + precursorstable
        + " ON " + msmstable + "." + TDFPasefFrameMsMsInfoTable.PRECURSOR_ID
        + "=" + precursorstable + "." + TDFPrecursorTable.PRECURSOR_ID + " "
        //
        + "ORDER BY " + msmstable + "." + TDFPasefFrameMsMsInfoTable.FRAME_ID;
  }

  /**
   * Summarises
   */
  private void collapseInfo() {
    for (int i = 0; i < frameIdColumn.size(); i++) {
      final int frameId = frameIdColumn.get(i).intValue();

      Set<ImsMsMsInfo> entry = info.computeIfAbsent(frameId, k -> new HashSet<>());
      entry.add(new ImsMsMsInfoImpl(largestPeakMzColumn.get(i),
          Range.closedOpen(scanNumBeginColumn.get(i).intValue(),
              scanNumEndColumn.get(i).intValue()), collisionEnergyColumn.get(i).floatValue(),
          chargeColumn.get(i).intValue(), parentIdColumn.get(i).intValue(), frameId));
    }
  }

  @Nullable
  public Set<ImsMsMsInfo> getMsMsInfoForFrame(int frameNum) {
    return info.get(frameNum);
  }

}
