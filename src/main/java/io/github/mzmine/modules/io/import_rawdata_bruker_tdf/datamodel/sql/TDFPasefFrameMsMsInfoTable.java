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
public class TDFPasefFrameMsMsInfoTable extends TDFDataTable<Long> {

  public static final String PASEF_FRAME_MSMS_TABLE_NAME = "PasefFrameMsMsInfo";

  /**
   * The PASEF frame to which this information applies.
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

  /**
   * Optionally, the ID of a precursor that was measured in this frame and scan-number range. May be
   * NULL in case this measurement was not based on a precursor search, but manually programmed
   * instead.
   */
  public static final String PRECURSOR_ID = "Precursor";

  private final TDFDataColumn<Long> frameIdColumn;
  private final TDFDataColumn<Long> precursorIdColumn;
  private final TDFDataColumn<Long> scanNumBeginColumn;
  private final TDFDataColumn<Long> scanNumEndColumn;

  public TDFPasefFrameMsMsInfoTable() {
    super(PASEF_FRAME_MSMS_TABLE_NAME, FRAME_ID);

    // added by constructor
    frameIdColumn = (TDFDataColumn<Long>) getColumn(TDFPasefFrameMsMsInfoTable.FRAME_ID);

    // add manually
    precursorIdColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.PRECURSOR_ID);
    scanNumBeginColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.SCAN_NUM_BEGIN);
    scanNumEndColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.SCAN_NUM_END);

    columns.addAll(Arrays.asList(
        scanNumBeginColumn,
        scanNumEndColumn,
        new TDFDataColumn<Double>(ISOLATION_MZ),
        new TDFDataColumn<Double>(ISOLATION_WIDTH),
        new TDFDataColumn<Double>(COLLISION_ENERGY),
        precursorIdColumn
    ));
  }

  /**
   *
   * @param frame
   * @param brukerScanNum Bruker layout!
   * @return the precursor id or -1;
   */
  public int getPrecursorIdAtScan(long frame, long brukerScanNum) {
    int index = 0;
    for (; index < getColumn(FRAME_ID).size(); index++) {
      if (frameIdColumn.get(index) == frame
          && scanNumBeginColumn.get(index) <= brukerScanNum
          && scanNumEndColumn.get(index) > brukerScanNum) {
        return precursorIdColumn.get(index).intValue();
      } else if (frameIdColumn.get(index) > frame) {
        break;
      }
    }
    return -1;
  }




}
