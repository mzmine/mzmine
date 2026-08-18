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

package io.github.mzmine.modules.io.import_rawdata_hapsite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_hapsite.HapsiteHpsParser.HapsiteScan;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HapsiteHpsParserTest {

  @TempDir
  Path temporaryDirectory;

  @Test
  void detectsAndDecodesNativeHapsScanLayout() throws IOException {
    final File file = writeSyntheticHapsScan("native.hps");
    assertEquals(RawDataFileType.INFICON_HAPSITE,
        RawDataFileTypeDetector.detectDataFileType(file));

    final HapsiteHpsParser parser = new HapsiteHpsParser(file);
    assertEquals(10, parser.getTotalScans());
    final HapsiteScan first = parser.readNextScan();
    assertNotNull(first);
    assertEquals(1, first.scanNumber());
    assertEquals(1f / 60f, first.retentionTime(), 1e-7f);
    assertArrayEquals(new double[]{45d, 48d}, first.mzValues());
    assertArrayEquals(new double[]{100d, 5d}, first.intensityValues());
    for (int scan = 1; scan < 10; scan++) {
      assertNotNull(parser.readNextScan());
    }
    assertNull(parser.readNextScan());
    assertEquals(1d, parser.getFinishedPercentage());
  }

  @Test
  void decodesFullScanGpirLayout() throws IOException {
    final File file = writeSyntheticFullScan("full.hps");
    final HapsiteHpsParser parser = new HapsiteHpsParser(file);
    assertEquals(10, parser.getTotalScans());
    final HapsiteScan first = parser.readNextScan();
    assertNotNull(first);
    assertArrayEquals(new double[]{42d, 45d}, first.mzValues());
    assertArrayEquals(new double[]{200d, 7d}, first.intensityValues());
    assertEquals((31d / 30d + 148.3666666667d) / 60d, first.retentionTime(), 1e-6);
  }

  @Test
  void singleReportPeakKeepsLayoutTimeCalibration() throws IOException {
    // One matched report line cannot define a slope. The parser must keep the layout's calibration
    // instead of silently falling back to an uncalibrated 1 s/scan fit.
    final File file = writeSyntheticFullScan("one-report-peak.hps",
        "    1    0m30s    100    1.0    2.0\n");
    final HapsiteHpsParser parser = new HapsiteHpsParser(file);
    final HapsiteScan first = parser.readNextScan();
    assertNotNull(first);
    assertEquals((31d / 30d + 148.3666666667d) / 60d, first.retentionTime(), 1e-6);
  }

  @Test
  void twoReportPeaksOverrideLayoutTimeCalibration() throws IOException {
    // Scan 1 -> 30 s and scan 11 -> 70 s gives 4 s/scan with a 26 s intercept. The layout's
    // fallback must lose to a report fit that actually has enough points.
    final File file = writeSyntheticFullScan("two-report-peaks.hps",
        "    1    0m30s    100    1.0    2.0\n   11    1m10s    100    1.0    2.0\n");
    final HapsiteHpsParser parser = new HapsiteHpsParser(file);
    final HapsiteScan first = parser.readNextScan();
    assertNotNull(first);
    assertEquals(30d / 60d, first.retentionTime(), 1e-6);
  }

  @Test
  void importsHapsiteFileIntoProject() throws IOException {
    final File file = writeSyntheticHapsScan("import.hps");
    final MZmineProjectImpl project = new MZmineProjectImpl();
    final SimpleParameterSet parameters = new SimpleParameterSet(new Parameter<?>[0]);
    final HapsiteImportTask task = new HapsiteImportTask(project, file,
        ScanImportProcessorConfig.createDefault(), AllSpectralDataImportModule.class, parameters,
        Instant.now(), null);
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    assertEquals(1, project.getNumberOfDataFiles());
    assertEquals(10, project.getDataFiles()[0].getNumOfScans());
    assertEquals(100d, project.getDataFiles()[0].getScan(0).getBasePeakIntensity());
  }

  @Test
  void decodesAllConfiguredRealHapsiteLayouts() throws IOException {
    final String configuredRoot = System.getenv("HAPSITE_TEST_ROOT");
    if (configuredRoot == null || configuredRoot.isBlank()) {
      return;
    }

    final Map<String, Integer> representativeCounts = new HashMap<>();
    representativeCounts.put("SmallVOCtest2_20260528_001.hps", 585); // HapsScan
    representativeCounts.put("SmallVOCtest2_Propanal_Isop_MVK_1ppm.hps", 585); // full GPIR
    representativeCounts.put("ER Survey_20260527_001.hps", 255); // older GPIR

    int testedFiles = 0;
    try (var paths = Files.walk(Path.of(configuredRoot))) {
      for (Path path : paths.filter(Files::isRegularFile)
          .filter(candidate -> candidate.getFileName().toString().toLowerCase().endsWith(".hps"))
          .toList()) {
        final HapsiteHpsParser parser = new HapsiteHpsParser(path.toFile());
        assertTrue(parser.getTotalScans() >= 10, path.toString());
        final Integer expectedCount = representativeCounts.get(path.getFileName().toString());
        if (expectedCount != null) {
          assertEquals(expectedCount, parser.getTotalScans(), path.toString());
        }

        float previousRt = -1f;
        int scans = 0;
        int nonEmptyScans = 0;
        HapsiteScan spectrum;
        while ((spectrum = parser.readNextScan()) != null) {
          if (scans == 0) {
            assertFirstSpectrumMatchesAtmos(path.getFileName().toString(), spectrum);
          }
          assertTrue(spectrum.retentionTime() >= previousRt, path.toString());
          assertEquals(spectrum.mzValues().length, spectrum.intensityValues().length);
          if (spectrum.mzValues().length > 0) {
            nonEmptyScans++;
          }
          previousRt = spectrum.retentionTime();
          scans++;
        }
        assertEquals(parser.getTotalScans(), scans, path.toString());
        assertTrue(nonEmptyScans > 0, path.toString());
        testedFiles++;
      }
    }
    assertTrue(testedFiles >= representativeCounts.size());
  }

  @Test
  void importsConfiguredRealHapsiteFileWhenAvailable() throws IOException {
    final String configuredRoot = System.getenv("HAPSITE_TEST_ROOT");
    if (configuredRoot == null || configuredRoot.isBlank()) {
      return;
    }
    final Path representative;
    try (var paths = Files.walk(Path.of(configuredRoot))) {
      representative = paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString()
              .equals("SmallVOCtest2_20260528_001.hps"))
          .findFirst().orElse(null);
    }
    if (representative == null) {
      return;
    }

    final MZmineProjectImpl project = new MZmineProjectImpl();
    final SimpleParameterSet parameters = new SimpleParameterSet(new Parameter<?>[0]);
    final HapsiteImportTask task = new HapsiteImportTask(project, representative.toFile(),
        ScanImportProcessorConfig.createDefault(), AllSpectralDataImportModule.class, parameters,
        Instant.now(), null);
    task.run();
    assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    assertEquals(585, project.getDataFiles()[0].getNumOfScans());
    assertEquals(0.268183148094, project.getDataFiles()[0].getScan(0).getRetentionTime(), 1e-6);
  }

  private static void assertFirstSpectrumMatchesAtmos(String fileName, HapsiteScan spectrum) {
    final double total = java.util.Arrays.stream(spectrum.intensityValues()).sum();
    final double basePeak = java.util.Arrays.stream(spectrum.intensityValues()).max().orElse(0d);
    switch (fileName) {
      case "SmallVOCtest2_20260528_001.hps" -> {
        assertEquals(0.268183148094, spectrum.retentionTime(), 1e-6);
        assertEquals(147, spectrum.mzValues().length);
        assertEquals(175851d, total, 1e-6);
        assertEquals(9288d, basePeak, 1e-6);
        assertEquals(45d, spectrum.mzValues()[0]);
        assertEquals(304d, spectrum.mzValues()[spectrum.mzValues().length - 1]);
      }
      case "SmallVOCtest2_Propanal_Isop_MVK_1ppm.hps" -> {
        assertEquals(0.268183148094, spectrum.retentionTime(), 1e-6);
        assertEquals(147, spectrum.mzValues().length);
        assertEquals(175851d, total, 1e-6);
        assertEquals(9288d, basePeak, 1e-6);
        assertEquals(42d, spectrum.mzValues()[0]);
        assertEquals(301d, spectrum.mzValues()[spectrum.mzValues().length - 1]);
      }
      case "ER Survey_20260527_001.hps" -> {
        assertEquals(0d, spectrum.retentionTime(), 1e-6);
        assertEquals(112, spectrum.mzValues().length);
        assertEquals(22.03755307d, total, 1e-5);
        assertEquals(2.00424719d, basePeak, 1e-6);
        assertEquals(23d, spectrum.mzValues()[0]);
        assertEquals(275d, spectrum.mzValues()[spectrum.mzValues().length - 1]);
      }
      default -> {
      }
    }
  }

  private File writeSyntheticHapsScan(String name) throws IOException {
    final int gpirOffset = 100;
    final int scanOffset = 300;
    final int dataOffset = scanOffset + 84;
    final int infoOffset = dataOffset + 10 * 1040;
    final byte[] bytes = new byte[infoOffset + 32];
    putAscii(bytes, 4, "SPAH");
    putAscii(bytes, gpirOffset, "HapsGPIR");
    putLittleEndianInt(bytes, gpirOffset + 176, infoOffset - (gpirOffset + 256));
    putAscii(bytes, scanOffset, "HapsScan");
    putAscii(bytes, infoOffset, "HapsInfo");
    for (int scan = 0; scan < 10; scan++) {
      final int base = dataOffset + scan * 1040 - 4;
      putLittleEndianFloat(bytes, base, 100f + scan);
      putLittleEndianFloat(bytes, base + 4, -4f);
      putLittleEndianFloat(bytes, base + 8, Float.NaN);
      putLittleEndianFloat(bytes, base + 12, 5f);
    }
    final Path path = temporaryDirectory.resolve(name);
    Files.write(path, bytes);
    return path.toFile();
  }

  private File writeSyntheticFullScan(String name) throws IOException {
    return writeSyntheticFullScan(name, "");
  }

  private File writeSyntheticFullScan(String name, String reportText) throws IOException {
    final int gpirOffset = 100;
    final int dataOffset = gpirOffset + 256;
    final int availableLength = 10 * 1040;
    final int infoOffset = dataOffset + availableLength;
    final byte[] reportBytes = reportText.getBytes(StandardCharsets.ISO_8859_1);
    final byte[] bytes = new byte[infoOffset + 32 + reportBytes.length];
    System.arraycopy(reportBytes, 0, bytes, infoOffset + 32, reportBytes.length);
    putAscii(bytes, 4, "SPAH");
    putAscii(bytes, gpirOffset, "HapsGPIR");
    putLittleEndianInt(bytes, gpirOffset + 176, availableLength);
    putAscii(bytes, infoOffset, "HapsInfo");
    for (int scan = 0; scan < 10; scan++) {
      final int base = dataOffset + scan * 1040;
      putLittleEndianFloat(bytes, base, 200f + scan);
      putLittleEndianFloat(bytes, base + 12, 7f);
    }
    final Path path = temporaryDirectory.resolve(name);
    Files.write(path, bytes);
    return path.toFile();
  }

  private static void putAscii(byte[] target, int offset, String value) {
    final byte[] encoded = value.getBytes(StandardCharsets.US_ASCII);
    System.arraycopy(encoded, 0, target, offset, encoded.length);
  }

  private static void putLittleEndianInt(byte[] target, int offset, int value) {
    ByteBuffer.wrap(target, offset, Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value);
  }

  private static void putLittleEndianFloat(byte[] target, int offset, float value) {
    ByteBuffer.wrap(target, offset, Float.BYTES).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
  }
}
