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

package io.github.mzmine.modules.tools.output_compare_csv;

import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import io.github.mzmine.util.io.CsvReader;
import io.github.mzmine.util.io.CsvWriter;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckResultTest {

  @Test
  public void writeReadFile(@TempDir Path tempDir) throws IOException {
    final List<CheckResult> checks = List.of(
        CheckResult.create("Test with simple", Severity.INFO, "Simple creation"),
        CheckResult.create("Test type", Severity.ERROR, DataTypes.get(MZType.class), 200.0, 50.0,
            "With data types and values"),
        CheckResult.create("Test type", Severity.ERROR, DataTypes.get(IDType.class), 1,
            "5 as string", "With data types and values of different types"));

    final File file = tempDir.resolve("test.csv").toFile();
    CsvWriter.writeToFile(file, checks, CheckResult.class, WriterOptions.REPLACE);
    final List<CheckResult> loadedChecks = CsvReader.readToList(file, CheckResult.class);

    assertEquals(checks.size(), loadedChecks.size());
  }

}