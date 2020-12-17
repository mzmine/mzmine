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

package io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.sql;

import com.google.common.collect.Range;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * As this is not a "real" table of the tdf format, it does not share the TDF prefix.
 * <p>
 * Maps precursor info (isolation m/z, precursor id and charge) to the respective scan numbers.
 * Returns {@link FramePrecursorInfo} for each* scan in a frame.
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
  private final TDFDataColumn<Double> largestPeakMzColumn;
  private final TDFDataColumn<Long> chargeColumn;

  /**
   * Key = FrameId
   */
  private final Map<Long, Collection<FramePrecursorInfo>> info;

  /**
   * Key = PrecursorId
   * <p></p>
   * Value = Set of fragment scan numbers.
   */
  private final Map<Long, Set<Integer>> fragmentScanNumbers;

  private final Map<Long, Range<Long>> precursorScanNumRanges;

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
    largestPeakMzColumn = new TDFDataColumn<>(TDFPrecursorTable.LARGEST_PEAK_MZ);
    chargeColumn = new TDFDataColumn<>(TDFPrecursorTable.CHARGE);

    columns.addAll(Arrays.asList(
        precursorIdColumn,
        scanNumBeginColumn,
        scanNumEndColumn,
        largestPeakMzColumn,
        chargeColumn
    ));

    info = new HashMap<>();
    fragmentScanNumbers = new HashMap<>();
    precursorScanNumRanges = new HashMap<>();
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
        + precursorstable + "." + TDFPrecursorTable.LARGEST_PEAK_MZ + ", "
        + precursorstable + "." + TDFPrecursorTable.CHARGE;
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
    for (int i = 0; i < keyList.size(); i++) {
      final long frameId = keyList.get(i);
      final long precoursorId = precursorIdColumn.get(i);

      Collection<FramePrecursorInfo> entry = info.computeIfAbsent(frameId, k -> new HashSet<>());
      entry.add(
          new FramePrecursorInfo(precoursorId, scanNumBeginColumn.get(i).intValue(),
              scanNumEndColumn.get(i).intValue(), largestPeakMzColumn.get(i),
              chargeColumn.get(i).intValue()));

      final Set<Integer> fragmentScanNums =
          fragmentScanNumbers.computeIfAbsent(precoursorId, k -> new HashSet<>());
      final long baseScanNum = frameTable.getFirstScanNumForFrame(frameIdColumn.get(i).intValue());
      for (int scanNum = scanNumBeginColumn.get(i).intValue();
          scanNum < scanNumEndColumn.get(i).intValue(); scanNum++) {
        fragmentScanNums.add((int) (baseScanNum + scanNum));
      }

      final int finali = i;
      precursorScanNumRanges.computeIfAbsent(precoursorId,
          key -> Range.closed(scanNumBeginColumn.get(finali), scanNumEndColumn.get(finali)));
    }
  }

  public Range<Long> getBrukerScanNumberRangeForPrecursor(long precursorId) {
    return precursorScanNumRanges.get(precursorId);
  }

  /**
   * @param precursorId
   * @return Array of fragment scan numbers (unsorted) or empty set if no fragment scans for that
   * prefursor exist
   */
  @Nonnull
  public Set<Integer> getFragmentScansForPrecursor(long precursorId) {
    return fragmentScanNumbers.getOrDefault(precursorId, Collections.emptySet());
  }

  /**
   * Returns the precursor info at the requested MS2 frame as read from the tdf file. Does not match
   * with MZmine layout!
   *
   * @param frameId The frame id
   * @param scanNum The <b>Bruker</b> scan number - Does <b>not</b> match MZmine layout!
   * @return The {@link FramePrecursorInfo} or null if not present.
   */
  @Nullable
  public FramePrecursorInfo getPrecursorInfoForMS2ScanNumber(long frameId, int scanNum) {
    Collection<FramePrecursorInfo> set = info.get(frameId);
    if (set == null) {
      return null;
    }

    for (FramePrecursorInfo fpi : set) {
      if (fpi.containsScanNum(scanNum)) {
        return fpi;
      }
    }
    return null;
  }

  /**
   * Summarises information on precursors in a specific frame.
   */
  public final static class FramePrecursorInfo {

    private final long precursorId;
    private final double largestPeakMz;
    private final int charge;

    private final Range<Integer> scanRange;

    /**
     * @param precursorId
     * @param brukerScanNumBegin bruker layout
     * @param brukerScanNumEnd   bruker layout
     * @param largestPeakMz
     * @param charge
     */
    public FramePrecursorInfo(long precursorId, int brukerScanNumBegin, int brukerScanNumEnd,
        double largestPeakMz, int charge) {
      this.precursorId = precursorId;
      this.largestPeakMz = largestPeakMz;
      this.charge = charge;

      /**
       * see {@link TDFPasefFrameMsMsInfoTable#SCAN_NUM_END}
       */
      scanRange = Range.closedOpen(brukerScanNumBegin, brukerScanNumEnd);
    }

    boolean containsScanNum(int scanNum) {
      return scanRange.contains(scanNum);
    }

    public long getPrecursorId() {
      return precursorId;
    }

    public int getScanNumBegin() {
      return scanRange.lowerEndpoint();
    }

    public int getScanNumEnd() {
      return scanRange.upperEndpoint();
    }

    public double getLargestPeakMz() {
      return largestPeakMz;
    }

    public int getCharge() {
      return charge;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof FramePrecursorInfo)) {
        return false;
      }
      FramePrecursorInfo that = (FramePrecursorInfo) o;
      return getPrecursorId() == that.getPrecursorId()
          && Double.compare(that.getLargestPeakMz(), getLargestPeakMz()) == 0
          && getCharge() == that.getCharge() && scanRange.equals(that.scanRange);
    }

    @Override
    public int hashCode() {
      return Objects.hash(getPrecursorId(), getLargestPeakMz(), getCharge(), scanRange);
    }

  }
}
