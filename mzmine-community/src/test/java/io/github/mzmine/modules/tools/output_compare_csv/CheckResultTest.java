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