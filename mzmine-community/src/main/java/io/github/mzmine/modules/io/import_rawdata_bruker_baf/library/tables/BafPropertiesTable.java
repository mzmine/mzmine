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

package io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionMethodNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareType;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareVersionType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentModelType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentSerialNumberType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentVendorType;
import io.github.mzmine.datamodel.features.rawfiletypes.OperatorNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.SampleNameType;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFDataColumn;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFDataTable;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
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

  /**
   * Sets all values that map to a raw file metadata type.
   *
   * @param metadata the file metadata of the raw data file
   */
  public void applyToFileMetadata(@NotNull final ModularDataModel metadata) {
    for (final Values key : Values.values()) {
      final Class<? extends StringType> type = key.getFileMetadataType();
      if (type == null) {
        continue;
      }
      final String value = getValue(key);
      if (value != null && !value.isBlank()) {
        metadata.set(type, value.strip());
      }
    }
  }

  public enum Values {
    SchemaType, //
    AcquisitionSoftware(AcquisitionSoftwareType.class), //
    AcquisitionSoftwareVendor, //
    AcquisitionSoftwareVersion(AcquisitionSoftwareVersionType.class), //
    InstrumentVendor(InstrumentVendorType.class), //
    InstrumentFamily, //
    InstrumentName(InstrumentModelType.class), //
    InstrumentRevision, //
    // decision: numeric vendor code without known mapping
    InstrumentSourceType, //
    OperatorName(OperatorNameType.class), //
    Description, //
    SampleName(SampleNameType.class), //
    AcquisitionMethod(AcquisitionMethodNameType.class), //
    AcquisitionDateTime, //
    InstrumentSerialNumber(InstrumentSerialNumberType.class);

    private final @Nullable Class<? extends StringType> fileMetadataType;

    Values() {
      this(null);
    }

    Values(@Nullable final Class<? extends StringType> fileMetadataType) {
      this.fileMetadataType = fileMetadataType;
    }

    /**
     * @return the raw file metadata type this value is mapped to or null
     */
    public @Nullable Class<? extends StringType> getFileMetadataType() {
      return fileMetadataType;
    }
  }

  @Nullable
  public String getValue(Values value) {
    return IntStream.range(0, keyColumn.size())
        .filter(i -> keyColumn.get(i).equals(value.toString())).mapToObj(valueColumn::get).findAny()
        .orElse(null);
  }
}
