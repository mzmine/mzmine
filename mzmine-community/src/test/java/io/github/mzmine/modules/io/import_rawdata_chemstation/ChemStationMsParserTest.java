/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_chemstation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.io.import_rawdata_chemstation.ChemStationMsParser.ChemStationScan;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChemStationMsParserTest {

  @TempDir
  Path temporaryDirectory;

  @Test
  void detectsAndDecodesFullChemStationFile() throws IOException {
    final File folder = temporaryDirectory.resolve("sample.D").toFile();
    assertTrue(folder.mkdir());
    writeSyntheticDataMs(folder.toPath().resolve("DATA.MS"));

    assertEquals(RawDataFileType.AGILENT_CHEMSTATION_D,
        RawDataFileTypeDetector.detectDataFileType(folder));

    try (ChemStationMsParser parser = new ChemStationMsParser(folder)) {
      assertEquals(2, parser.getTotalScans());

      final ChemStationScan first = parser.readNextScan();
      assertNotNull(first);
      assertEquals(1f, first.retentionTime());
      assertArrayEquals(new double[]{50d, 51d}, first.mzValues());
      assertArrayEquals(new double[]{10d, 24d}, first.intensityValues());

      final ChemStationScan second = parser.readNextScan();
      assertNotNull(second);
      assertEquals(2f, second.retentionTime());
      assertArrayEquals(new double[]{50d}, second.mzValues());
      assertArrayEquals(new double[]{12d}, second.intensityValues());
      assertNull(parser.readNextScan());
      assertEquals(1d, parser.getFinishedPercentage());
    }
  }

  @Test
  void acceptsDirectDataMsSelectionAndMapsItToUppercaseDFolder() throws IOException {
    final File folder = temporaryDirectory.resolve("direct.D").toFile();
    assertTrue(folder.mkdir());
    final File dataMs = folder.toPath().resolve("DATA.MS").toFile();
    writeSyntheticDataMs(dataMs.toPath());

    assertEquals(RawDataFileType.AGILENT_CHEMSTATION_D,
        RawDataFileTypeDetector.detectDataFileType(dataMs));
    assertEquals(folder,
        AllSpectralDataImportModule.validateBrukerPath(dataMs));
    try (ChemStationMsParser parser = new ChemStationMsParser(dataMs)) {
      assertEquals(2, parser.getTotalScans());
      assertNotNull(parser.readNextScan());
    }
  }

  @Test
  void recognizesNamedDataMsBeforeDetailedParserValidation() throws IOException {
    final File folder = temporaryDirectory.resolve("named.D").toFile();
    assertTrue(folder.mkdir());
    final File dataMs = folder.toPath().resolve("data.ms").toFile();
    Files.write(dataMs.toPath(), new byte[0x200]);

    assertEquals(RawDataFileType.AGILENT_CHEMSTATION_D,
        RawDataFileTypeDetector.detectDataFileType(folder));
    assertEquals(RawDataFileType.AGILENT_CHEMSTATION_D,
        RawDataFileTypeDetector.detectDataFileType(dataMs));
    assertEquals(folder, AllSpectralDataImportModule.validateBrukerPath(dataMs));
    assertThrows(IOException.class, () -> new ChemStationMsParser(folder));
  }

  @Test
  void doesNotSendUnknownDFolderLayoutToMsConvert() throws IOException {
    final File folder = temporaryDirectory.resolve("unknown.D").toFile();
    assertTrue(folder.mkdir());
    Files.writeString(folder.toPath().resolve("vendor.payload"), "vendor data");

    assertNull(RawDataFileTypeDetector.detectDataFileType(folder));
  }

  @Test
  void recognizesAcqDataDirectoryCaseInsensitively() {
    final File folder = temporaryDirectory.resolve("modern.D").toFile();
    assertTrue(folder.mkdir());
    assertTrue(folder.toPath().resolve("ACQDATA").toFile().mkdir());

    assertEquals(RawDataFileType.AGILENT_D,
        RawDataFileTypeDetector.detectDataFileType(folder));
  }

  @Test
  void acqDataFolderWinsOverAnUnrelatedMsFile() throws IOException {
    // The header probe accepts any .ms file with a zero pointer at 0x10A, which an unrelated
    // method or tune export can satisfy by accident. A modern AcqData tree must still win.
    final File folder = temporaryDirectory.resolve("modern-with-stray-ms.D").toFile();
    assertTrue(folder.mkdir());
    assertTrue(folder.toPath().resolve("AcqData").toFile().mkdir());
    final byte[] strayFile = new byte[0x200];
    Files.write(folder.toPath().resolve("tune.ms"), strayFile);

    assertEquals(RawDataFileType.AGILENT_D, RawDataFileTypeDetector.detectDataFileType(folder));
  }

  @Test
  void renamedChemStationPayloadIsStillRecognized() throws IOException {
    // No newer vendor layout is present, so the permissive header probe may still claim the
    // folder for the legacy reader even though the payload is not called DATA.MS.
    final File folder = temporaryDirectory.resolve("renamed.D").toFile();
    assertTrue(folder.mkdir());
    writeSyntheticDataMs(folder.toPath().resolve("ACQ.ms"));

    assertEquals(RawDataFileType.AGILENT_CHEMSTATION_D,
        RawDataFileTypeDetector.detectDataFileType(folder));
  }

  @Test
  void readsConfiguredRealChemStationFolderWhenAvailable() throws IOException {
    final String configuredFolder = System.getenv("CHEMSTATION_TEST_DIR");
    if (configuredFolder == null || configuredFolder.isBlank()) {
      return;
    }

    try (ChemStationMsParser parser = new ChemStationMsParser(new File(configuredFolder))) {
      assertTrue(parser.getTotalScans() > 100);
      ChemStationScan scan;
      float previousRt = -1f;
      int count = 0;
      int nonEmptyScans = 0;
      while ((scan = parser.readNextScan()) != null) {
        assertTrue(scan.retentionTime() >= previousRt);
        assertEquals(scan.mzValues().length, scan.intensityValues().length);
        if (scan.mzValues().length > 0) {
          nonEmptyScans++;
        }
        previousRt = scan.retentionTime();
        count++;
      }
      assertEquals(parser.getTotalScans(), count);
      assertTrue(nonEmptyScans > 0);
    }
  }

  @Test
  void importsSyntheticFolderIntoProject() throws IOException {
    final File folder = temporaryDirectory.resolve("import.D").toFile();
    assertTrue(folder.mkdir());
    writeSyntheticDataMs(folder.toPath().resolve("DATA.MS"));

    final MZmineProjectImpl project = new MZmineProjectImpl();
    final SimpleParameterSet parameters = new SimpleParameterSet(new Parameter<?>[0]);
    final ChemStationImportTask task = new ChemStationImportTask(project, folder,
        ScanImportProcessorConfig.createDefault(), AllSpectralDataImportModule.class, parameters,
        java.time.Instant.now(), null);
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    assertEquals(1, project.getNumberOfDataFiles());
    assertEquals(2, project.getDataFiles()[0].getNumOfScans());
    assertEquals(24d, project.getDataFiles()[0].getScan(0).getBasePeakIntensity());
  }

  @Test
  void importsConfiguredRealFolderIntoProjectWhenAvailable() {
    final String configuredFolder = System.getenv("CHEMSTATION_TEST_DIR");
    if (configuredFolder == null || configuredFolder.isBlank()) {
      return;
    }

    final MZmineProjectImpl project = new MZmineProjectImpl();
    final SimpleParameterSet parameters = new SimpleParameterSet(new Parameter<?>[0]);
    final ChemStationImportTask task = new ChemStationImportTask(project,
        new File(configuredFolder), ScanImportProcessorConfig.createDefault(),
        AllSpectralDataImportModule.class, parameters, java.time.Instant.now(), null);
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    assertEquals(1, project.getNumberOfDataFiles());
    assertTrue(project.getDataFiles()[0].getNumOfScans() > 100);
    assertTrue(project.getDataFiles()[0].getDataRTRange().upperEndpoint()
        > project.getDataFiles()[0].getDataRTRange().lowerEndpoint());
  }

  private static void writeSyntheticDataMs(Path path) throws IOException {
    Files.createFile(path);
    try (RandomAccessFile output = new RandomAccessFile(path.toFile(), "rw")) {
      output.setLength(0x300);
      output.seek(0);
      output.writeInt(0x01320000);
      output.seek(0x4);
      writeGapString(output, "MSD Spectral File");
      output.seek(0x116);
      output.writeInt(2);

      final int dataStart = 0x180;
      output.seek(0x10A);
      output.writeShort((dataStart + 2) / 2);
      output.seek(dataStart);
      writeScan(output, 60_000, new int[][]{{1_000, 10}, {1_020, 0x4003}});
      // Both encoded m/z values round to nominal mass 50 and must be combined.
      writeScan(output, 120_000, new int[][]{{1_000, 5}, {1_001, 7}});
    }
  }

  private static void writeGapString(RandomAccessFile output, String value) throws IOException {
    output.writeByte(value.length());
    output.writeBytes(value);
  }

  private static void writeScan(RandomAccessFile output, int timeMillis, int[][] pairs)
      throws IOException {
    output.writeShort(0);
    output.writeInt(timeMillis);
    output.write(new byte[6]);
    output.writeShort(pairs.length);
    output.write(new byte[4]);
    for (int[] pair : pairs) {
      output.writeShort(pair[0]);
      output.writeShort(pair[1]);
    }
    output.write(new byte[10]);
  }
}
