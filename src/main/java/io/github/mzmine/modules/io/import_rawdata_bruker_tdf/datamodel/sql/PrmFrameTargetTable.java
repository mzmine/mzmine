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

import com.google.common.collect.Range;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * This table describes PRM PASEF frames. For every PRM PASEF frame, it represents a list of
 * non-overlapping scan-number ranges. For each of the scans inside a given range, the quadrupole
 * has been configured to be at the same specified isolation m/z and width.
 *
 * @author https://github.com/SteffenHeu
 */
public class PrmFrameTargetTable extends TDFDataTable<Long> {

  /**
   * This is not actually used in the query, we just have it for consistency purposes.
   */
  public static final String PRM_FRAME_PRECURSOR_TABLE = "PrmFramePrecursorTable";

  private final TDFDataColumn<Long> frameIdColumn;
  private final TDFDataColumn<Long> targetIdColumn;
  private final TDFDataColumn<Long> scanNumBeginColumn;
  private final TDFDataColumn<Long> scanNumEndColumn;
  private final TDFDataColumn<Double> collisionEnergyColumn;
  private final TDFDataColumn<Double> isolationWidthColumn;
  private final TDFDataColumn<Double> isolationMzColumn;
  private final TDFDataColumn<Long> chargeColumn;

  /**
   * Key = FrameId of the MS2 Frame
   * <p></p>
   * Value = Collection of PasefMsMsInfo on all precursors in the frame.
   */
  private final Map<Integer, Set<BuildingPASEFMsMsInfo>> info;

  public PrmFrameTargetTable() {
    super(PRM_FRAME_PRECURSOR_TABLE, TDFPasefFrameMsMsInfoTable.FRAME_ID);

    // added by constructor
    frameIdColumn = (TDFDataColumn<Long>) getColumn(TDFPrmFrameMsMsInfoTable.FRAME_ID);

    // add manually
    targetIdColumn = new TDFDataColumn<>(TDFPrmFrameMsMsInfoTable.TARGET);
    scanNumBeginColumn = new TDFDataColumn<>(TDFPrmFrameMsMsInfoTable.SCAN_NUM_BEGIN);
    scanNumEndColumn = new TDFDataColumn<>(TDFPrmFrameMsMsInfoTable.SCAN_NUM_END);
    isolationMzColumn = new TDFDataColumn<>(TDFPrmFrameMsMsInfoTable.ISOLATION_MZ);
    isolationWidthColumn = new TDFDataColumn<>(TDFPrmFrameMsMsInfoTable.ISOLATION_WIDTH);
    collisionEnergyColumn = new TDFDataColumn<>(TDFPrmFrameMsMsInfoTable.COLLISION_ENERGY);
    chargeColumn = new TDFDataColumn<>(TDFPrmTargetsTable.CHARGE);

    columns.addAll(
        Arrays.asList(targetIdColumn, scanNumBeginColumn, scanNumEndColumn, collisionEnergyColumn,
            isolationWidthColumn, isolationMzColumn, chargeColumn));

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
    final String msmstable = TDFPrmFrameMsMsInfoTable.PRM_FRAME_MSMS_TABLE_NAME;
    final String precursorstable = TDFPrmTargetsTable.TARGET_TABLE_NAME;

    return msmstable + "." + TDFPrmFrameMsMsInfoTable.FRAME_ID + ", " + msmstable + "."
        + TDFPrmFrameMsMsInfoTable.TARGET + ", " + msmstable + "."
        + TDFPrmFrameMsMsInfoTable.SCAN_NUM_BEGIN + ", " + msmstable + "."
        + TDFPrmFrameMsMsInfoTable.SCAN_NUM_END + ", " + msmstable + "."
        + TDFPrmFrameMsMsInfoTable.COLLISION_ENERGY + ", " + msmstable + "."
        + TDFPrmFrameMsMsInfoTable.ISOLATION_WIDTH + ", " + msmstable + "."
        + TDFPrmFrameMsMsInfoTable.ISOLATION_MZ + ", " + precursorstable + "."
        + TDFPrmTargetsTable.CHARGE;
  }

  @Override
  protected String getQueryText(String columnHeadersForQuery) {
    String msmstable = TDFPrmFrameMsMsInfoTable.PRM_FRAME_MSMS_TABLE_NAME;
    String precursorstable = TDFPrmTargetsTable.TARGET_TABLE_NAME;

    return "SELECT " + columnHeadersForQuery + " " + "FROM " + msmstable + " " + "LEFT JOIN "
        + precursorstable + " ON " + msmstable + "." + TDFPrmFrameMsMsInfoTable.TARGET + "="
        + precursorstable + "." + TDFPrmTargetsTable.TARGET_ID + " "
        //
        + "ORDER BY " + msmstable + "." + TDFPrmFrameMsMsInfoTable.FRAME_ID;
  }

  /**
   * Summarises
   */
  private void collapseInfo() {
    for (int i = 0; i < frameIdColumn.size(); i++) {
      final int frameId = frameIdColumn.get(i).intValue();

      Set<BuildingPASEFMsMsInfo> entry = info.computeIfAbsent(frameId, k -> new HashSet<>());
      final double precursorMz = isolationMzColumn.get(i);

      entry.add(new BuildingPASEFMsMsInfo(precursorMz,
          Range.closedOpen(scanNumBeginColumn.get(i).intValue() - 1,
              // bruker scan numbers start at 1, ours start at 0
              scanNumEndColumn.get(i).intValue() - 1), collisionEnergyColumn.get(i).floatValue(),
          chargeColumn.get(i).intValue(), null, frameId, isolationWidthColumn.get(i)));
    }
  }

  @Nullable
  public Set<BuildingPASEFMsMsInfo> getMsMsInfoForFrame(int frameNum) {
    return info.get(frameNum);
  }

}
