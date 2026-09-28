package io.github.mzmine.modules.visualization.acquisitionmetadata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.AcquisitionMetadata.Field;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegacyAcquisitionMetadataCleanupTest {

  @Test
  void removesOnlyCompleteUneditedLegacyDuplicates() {
    final RawDataFileImpl first = raw("first", "method-A", "batch-A");
    final RawDataFileImpl second = raw("second", "method-B", "batch-B");
    final MetadataTable table = new MetadataTable(false);

    final StringMetadataColumn generated = new StringMetadataColumn("Imported: MethodName",
        "Declared source metadata; may contain sample/study information");
    table.setValue(generated, first, "method-A");
    table.setValue(generated, second, "method-B");

    final StringMetadataColumn edited = new StringMetadataColumn("Imported: Batch",
        "Declared source metadata; may contain sample/study information");
    table.setValue(edited, first, "batch-A");
    table.setValue(edited, second, "reviewed-batch");

    final StringMetadataColumn similarlyNamedUserColumn = new StringMetadataColumn("Imported: Notes",
        "User supplied notes");
    table.setValue(similarlyNamedUserColumn, first, "keep");
    table.setValue(similarlyNamedUserColumn, second, "keep");

    final StringMetadataColumn partial = new StringMetadataColumn("Measured: scan count",
        "Number of imported scans");
    table.setValue(partial, first, "0");

    final StringMetadataColumn unavailable = new StringMetadataColumn("Measured: RT range (min)",
        "Derived from imported scans");
    table.setValue(unavailable, first, "[0.0..1.0]");
    table.setValue(unavailable, second, "[0.0..1.0]");

    LegacyAcquisitionMetadataCleanup.cleanup(table, List.of(first, second));

    assertFalse(table.getColumns().contains(generated));
    assertNotNull(table.getColumnByName("Imported: Batch"));
    assertNotNull(table.getColumnByName("Imported: Notes"));
    assertNotNull(table.getColumnByName("Measured: scan count"));
    assertNotNull(table.getColumnByName("Measured: RT range (min)"));
  }

  private static RawDataFileImpl raw(final String name, final String method, final String batch) {
    final RawDataFileImpl file = new RawDataFileImpl(name, "/data/" + name + ".mzML", null);
    file.setAcquisitionMetadata(new AcquisitionMetadata(AcquisitionMetadata.resolveLabel(
        Field.INSTRUMENT_MODEL, "Q Exactive"), Map.of("MethodName", method, "Batch", batch)));
    return file;
  }
}
