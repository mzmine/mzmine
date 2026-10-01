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

package io.github.mzmine.modules.batchmode.timing;

import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.ChromatogramBuilderSensitivity;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderParameters;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.io.CsvWriter;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StepAlgorithmsTest {

  private static final RawDataFilesSelection FILES = new RawDataFilesSelection(
      RawDataFilesSelectionType.BATCH_LAST_FILES);

  @Test
  void describesTheSelectedAlgorithm() {
    Assertions.assertEquals("Fast (auto)", StepAlgorithms.describe(
        ADAPChromatogramBuilderParameters.createFastAuto(FILES, new ScanSelection(1),
            ChromatogramBuilderSensitivity.MEDIUM, "eics", true)));
    Assertions.assertEquals("mzmine <4.11", StepAlgorithms.describe(
        ADAPChromatogramBuilderParameters.createLegacy(FILES, new ScanSelection(1), 4,
            new MZTolerance(0.002, 10), "eics", 1E3, 1E4, true)));
  }

  @Test
  void modulesWithoutChoiceHaveNoAlgorithm() {
    Assertions.assertNull(StepAlgorithms.describe(new ImageBuilderParameters()));
    Assertions.assertNull(StepAlgorithms.describe(null));
  }

  @Test
  void measurementsHaveAnAlgorithmColumn() throws Exception {
    final StepStorageMeasurement storage = new StepStorageMeasurement(1, "Chromatogram builder", 0,
        0, 0, 0, 0);
    final List<StepMeasurement> measurements = List.of(new StepMeasurement(
        new StepTimeMeasurement(1, "Chromatogram builder", "Fast (auto)", Duration.ofSeconds(2),
            false), storage), new StepMeasurement(
        new StepTimeMeasurement(2, "Image builder", Duration.ofSeconds(1), false), storage));
    final String csv = CsvWriter.writeToString(measurements, StepMeasurement.class, '\t', true);
    final String[] lines = csv.split("\\R");
    Assertions.assertTrue(lines[0].startsWith("step\tname\talgorithm\t"), lines[0]);
    // text with spaces is quoted
    Assertions.assertTrue(lines[1].startsWith("1\t\"Chromatogram builder\"\t\"Fast (auto)\"\t"),
        lines[1]);
    Assertions.assertTrue(lines[2].startsWith("2\t\"Image builder\"\t\t"), lines[2]);
    Assertions.assertTrue(measurements.getFirst().toString()
        .startsWith("Step 1: Chromatogram builder (Fast (auto)) took"));
  }
}
