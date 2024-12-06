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
import java.util.stream.IntStream;
import org.jetbrains.annotations.Nullable;

public class BafPropertiesTable extends TDFDataTable<String> {

  public static final String NAME = "Properties";
  public static final String KEY_COLUMN = "Key";
  public static final String VALUE_COLUMN = "Value";

  private final TDFDataColumn<String> keyColumn;
  private final TDFDataColumn<String> valueColumn = new TDFDataColumn<>(VALUE_COLUMN);


  public BafPropertiesTable() {
    super(NAME, KEY_COLUMN);
    keyColumn = (TDFDataColumn<String>) getColumn(KEY_COLUMN);
    columns.add(valueColumn);
  }

  public enum Values {
    SchemaType, AcquisitionSoftware, AcquisitionSoftwareVendor, AcquisitionSoftwareVersion, //
    InstrumentVendor, InstrumentFamily, InstrumentName, InstrumentRevision, InstrumentSourceType, //
    OperatorName, Description, SampleName, AcquisitionMethod, AcquisitionDateTime, InstrumentSerialNumber
  }

  @Nullable
  public String getValue(Values value) {
    return IntStream.range(0, keyColumn.size())
        .filter(i -> keyColumn.get(i).equals(value.toString())).mapToObj(valueColumn::get).findAny()
        .orElse(null);
  }
}
