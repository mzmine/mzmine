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
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationDateTimeType;
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationUserType;
import io.github.mzmine.datamodel.features.rawfiletypes.MzCalibrationModeType;
import io.github.mzmine.datamodel.features.rawfiletypes.MzCalibrationStdDevPpmType;
import io.github.mzmine.datamodel.features.rawfiletypes.RawFileMetadataTypes;
import io.github.mzmine.datamodel.features.types.DataType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calibration information of baf files from the per spectrum {@link BafVariables} of the sqlite
 * cache. Variables are identified by the
 * {@link BafSupportedVariables#PERMANENT_NAME_COL permanent name} and mapped to the same raw file
 * metadata types as the CalibrationInfo table of tdf and tsf files. For each variable, the most
 * frequent value over all spectra is used.
 */
public class BafCalibrationVariables {

  private static final Logger logger = Logger.getLogger(BafCalibrationVariables.class.getName());

  private static final String MOST_FREQUENT_VALUE_QUERY = """
      SELECT v.%s, COUNT(*) AS n FROM %s v
      JOIN %s sv ON sv.%s = v.%s
      WHERE sv.%s = ? AND v.%s IS NOT NULL
      GROUP BY v.%s ORDER BY n DESC LIMIT 1""".formatted(BafVariables.VALUE_COL, BafVariables.NAME,
      BafSupportedVariables.NAME, BafSupportedVariables.VARIABLE_COL, BafVariables.VARIABLE_COL,
      BafSupportedVariables.PERMANENT_NAME_COL, BafVariables.VALUE_COL, BafVariables.VALUE_COL);

  private static final int DEFAULT_TOF_CALIBRATION_VERSION = 1;

  private final Map<Variable, String> values = new EnumMap<>(Variable.class);

  /**
   * Reads the most frequent value of each {@link Variable}.
   *
   * @return true if the query was successful
   */
  public boolean executeQuery(@NotNull final Connection connection) {
    values.clear();
    try (PreparedStatement statement = connection.prepareStatement(MOST_FREQUENT_VALUE_QUERY)) {
      for (final Variable variable : Variable.values()) {
        statement.setString(1, variable.name());
        try (ResultSet rs = statement.executeQuery()) {
          if (rs.next()) {
            final String value = rs.getString(1);
            if (value != null && !value.isBlank()) {
              values.put(variable, value.strip());
            }
          }
        }
      }
      return true;
    } catch (SQLException e) {
      logger.log(Level.FINE, "Cannot read calibration variables of baf file: " + e.getMessage(), e);
      return false;
    }
  }

  /**
   * @return the most frequent value of the variable or null
   */
  public @Nullable String getValue(@NotNull final Variable variable) {
    return values.get(variable);
  }

  /**
   * @return the active TOF calibration, 1 or 2. Defaults to 1 if not available.
   */
  public int getActiveTofCalibrationVersion() {
    final String version = values.get(Variable.Calibration_TofCalVersion);
    if (version == null) {
      return DEFAULT_TOF_CALIBRATION_VERSION;
    }
    try {
      return (int) Double.parseDouble(version);
    } catch (NumberFormatException e) {
      return DEFAULT_TOF_CALIBRATION_VERSION;
    }
  }

  /**
   * Sets all values that map to a raw file metadata type. The m/z calibration values are taken from
   * the active TOF calibration.
   */
  public void applyToFileMetadata(@NotNull final ModularDataModel metadata) {
    final int activeTofCalibration = getActiveTofCalibrationVersion();
    for (final Variable variable : Variable.values()) {
      final Class<? extends DataType<?>> type = variable.getFileMetadataType();
      if (type == null) {
        continue;
      }
      final Integer tofCalibration = variable.getTofCalibrationVersion();
      if (tofCalibration != null && tofCalibration != activeTofCalibration) {
        continue;
      }
      RawFileMetadataTypes.setParsed(metadata, type, values.get(variable));
    }
  }

  /**
   * Variables by their permanent name in the SupportedVariables table. Baf files are imported
   * without ion mobility, IMS specific variables are not mapped.
   */
  public enum Variable {
    // defines the active TOF calibration per spectrum
    // assumption: version 1 uses the TofCal (TOF1) variables, version 2 the Tof2Cal variables
    Calibration_TofCalVersion(null, null), //
    Calibration_StdDevInPPM(MzCalibrationStdDevPpmType.class, 1), //
    Calibration_RegressionMode(MzCalibrationModeType.class, 1), //
    Calibration_Tof2StdDevInPPM(MzCalibrationStdDevPpmType.class, 2), //
    Calibration_Tof2CalibrationMode(MzCalibrationModeType.class, 2), //
    Calibration_LastCalibrationDate(CalibrationDateTimeType.class, null), //
    Calibration_LastCalibrationUser(CalibrationUserType.class, null);

    private final @Nullable Class<? extends DataType<?>> fileMetadataType;
    private final @Nullable Integer tofCalibrationVersion;

    Variable(@Nullable final Class<? extends DataType<?>> fileMetadataType,
        @Nullable final Integer tofCalibrationVersion) {
      this.fileMetadataType = fileMetadataType;
      this.tofCalibrationVersion = tofCalibrationVersion;
    }

    /**
     * @return the raw file metadata type or null if the variable is not mapped directly
     */
    public @Nullable Class<? extends DataType<?>> getFileMetadataType() {
      return fileMetadataType;
    }

    /**
     * @return the TOF calibration this variable belongs to or null if it applies to all
     */
    public @Nullable Integer getTofCalibrationVersion() {
      return tofCalibrationVersion;
    }
  }
}
