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

public class BafVariables extends TDFDataTable<Long> {

  public static final String NAME = "Variables";

  public static final String SPECTRUM_COL = "Spectrum";
  public static final String VARIABLE_COL = "Variable";
  public static final String VALUE_COL = "Value";

  private final TDFDataColumn<Long> spectrumColumn;
  private final TDFDataColumn<Long> variableColumn = new TDFDataColumn<>(VARIABLE_COL);
  private final TDFDataColumn<Object> valueColumn = new TDFDataColumn<>(VALUE_COL);

  public BafVariables(String table, String entryHeader) {
    super(table, entryHeader);
    spectrumColumn = (TDFDataColumn<Long>) getColumn(SPECTRUM_COL);
    columns.addAll(Arrays.asList(variableColumn, valueColumn));
  }
}
