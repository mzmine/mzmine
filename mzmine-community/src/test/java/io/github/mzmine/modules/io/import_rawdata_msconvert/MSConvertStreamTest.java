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

package io.github.mzmine.modules.io.import_rawdata_msconvert;

import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

class MSConvertStreamTest {

  @Test
  void usesRetainedConversionAndPublishesItOnlyOnce(@TempDir @NotNull final Path directory)
      throws IOException {
    final File source = directory.resolve("retained.raw").toFile();
    final Path converted = directory.resolve("retained.mzML");
    try (final var fixture = MSConvertStreamTest.class.getResourceAsStream(
        "/rawdatafiles/additional/no_scan_window_limits.mzML")) {
      Assertions.assertNotNull(fixture);
      Files.copy(fixture, converted);
    }
    final var project = new MZmineProjectImpl();
    for (int i = 0; i < 2; i++) {
      final var parameters = new AllSpectralDataImportParameters().cloneParameterSet();
      parameters.setParameter(AllSpectralDataImportParameters.fileNames, new File[]{source});
      final var task = new MSConvertImportTask(null, Instant.EPOCH, source,
          ScanImportProcessorConfig.createDefault(), project, AllSpectralDataImportModule.class,
          parameters);
      task.run();
      Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus(), task::getErrorMessage);
      Assertions.assertEquals(1, project.getCurrentRawDataFiles().size());
      final var file = project.getCurrentRawDataFiles().getFirst();
      Assertions.assertEquals(converted.toString(), file.getAbsolutePath());
      Assertions.assertEquals(1, file.getAppliedMethods().size());
      Assertions.assertEquals(i == 0 ? List.of(file) : List.of(), task.getImportedRawDataFiles());
    }
  }

  @Test
  void preservesXmlBytesAfterMultibytePreambleAndSplitDeclaration() throws IOException {
    final String xml = "<?xml version=\"1.0\"?><mzML><run id=\"측정\"/></mzML>";
    final String output = "測".repeat(85) + xml;
    try (var stream = MSConvertImportTask.mzMLStream(
        new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)))) {
      Assertions.assertArrayEquals(xml.getBytes(StandardCharsets.UTF_8), stream.readAllBytes());
    }
  }

  @Test
  void failsAtEndOfConverterOutputWithoutXml() {
    Assertions.assertThrows(IOException.class, () -> MSConvertImportTask.mzMLStream(
        new ByteArrayInputStream("Conversion failed".getBytes(StandardCharsets.UTF_8))));
  }
}
