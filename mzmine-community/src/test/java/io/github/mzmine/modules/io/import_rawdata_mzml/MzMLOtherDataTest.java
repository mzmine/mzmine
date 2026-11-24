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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import io.github.mzmine.datamodel.otherdetectors.ChromatogramType;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MzMLOtherDataTest {

  /**
   * Checks both Agilent UV channels in the MSConvert chromatogram-only fixture.
   */
  @Test
  void importsAgilentUvTraces() throws URISyntaxException {
    final var fixture = MzMLOtherDataTest.class.getResource(
        "/rawdatafiles/additional/agilent_uv_traces.mzML");
    Assertions.assertNotNull(fixture);
    final var project = new MZmineProjectImpl();
    final var parameters = new MSDKmzMLImportParameters().cloneParameterSet();
    final var task = new MzMLImportTask(project, new File(fixture.toURI()),
        ScanImportProcessorConfig.createDefault(), MSDKmzMLImportModule.class, parameters,
        Instant.EPOCH, null);
    task.run();

    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus(), task::getErrorMessage);
    Assertions.assertEquals(1, task.getImportedRawDataFiles().size());
    final var file = task.getImportedRawDataFiles().getFirst();
    Assertions.assertEquals(0, file.getNumOfScans());
    Assertions.assertEquals(1, file.getOtherDataFiles().size());
    final var series = file.getOtherDataFiles().getFirst().getOtherTimeSeriesData();
    Assertions.assertNotNull(series);
    Assertions.assertEquals(ChromatogramType.ABSORPTION, series.getChromatogramType());
    Assertions.assertEquals("Absorbance", series.getTimeSeriesRangeLabel());

    final var traces = series.getRawTraces();
    Assertions.assertEquals(
        List.of("DAD1 A: Sig=220.0,4.0  Ref=off", "DAD1 B: Sig=254.0,4.0  Ref=off"),
        traces.stream().map(trace -> trace.getFeatureData().getName()).toList());
    final var first = traces.getFirst().getFeatureData();
    final var second = traces.getLast().getFeatureData();
    final int sample = 14_999;
    for (final var trace : List.of(first, second)) {
      Assertions.assertEquals(29_999, trace.getNumberOfValues());
      Assertions.assertEquals(12.4997917f, trace.getRetentionTime(sample), 0.000001f);
    }
    Assertions.assertEquals(39.7253647, first.getIntensity(sample), 0.000001);
    Assertions.assertEquals(1.57006085, second.getIntensity(sample), 0.000001);
  }
}
