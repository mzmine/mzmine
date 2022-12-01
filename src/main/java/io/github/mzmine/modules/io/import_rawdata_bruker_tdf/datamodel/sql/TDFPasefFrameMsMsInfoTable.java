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
