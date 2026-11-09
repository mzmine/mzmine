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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationDateTimeType;
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationUserType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationDateTimeType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationReferencePressureType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationStdDevPercentType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityCalibrationUserType;
import io.github.mzmine.datamodel.features.rawfiletypes.MzCalibrationModeType;
import io.github.mzmine.datamodel.features.rawfiletypes.MzCalibrationStdDevPpmType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFCalibrationInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFCalibrationInfoTable.Keys;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Reads the CalibrationInfo table of a tims test file and maps it to the raw file metadata.
 */
class TDFCalibrationInfoTableTest {

  private static @NotNull TDFCalibrationInfoTable readTable() throws Exception {
    final File tdf = new File(Objects.requireNonNull(
        TDFCalibrationInfoTableTest.class.getClassLoader()
            .getResource("rawdatafiles/additional/lc-tims-ms-pasef-a.d/analysis.tdf")).toURI());
    final TDFCalibrationInfoTable table = new TDFCalibrationInfoTable();
    synchronized (org.sqlite.JDBC.class) {
      try (Connection connection = DriverManager.getConnection(
          "jdbc:sqlite:" + tdf.getAbsolutePath())) {
        Assertions.assertTrue(table.executeQuery(connection));
      }
    }
    return table;
  }

  @Test
  void imsFileMapsAllKeys() throws Exception {
    final ModularDataModel metadata = new SimpleModularDataModel();
    readTable().applyToFileMetadata(metadata, List.of("+", "+"), true);

    Assertions.assertEquals(0.755987, metadata.get(MzCalibrationStdDevPpmType.class));
    Assertions.assertEquals(7, metadata.get(MzCalibrationModeType.class));
    Assertions.assertEquals("unknown", metadata.get(CalibrationUserType.class));
    // 2024-01-17T13:22:53+01:00 in UTC, same as the acquisition date
    Assertions.assertEquals(LocalDateTime.of(2024, 1, 17, 12, 22, 53),
        metadata.get(CalibrationDateTimeType.class));

    Assertions.assertEquals(674.585316, metadata.get(MobilityCalibrationStdDevPercentType.class));
    Assertions.assertEquals("unknown", metadata.get(MobilityCalibrationUserType.class));
    Assertions.assertEquals(LocalDateTime.of(2024, 1, 16, 16, 17, 53),
        metadata.get(MobilityCalibrationDateTimeType.class));
    Assertions.assertEquals(2.52874, metadata.get(MobilityCalibrationReferencePressureType.class));
  }

  @Test
  void nonImsFileSkipsMobilityKeys() throws Exception {
    final ModularDataModel metadata = new SimpleModularDataModel();
    readTable().applyToFileMetadata(metadata, List.of("+"), false);

    Assertions.assertEquals(0.755987, metadata.get(MzCalibrationStdDevPpmType.class));
    Assertions.assertNotNull(metadata.get(CalibrationDateTimeType.class));
    for (final Keys key : Keys.values()) {
      if (key.isImsSpecific()) {
        final DataType<?> type = DataTypes.get(key.getFileMetadataType());
        Assertions.assertNull(metadata.get(type),
            () -> key + " is ion mobility specific and must not be set for non IMS files");
      }
    }
  }

  @Test
  void unknownPolarityFallsBackToAvailableValue() throws Exception {
    // the test file only has positive calibration values
    final TDFCalibrationInfoTable table = readTable();
    Assertions.assertEquals("0.755987", table.getValue(Keys.MzStandardDeviationPPM, "-"));
    Assertions.assertEquals("0.755987", table.getValue(Keys.MzStandardDeviationPPM, null));
  }
}
