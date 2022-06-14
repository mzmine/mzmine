/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

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

  // we only keep these keys from the metadata table. Add more, if we need anything else.
  public enum Keys {
    SchemaType, SchemaVersionMajor, SchemaVersionMinor, MzAcqRangeLower, MzAcqRangeUpper, //
    OneOverK0AcqRangeLower, OneOverK0AcqRangeUpper, AcquisitionSoftwareVersion, InstrumentName, //
    Description, SampleName, MethodName, HasProfileSpectra, HasLineSpectra, ImagingAreaMinXIndexPos, //
    Geometry, ImagingAreaMaxXIndexPos, ImagingAreaMinYIndexPos, ImagingAreaMaxYIndexPos;
  }

  public String getValueForKey(Keys key) {
    int index = keyCol.indexOf(key.toString());
    return index != -1 ? valueCol.get(index) : "";
  }
}
