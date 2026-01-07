/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import java.util.HashMap;
import java.util.Map;

public class DiaFrameMsMsInfoTable extends TDFDataTable<Long> {

  private final TDFDataColumn<Long> windowGroupColumn;
  private final TDFDataColumn<Long> frameColumn;

  public DiaFrameMsMsInfoTable() {
    super("DiaFrameMsMsInfo", TDFPasefFrameMsMsInfoTable.FRAME_ID);

    frameColumn = (TDFDataColumn<Long>) getColumn(TDFPasefFrameMsMsInfoTable.FRAME_ID);
    windowGroupColumn = new TDFDataColumn<>(DiaFrameMsMsWindowTable.windowGroupColName);
    columns.add(windowGroupColumn);
  }

  public Map<Long, Long> getFrameToWindowGroupMap() {
    final HashMap<Long, Long> map = new HashMap<>();

    for(int i = 0; i < frameColumn.size(); i++) {
      map.put(frameColumn.get(i), windowGroupColumn.get(i));
    }
    return map;
  }
}
