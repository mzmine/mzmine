/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import io.github.mzmine.datamodel.impl.DIAImsMsMsInfoImpl;
import io.github.mzmine.util.RangeUtils;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DiaFrameMsMsWindowTable extends TDFDataTable<Long> {

  public static final String DIA_FRAME_MS_MS_WINDOW_TABLE_NAME = "DiaFrameMsMsWindows";
  public static final String windowGroupColName = "WindowGroup";

  private final TDFDataColumn<Long> windowGroupColumn;
  private final TDFDataColumn<Long> scanNumEndColumn;
  // Spectrum range as listed in the bruker table. (scan numbers start at 1 and are end-exclusive)
  private final TDFDataColumn<Long> scanNumBeginColumn;
  private final TDFDataColumn<Double> isolationMzColumn;
  private final TDFDataColumn<Double> isolationWidthColumn;
  private final TDFDataColumn<Double> collisionEnergyColumn;

  private final Map<Long, Set<DIAImsMsMsInfoImpl>> windowGroupMsMsInfoMap = new HashMap<>();

  public DiaFrameMsMsWindowTable() {
    super(DIA_FRAME_MS_MS_WINDOW_TABLE_NAME, windowGroupColName);

    windowGroupColumn = (TDFDataColumn<Long>) getColumn(windowGroupColName);

    scanNumBeginColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.SCAN_NUM_BEGIN);
    scanNumEndColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.SCAN_NUM_END);
    isolationMzColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.ISOLATION_MZ);
    isolationWidthColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.ISOLATION_WIDTH);
    collisionEnergyColumn = new TDFDataColumn<>(TDFPasefFrameMsMsInfoTable.COLLISION_ENERGY);

    columns.addAll(
        Arrays.asList(scanNumBeginColumn, scanNumEndColumn, isolationMzColumn, isolationWidthColumn,
            collisionEnergyColumn));
  }

  @Override
  public boolean executeQuery(Connection connection) {
    boolean ok = super.executeQuery(connection);
    if (ok) {
      buildMsMsInfos();
    }
    return ok;
  }

  private void buildMsMsInfos() {
    for (int i = 0; i < windowGroupColumn.size(); i++) {
      // Spectrum range are listed in the bruker table where scan numbers start at 1 and are end-exclusive
      // hence we need to subtract 1 from the start and 2 from the end, because we deal with incluse ranges and 0 indices
      final Range<Integer> scanRange = Range.closedOpen(scanNumBeginColumn.get(i).intValue() - 1,
          scanNumEndColumn.get(i).intValue() - 2);
      final Double mz = isolationMzColumn.get(i);
      final Double width = isolationWidthColumn.get(i);
      final Range<Double> isolationRange = RangeUtils.rangeAround(mz, width);

      final DIAImsMsMsInfoImpl diaImsMsMsInfo = new DIAImsMsMsInfoImpl(scanRange,
          collisionEnergyColumn.get(i).floatValue(), null, isolationRange);
      windowGroupMsMsInfoMap.computeIfAbsent(windowGroupColumn.get(i), l -> new HashSet<>())
          .add(diaImsMsMsInfo);
    }
  }

  public Map<Long, Set<DIAImsMsMsInfoImpl>> getWindowGroupMsMsInfoMap() {
    return windowGroupMsMsInfoMap;
  }
}
