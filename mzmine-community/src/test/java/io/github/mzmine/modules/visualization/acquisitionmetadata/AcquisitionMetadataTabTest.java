package io.github.mzmine.modules.visualization.acquisitionmetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.AcquisitionMetadata.Field;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

class AcquisitionMetadataTabTest {

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @AfterEach
  void resetProject() {
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
  }

  @Test
  void labelsFileHeadersAndDerivedFactsWithoutStudyMetadataWrites() {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    final RawDataFileImpl file = new RawDataFileImpl("sample.mzML", "/data/sample.mzML", null);
    file.setAcquisitionMetadata(new AcquisitionMetadata(AcquisitionMetadata.resolveLabel(
        Field.INSTRUMENT_MODEL, "Q Exactive"), Map.of("MethodName", "method-A")));
    project.addFile(file);

    final var rows = AcquisitionMetadataTab.rows();
    assertTrue(rows.stream().anyMatch(row -> row.field().equals("Instrument model")
        && row.source().equals("File header") && row.value().contains("Q Exactive")));
    assertTrue(rows.stream().anyMatch(row -> row.field().equals("Analyzer")
        && row.value().equals("Unknown") && row.source().equals("Not declared in file header")));
    assertTrue(rows.stream().anyMatch(row -> row.field().equals("Scan count")
        && row.source().equals("Derived from loaded scans") && row.unit().equals("scans")));
    assertEquals("method-A", file.getAcquisitionMetadata().localFields().get("MethodName"));
    assertTrue(project.getProjectMetadata().getColumns().stream()
        .noneMatch(column -> column.getTitle().equals("MethodName")));
  }
}
