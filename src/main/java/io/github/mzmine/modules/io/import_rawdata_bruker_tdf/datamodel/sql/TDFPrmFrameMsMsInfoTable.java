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

/**
 * This table describes PASEF frames. For every PASEF frame, it represents a list of non-overlapping
 * scan-number ranges. For each of the scans inside a given range, the quadrupole has been
 * configured to be at the same specified isolation m/z and width.
 * <p>
 * NOTE: every frame for which there is an entry in this table, will have Frames.MsMsType = 8, and
 * no entry in any other *FrameMsMsInfo table.
 * <p>
 * NOTE: PASEF acquisition allows, in principle, to change the quadrupole isolation window on a
 * per-scan basis. Therefore, the compound primary key (Frame, ScanNumBegin) uniquely identifies
 * each fragmentation region.
 */
public class TDFPrmFrameMsMsInfoTable extends TDFDataTable<Long> {

  public static final String PRM_FRAME_MSMS_TABLE_NAME = "PrmFrameMsMsInfo";

  /**
   * The PRM frame to which this information applies.
   */
  public static final String FRAME_ID = "Frame";

  /**
   * Beginning of scan-number range where quadrupole was tuned to 'IsolationMz'. (R5)
   */
  public static final String SCAN_NUM_BEGIN = "ScanNumBegin";

  /**
   * End (exclusive) of scan-number range where quadrupole was tuned to 'IsolationMz'.
   */
  public static final String SCAN_NUM_END = "ScanNumEnd";

  /**
   * The isolation m/z (in the m/z calibration state that was used during acquisition). The
   * quadrupole has been tuned to this mass during fragmentation between 'ScanNumBegin' and
   * 'ScanNumEnd'.
   */
  public static final String ISOLATION_MZ = "IsolationMz";

  /**
   * Specifies the total 3-dB width of the isolation window (in m/z units), the center of which is
   * given by 'IsolationMz'.
   */
  public static final String ISOLATION_WIDTH = "IsolationWidth";

  /**
   * Collision energy (in eV) set between 'ScanNumBegin' and 'ScanNumEnd'.
   */
  public static final String COLLISION_ENERGY = "CollisionEnergy";

  public static final String TARGET = "Target";

  private final TDFDataColumn<Long> frameIdColumn;
  private final TDFDataColumn<Long> targetIdColumn;
  private final TDFDataColumn<Long> scanNumBeginColumn;
  private final TDFDataColumn<Long> scanNumEndColumn;
  private final TDFDataColumn<Double> collisionEnergyColumn;
  private final TDFDataColumn<Double> isolationWidthColumn;
  private final TDFDataColumn<Double> isolationMzColumn;

  public TDFPrmFrameMsMsInfoTable() {
    super(PRM_FRAME_MSMS_TABLE_NAME, FRAME_ID);

    // added by constructor
    frameIdColumn = (TDFDataColumn<Long>) getColumn(TDFPrmFrameMsMsInfoTable.FRAME_ID);

    // add manually
    targetIdColumn = new TDFDataColumn<>(TDFPrmFrameMsMsInfoTable.TARGET);
    scanNumBeginColumn = new TDFDataColumn<>(TDFPrmFrameMsMsInfoTable.SCAN_NUM_BEGIN);
    scanNumEndColumn = new TDFDataColumn<>(TDFPrmFrameMsMsInfoTable.SCAN_NUM_END);
    isolationMzColumn = new TDFDataColumn<>(ISOLATION_MZ);
    isolationWidthColumn = new TDFDataColumn<>(ISOLATION_WIDTH);
    collisionEnergyColumn = new TDFDataColumn<>(COLLISION_ENERGY);

    columns.addAll(
        Arrays.asList(scanNumBeginColumn, scanNumEndColumn, isolationMzColumn, isolationWidthColumn,
            collisionEnergyColumn, targetIdColumn));
  }
}
