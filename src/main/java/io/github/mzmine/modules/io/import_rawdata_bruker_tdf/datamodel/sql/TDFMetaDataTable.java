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

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.DateTimeUtils;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
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
        valueCol.get(keyList.indexOf(Keys.SchemaVersionMajor.name())) + "."
            + valueCol.get(keyList.indexOf(Keys.SchemaVersionMinor.name()));

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

  // we only keep these keys from the metadata table. Add more, if we need anything else.
  public enum Keys {
    SchemaType, SchemaVersionMajor, SchemaVersionMinor, MzAcqRangeLower, MzAcqRangeUpper,
    OneOverK0AcqRangeLower, OneOverK0AcqRangeUpper, AcquisitionSoftwareVersion, InstrumentName,
    Description, SampleName, MethodName, HasProfileSpectra, HasLineSpectra, ImagingAreaMinXIndexPos,
    ImagingAreaMaxXIndexPos, ImagingAreaMinYIndexPos, ImagingAreaMaxYIndexPos, AcquisitionDateTime;
  }

  public String getValueForKey(Keys key) {
    int index = keyCol.indexOf(key.toString());
    return index != -1 ? valueCol.get(index) : "";
  }
}
