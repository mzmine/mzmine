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

package io.github.mzmine.modules.io.import_rawdata_bruker_baf;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationDateTimeType;
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationUserType;
import io.github.mzmine.datamodel.features.rawfiletypes.MzCalibrationModeType;
import io.github.mzmine.datamodel.features.rawfiletypes.MzCalibrationStdDevPpmType;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.BafCalibrationVariables;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Reads the calibration variables from the sqlite cache of a baf file. Skipped if the example file
 * is not available.
 */
class BafCalibrationVariablesTest {

  private static final File SQLITE_CACHE = new File(
      "D:\\OneDrive - mzio GmbH\\Example data - Documents\\Bruker\\baf\\D1_1 _ng_g_RA5_7013.d\\analysis.sqlite");

  @Test
  void readsActiveTofCalibration() throws Exception {
    if (!SQLITE_CACHE.exists()) {
      return;
    }
    final BafCalibrationVariables variables = new BafCalibrationVariables();
    synchronized (org.sqlite.JDBC.class) {
      try (Connection connection = DriverManager.getConnection(
          "jdbc:sqlite:" + SQLITE_CACHE.getAbsolutePath())) {
        Assertions.assertTrue(variables.executeQuery(connection));
      }
    }

    Assertions.assertEquals(1, variables.getActiveTofCalibrationVersion());

    final ModularDataModel metadata = new SimpleModularDataModel();
    variables.applyToFileMetadata(metadata);

    // TOF1 values, not the TOF2 values 0.827 ppm and mode 5
    Assertions.assertEquals(0.513062255420521, metadata.get(MzCalibrationStdDevPpmType.class));
    Assertions.assertEquals(12, metadata.get(MzCalibrationModeType.class));
    Assertions.assertEquals("Demo User", metadata.get(CalibrationUserType.class));
    // 2025-05-16T10:22:51+02:00 in UTC
    Assertions.assertEquals(LocalDateTime.of(2025, 5, 16, 8, 22, 51),
        metadata.get(CalibrationDateTimeType.class));
  }
}
