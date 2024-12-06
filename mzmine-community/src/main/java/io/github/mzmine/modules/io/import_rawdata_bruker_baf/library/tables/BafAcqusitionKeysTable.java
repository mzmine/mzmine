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

public class BafAcqusitionKeysTable extends TDFDataTable<Long> {

  public static final String NAME = "AcquisitionKeys";
  public static final String ID_COL = "Id";
  public static final String POLAIRTY_COL = "Polarity";
  public static final String SCAN_MODE_COL = "ScanMode";
  public static final String ACQUISITION_MODE_COL = "AcquisitionMode";
  public static final String MsLevelCol = "MsLevel";

  private final TDFDataColumn<Long> idCol;
  private final TDFDataColumn<Long> polarityCol = new TDFDataColumn<>(POLAIRTY_COL);
  private final TDFDataColumn<Long> scanModeCol = new TDFDataColumn<>(SCAN_MODE_COL);
  private final TDFDataColumn<Long> acquisitionModeCol = new TDFDataColumn<>(ACQUISITION_MODE_COL);
  private final TDFDataColumn<Long> msLevelCol = new TDFDataColumn<>(MsLevelCol);

  public BafAcqusitionKeysTable() {
    super("AcquisitionKeys", ID_COL);

    idCol = (TDFDataColumn<Long>) getColumn(ID_COL);
    columns.addAll(Arrays.asList(polarityCol, scanModeCol, acquisitionModeCol, msLevelCol));
  }
}
