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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareType;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareVersionType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentModelType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentSerialNumberType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentVendorType;
import io.github.mzmine.datamodel.features.rawfiletypes.OperatorNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.SampleNameType;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.annotations.AcquisitionMethodType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.date.DateTimeUtils;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.AcquisitionMetadata.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TDFMetaDataTable extends TDFDataTable<String> {

  private static final Logger logger = Logger.getLogger(TDFMetaDataTable.class.getName());

  public static final String METADATA_TABLE = "GlobalMetadata";
  public static final String VALUE_COMLUMN = "Value";
  public static final String KEY_COMLUMN = "Key";

  private static final List<String> allowedFileVersions = Arrays.asList("3.1", "3.2");

  private final TDFDataColumn<String> valueCol;
  private final TDFDataColumn<String> keyCol;

  public TDFMetaDataTable() {
    super(METADATA_TABLE, KEY_COMLUMN);

    keyCol = (TDFDataColumn<String>) columns.get(0);
    valueCol = new TDFDataColumn<>(VALUE_COMLUMN);

    columns.add(valueCol);
  }

  private Range<Double> mzRange;
  private String instrumentType;

  /**
   * @return -1 if key does not exist, 0 if no line spectra exist, 1 if they do.
   */
  public boolean hasLineSpectra() {
    int index = keyCol.indexOf(Keys.HasLineSpectra.name());
    return index != -1 && Integer.parseInt(valueCol.get(index)) == 1;
  }

  public boolean isFileVersionValid() {
    if (valueCol == null) {
      return false;
    }
    String version =
        valueCol.get(keyList.indexOf(Keys.SchemaVersionMajor.name())) + "." + valueCol.get(
            keyList.indexOf(Keys.SchemaVersionMinor.name()));

    if (!allowedFileVersions.contains(version)) {
      MZmineCore.getDesktop().displayMessage(
          "TDF version " + version + " is not supported.\\This might lead to unexpected results.");
      return false;
    }

    return true;
  }

  @Override
  public boolean executeQuery(Connection connection) {
    boolean b = super.executeQuery(connection);
    if (!b) {
      return false;
    }

    for (int i = 0; i < keyList.size(); i++) {
      try {
        Keys.valueOf(keyList.get(i));
      } catch (IllegalArgumentException | ClassCastException e) {
        for (TDFDataColumn<?> col : columns) {
          col.remove(i);
        }
        i--;
      }
    }
//    print();
    return true;
  }

  public Range<Double> getMzRange() {
    if (mzRange == null) {
      if (keyList.isEmpty()) {
        logger.info("Cannot determine mz range. Metadata not loaded yet.");
        return Range.closed(0.d, 0.d);
      }
      int lowerIndex = keyList.indexOf(Keys.MzAcqRangeLower.name());
      int upperIndex = keyList.indexOf(Keys.MzAcqRangeUpper.name());
      if (lowerIndex == -1 || upperIndex == -1) {
        logger.info("Cannot determine mz range. Metadata did not contain required information.");
        return Range.closed(0.d, 0.d);
      }
      mzRange = Range.closed(Double.valueOf((String) getColumn(VALUE_COMLUMN).get(lowerIndex)),
          Double.valueOf((String) getColumn(VALUE_COMLUMN).get(upperIndex)));
    }
    return mzRange;
  }

  public String getInstrumentType() {
    if (instrumentType == null) {
      int row = keyList.indexOf(Keys.InstrumentName.name());
      instrumentType = (String) getColumn(VALUE_COMLUMN).get(row);
    }
    return instrumentType;
  }

  /**
   * @return -1 if key does not exist, 0 if no profile spectra exist, 1 if they do.
   */
  public boolean hasProfileSpectra() {
    int index = keyCol.indexOf(Keys.HasLineSpectra.name());
    return index != -1 && Integer.parseInt(valueCol.get(index)) == 1;
  }

  @Nullable
  public LocalDateTime getAcquisitionDateTime() {
    int index = keyCol.indexOf(Keys.AcquisitionDateTime.name());
    String date = index != -1 ? valueCol.get(index) : null;

    if(date == null) {
      return null;
    }
    try {
      return DateTimeUtils.parse(date);
    } catch (DateTimeParseException e) {
      var sampleName = valueCol.get(keyCol.indexOf(Keys.SampleName));
      logger.warning(() -> "Cannot parse acquisition date of sample " + sampleName);
      return null;
    }
  }

  /**
   * Sets all values of keys that map to a raw file metadata type.
   *
   * @param metadata the file metadata of the raw data file
   */
  public void applyToFileMetadata(@NotNull final ModularDataModel metadata) {
    for (final Keys key : Keys.values()) {
      final Class<? extends StringType> type = key.getFileMetadataType();
      if (type == null) {
        continue;
      }
      final String value = getValueForKey(key);
      if (value != null && !value.isBlank()) {
        metadata.set(type, value.strip());
      }
    }
  }

  // we only keep these keys from the metadata table. Add more, if we need anything else.
  public enum Keys {
    SchemaType, SchemaVersionMajor, SchemaVersionMinor, MzAcqRangeLower, MzAcqRangeUpper, OneOverK0AcqRangeLower, OneOverK0AcqRangeUpper, //
    AcquisitionSoftware(AcquisitionSoftwareType.class), //
    AcquisitionSoftwareVendor, //
    AcquisitionSoftwareVersion(AcquisitionSoftwareVersionType.class), //
    InstrumentName(InstrumentModelType.class), //
    InstrumentVendor(InstrumentVendorType.class), //
    InstrumentSerialNumber(InstrumentSerialNumberType.class), //
    OperatorName(OperatorNameType.class), //
    Description, //
    SampleName(SampleNameType.class), //
    MethodName(AcquisitionMethodType.class), //
    HasProfileSpectra, HasLineSpectra, ImagingAreaMinXIndexPos, Geometry, ImagingAreaMaxXIndexPos, ImagingAreaMinYIndexPos, ImagingAreaMaxYIndexPos, AcquisitionDateTime;

    private final @Nullable Class<? extends StringType> fileMetadataType;

    Keys() {
      this(null);
    }

    Keys(@Nullable final Class<? extends StringType> fileMetadataType) {
      this.fileMetadataType = fileMetadataType;
    }

    /**
     * @return the raw file metadata type this key is mapped to or null
     */
    public @Nullable Class<? extends StringType> getFileMetadataType() {
      return fileMetadataType;
    }
  }

  /** Reuses parsed SQLite metadata; names and study fields are retained only inside mzmine. */


  public String getValueForKey(Keys key) {
    int index = keyCol.indexOf(key.toString());
    return index != -1 ? valueCol.get(index) : "";
  }

  /** Retains selected vendor header values without turning them into editable study metadata. */
  public @NotNull AcquisitionMetadata acquisitionMetadata(final @NotNull Collection<Long> scanModes) {
    final Map<String, String> fields = new LinkedHashMap<>();
    for (final Keys key : List.of(Keys.InstrumentName, Keys.AcquisitionSoftwareVersion,
        Keys.SampleName, Keys.MethodName, Keys.Description)) {
      final String value = getValueForKey(key);
      if (value != null && !value.isBlank()) {
        fields.put(key.name(), value);
      }
    }
    final List<AcquisitionMetadata.Term> terms = new ArrayList<>(AcquisitionMetadata.resolveLabel(
        Field.INSTRUMENT_MODEL, getValueForKey(Keys.InstrumentName)));
    // Native scan-mode declarations are evidence for DDA/DIA; MS levels alone are not.
    if (scanModes.contains(1L)) {
      terms.addAll(AcquisitionMetadata.resolve("MS:1003221"));
    }
    if (scanModes.contains(9L)) {
      terms.addAll(AcquisitionMetadata.resolve("MS:1003215"));
    }
    fields.put("Acquisition scan modes", scanModes.stream().distinct().sorted().map(mode ->
        io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.BrukerScanMode.fromScanMode(
            mode.intValue()).getDescription() + " (" + mode + ")")
        .collect(java.util.stream.Collectors.joining(", ")));
    return new AcquisitionMetadata(terms, fields);
  }
}
