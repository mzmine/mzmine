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

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationDateTimeType;
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationUserType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationDateTimeType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationReferencePressureType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationStdDevPercentType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationUserType;
import io.github.mzmine.datamodel.features.rawfiletypes.MzCalibrationModeType;
import io.github.mzmine.datamodel.features.rawfiletypes.MzCalibrationStdDevPpmType;
import io.github.mzmine.datamodel.features.rawfiletypes.RawFileMetadataTypes;
import io.github.mzmine.datamodel.features.types.DataType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The CalibrationInfo table of Bruker tdf and tsf files. Contains key value pairs per polarity,
 * e.g., the standard deviation of the m/z calibration and the date of the calibration.
 */
public class TDFCalibrationInfoTable extends TDFDataTable<String> {

  public static final String CALIBRATION_INFO_TABLE = "CalibrationInfo";
  public static final String KEY_NAME_COLUMN = "KeyName";
  public static final String KEY_POLARITY_COLUMN = "KeyPolarity";
  public static final String VALUE_COLUMN = "Value";
  private static final Logger logger = Logger.getLogger(TDFCalibrationInfoTable.class.getName());
  private static final String POSITIVE = "+";

  private final TDFDataColumn<String> polarityCol = new TDFDataColumn<>(KEY_POLARITY_COLUMN);
  private final TDFDataColumn<String> valueCol = new TDFDataColumn<>(VALUE_COLUMN);

  public TDFCalibrationInfoTable() {
    super(CALIBRATION_INFO_TABLE, KEY_NAME_COLUMN);
    columns.add(polarityCol);
    columns.add(valueCol);
  }

  /**
   * Not all files contain the calibration info table. Missing tables are skipped without error.
   */
  @Override
  public boolean executeQuery(@NotNull final Connection connection) {
    try (ResultSet tables = connection.getMetaData()
        .getTables(null, null, CALIBRATION_INFO_TABLE, null)) {
      if (!tables.next()) {
        logger.finest("No %s table in file".formatted(CALIBRATION_INFO_TABLE));
        return false;
      }
    } catch (SQLException e) {
      logger.log(Level.FINE, "Cannot check for table " + CALIBRATION_INFO_TABLE, e);
      return false;
    }
    return super.executeQuery(connection);
  }

  /**
   * @param key      the key
   * @param polarity the preferred polarity (+ or -) or null if there is no preference
   * @return the value of the key for the preferred polarity, otherwise for positive polarity,
   * otherwise of any polarity. null if the key does not exist.
   */
  public @Nullable String getValue(@NotNull final Keys key, @Nullable final String polarity) {
    String positive = null;
    String any = null;
    for (int i = 0; i < keyList.size(); i++) {
      if (!key.name().equals(keyList.get(i))) {
        continue;
      }
      final String rowPolarity = polarityCol.get(i);
      final String value = valueCol.get(i);
      if (polarity != null && polarity.equals(rowPolarity)) {
        return value;
      }
      if (positive == null && POSITIVE.equals(rowPolarity)) {
        positive = value;
      }
      if (any == null) {
        any = value;
      }
    }
    return positive != null ? positive : any;
  }

  /**
   * Sets all values that map to a raw file metadata type.
   *
   * @param metadata            the file metadata of the raw data file
   * @param frameDataPolarities the polarity of each frame, see the polarity column of the frames
   *                            table. Calibration values of this polarity are preferred if all
   *                            frames have the same polarity.
   * @param includeIms          true to also set ion mobility specific values, false for files
   *                            without ion mobility, e.g., tsf
   */
  public void applyToFileMetadata(@NotNull final ModularDataModel metadata,
      @NotNull final Collection<String> frameDataPolarities, final boolean includeIms) {
    final List<String> polarities = frameDataPolarities.stream().filter(Objects::nonNull).distinct()
        .toList();
    // decision: polarity switching files have calibrations for both, positive is used then
    final String polarity = polarities.size() == 1 ? polarities.getFirst() : null;

    for (final Keys key : Keys.values()) {
      if (key.isImsSpecific() && !includeIms) {
        continue;
      }
      RawFileMetadataTypes.setParsed(metadata, key.getFileMetadataType(), getValue(key, polarity));
    }
  }

  /**
   * Keys of the calibration info table that are mapped to the raw file metadata. Keys are tagged as
   * general or ion mobility specific.
   */
  public enum Keys {
    MzStandardDeviationPPM(MzCalibrationStdDevPpmType.class, false), //
    MzCalibrationMode(MzCalibrationModeType.class, false), //
    CalibrationUser(CalibrationUserType.class, false), //
    CalibrationDateTime(CalibrationDateTimeType.class, false), //
    MobilityStandardDeviationPercent(MobilityCalibrationStdDevPercentType.class, true), //
    MobilityCalibrationUser(MobilityCalibrationUserType.class, true), //
    MobilityCalibrationDateTime(MobilityCalibrationDateTimeType.class, true), //
    // spelling as in the Bruker schema
    MobilitiyReferencePressure(MobilityCalibrationReferencePressureType.class, true);

    private final @NotNull Class<? extends DataType<?>> fileMetadataType;
    private final boolean imsSpecific;

    Keys(@NotNull final Class<? extends DataType<?>> fileMetadataType, final boolean imsSpecific) {
      this.fileMetadataType = fileMetadataType;
      this.imsSpecific = imsSpecific;
    }

    /**
     * @return the raw file metadata type this key is mapped to
     */
    public @NotNull Class<? extends DataType<?>> getFileMetadataType() {
      return fileMetadataType;
    }

    /**
     * @return true if the key only applies to ion mobility data, false if it is general
     */
    public boolean isImsSpecific() {
      return imsSpecific;
    }
  }
}
