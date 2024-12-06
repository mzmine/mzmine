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

public class BafSupportedVariables extends TDFDataTable<Long> {

  public static final String NAME = "SupportedVariables";

  public static final String VARIABLE_COL = "Variable";
  public static final String PERMANENT_NAME_COL = "PermanentName";
  public static final String TYPE_COL = "Type";
  public static final String DISPLAY_GROUP_NAME_COL = "DisplayGroupName";
  public static final String DISPLAY_NAME_COL = "DisplayName";
  public static final String DISPLAY_VALUE_TEXT_COL = "DisplayValueText";
  public static final String DISPLAY_FORMAT_COL = "DisplayFormat";
  public static final String DISPLAY_DIMENSION_COL = "DisplayDimension";

  private final TDFDataColumn<Long> variableColumn;
  private final TDFDataColumn<String> permanentNameColumn = new TDFDataColumn<>(PERMANENT_NAME_COL);
  private final TDFDataColumn<Long> typeColumn = new TDFDataColumn<>(TYPE_COL);
  private final TDFDataColumn<String> displayGroupNameColumn = new TDFDataColumn<>(
      DISPLAY_GROUP_NAME_COL);
  private final TDFDataColumn<String> displayNameColumn = new TDFDataColumn<>(DISPLAY_NAME_COL);
  private final TDFDataColumn<String> displayValueTextColumn = new TDFDataColumn<>(
      DISPLAY_VALUE_TEXT_COL);
  private final TDFDataColumn<String> displayFormatColumn = new TDFDataColumn<>(DISPLAY_FORMAT_COL);
  private final TDFDataColumn<String> displayDimensionColumn = new TDFDataColumn<>(
      DISPLAY_DIMENSION_COL);

  public BafSupportedVariables() {
    super(NAME, VARIABLE_COL);
    variableColumn = (TDFDataColumn<Long>) getColumn(VARIABLE_COL);
    columns.addAll(
        Arrays.asList(permanentNameColumn, typeColumn, displayGroupNameColumn, displayNameColumn,
            displayValueTextColumn, displayFormatColumn, displayDimensionColumn));
  }
}
