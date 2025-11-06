package resolver_tests;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import testutils.MZmineTestUtil;
import testutils.TaskResult;

public class ResolverTests {

  static final MemoryMapStorage storage = MemoryMapStorage.forMassList();
  static final List<FileToImport> filesToImport = List.of();

  record Peak(float topRt, @Nullable Range<Float> rtRange, @Nullable String desc) {

    public Peak(float topRt) {
      this(topRt, null, null);
    }

    static Peak top(float topRt) {
      return new Peak(topRt);
    }
  }

  @BeforeAll
  static void importFiles() throws InterruptedException {
    MZmineTestUtil.clearProjectAndLibraries();
    final Map<AdvancedSpectraImportParameters, List<FileToImport>> filesByImportParam = filesToImport.stream()
        .collect(Collectors.groupingBy(FileToImport::importParam));

    for (final var paramToFiles : filesByImportParam.entrySet()) {
      final TaskResult _ = MZmineTestUtil.importFiles(
          paramToFiles.getValue().stream().map(FileToImport::filePath).distinct().toList(), 10_000,
          paramToFiles.getKey());
    }
  }
}
