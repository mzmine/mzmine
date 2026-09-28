package io.github.mzmine.modules.visualization.acquisitionmetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.AcquisitionMetadata.Field;
import io.github.mzmine.datamodel.AcquisitionMetadata.Term;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testutils.MZmineTestUtil;

class AcquisitionMetadataProjectIOTest {

  @TempDir
  Path tempDir;

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @AfterEach
  void resetProject() {
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
  }

  @Test
  void persistsFileHeaderDeclarationsSeparatelyFromStudyMetadata() throws Exception {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    final RawDataFileImpl file = new RawDataFileImpl("sample.mzML", "/data/sample.mzML", null);
    file.setAcquisitionMetadata(new AcquisitionMetadata(List.of(
        new Term(Field.INSTRUMENT_MODEL, "MS:1001911", "Q Exactive")),
        Map.of("MethodName", "batch-7", "InstrumentName", "QE")));
    project.addFile(file);

    final Path archive = tempDir.resolve("acquisition-roundtrip.mzmine");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      assertTrue(AcquisitionMetadataProjectIO.saveToZip(output));
    }

    file.setAcquisitionMetadata(AcquisitionMetadata.EMPTY);
    try (ZipFile zip = new ZipFile(archive.toFile())) {
      assertTrue(AcquisitionMetadataProjectIO.loadFromZip(zip));
    }

    assertEquals("Q Exactive", file.getAcquisitionMetadata().terms().getFirst().label());
    assertEquals("batch-7", file.getAcquisitionMetadata().localFields().get("MethodName"));
    assertTrue(project.getProjectMetadata().getColumns().stream()
        .noneMatch(column -> column.getTitle().equals("MethodName")));
  }

  @Test
  void distinguishesFilesWithTheSameBasenameByTheirImportSource() throws Exception {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    final RawDataFileImpl first = raw("sample.mzML", "/first/sample.mzML", "Q Exactive");
    final RawDataFileImpl second = raw("sample.mzML", "/second/sample.mzML", "LTQ Orbitrap");
    project.addFile(first);
    project.addFile(second);
    final Path archive = tempDir.resolve("same-name.mzmine");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      assertTrue(AcquisitionMetadataProjectIO.saveToZip(output));
    }
    first.setAcquisitionMetadata(AcquisitionMetadata.EMPTY);
    second.setAcquisitionMetadata(AcquisitionMetadata.EMPTY);
    try (ZipFile zip = new ZipFile(archive.toFile())) {
      AcquisitionMetadataProjectIO.loadFromZip(zip);
    }
    assertEquals("Q Exactive", first.getAcquisitionMetadata().terms().getFirst().label());
    assertEquals("LTQ Orbitrap", second.getAcquisitionMetadata().terms().getFirst().label());
  }

  @Test
  void restoresEmbeddedSourcesThroughTheExplicitProjectLoadMapping() throws Exception {
    final MZmineProjectImpl savedProject = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(savedProject);
    final RawDataFileImpl saved = raw("sample.mzML", "/source/sample.mzML", "Q Exactive");
    savedProject.addFile(saved);
    final Path archive = tempDir.resolve("portable.mzmine");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      assertTrue(AcquisitionMetadataProjectIO.saveToZip(output, true));
    }

    final MZmineProjectImpl openedProject = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(openedProject);
    final RawDataFileImpl extracted = new RawDataFileImpl("sample.mzML", "/temporary/sample.mzML", null);
    openedProject.addFile(extracted);
    try (ZipFile zip = new ZipFile(archive.toFile())) {
      AcquisitionMetadataProjectIO.loadFromZip(zip,
          Map.of("$$msdatafiles/sample.mzML$$", "/temporary/sample.mzML"));
    }
    assertEquals("Q Exactive", extracted.getAcquisitionMetadata().terms().getFirst().label());
  }

  @Test
  void skipsMissingFilesAndRejectsMalformedRecords() throws Exception {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    final RawDataFileImpl saved = raw("sample.mzML", "/source/sample.mzML", "Q Exactive");
    project.addFile(saved);
    final Path archive = tempDir.resolve("missing-file.mzmine");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      AcquisitionMetadataProjectIO.saveToZip(output);
    }
    final MZmineProjectImpl openedProject = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(openedProject);
    final RawDataFileImpl other = new RawDataFileImpl("other.mzML", "/other/other.mzML", null);
    openedProject.addFile(other);
    try (ZipFile zip = new ZipFile(archive.toFile())) {
      assertTrue(AcquisitionMetadataProjectIO.loadFromZip(zip));
    }
    assertEquals(AcquisitionMetadata.EMPTY, other.getAcquisitionMetadata());

    final Path malformed = tempDir.resolve("malformed.mzmine");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(malformed))) {
      output.putNextEntry(new java.util.zip.ZipEntry(
          AcquisitionMetadataProjectIO.ACQUISITION_METADATA_FILENAME));
      output.write("<acquisition-metadata version=\"99\"/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      output.closeEntry();
    }
    try (ZipFile zip = new ZipFile(malformed.toFile())) {
      assertThrows(java.io.IOException.class, () -> AcquisitionMetadataProjectIO.loadFromZip(zip));
    }
    assertFalse(other.getAcquisitionMetadata().terms().stream()
        .anyMatch(term -> term.label().equals("Q Exactive")));

    final Path duplicate = tempDir.resolve("duplicate.mzmine");
    final String source = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
        "/other/other.mzML".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    final String name = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
        "other.mzML".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(duplicate))) {
      output.putNextEntry(new java.util.zip.ZipEntry(
          AcquisitionMetadataProjectIO.ACQUISITION_METADATA_FILENAME));
      output.write(("<acquisition-metadata version=\"1\"><raw-file source=\"" + source
          + "\" name=\"" + name + "\"><header-field name=\"TWV0aG9k\" value=\"dmFsdWU\"/>"
          + "</raw-file><raw-file source=\"" + source + "\" name=\"" + name
          + "\"/></acquisition-metadata>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
      output.closeEntry();
    }
    try (ZipFile zip = new ZipFile(duplicate.toFile())) {
      assertThrows(java.io.IOException.class, () -> AcquisitionMetadataProjectIO.loadFromZip(zip));
    }
    assertEquals(AcquisitionMetadata.EMPTY, other.getAcquisitionMetadata());
  }

  @Test
  void writerRejectsValuesAndOutputBeyondTheLoaderBounds() throws Exception {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    final RawDataFileImpl file = new RawDataFileImpl("large.mzML", "/data/large.mzML", null);
    file.setAcquisitionMetadata(new AcquisitionMetadata(List.of(), Map.of("large",
        "x".repeat(65_537))));
    project.addFile(file);
    final Path archive = tempDir.resolve("large.mzmine");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      assertThrows(java.io.IOException.class, () -> AcquisitionMetadataProjectIO.saveToZip(output));
    }
    try (ZipFile zip = new ZipFile(archive.toFile())) {
      assertEquals(null, zip.getEntry(AcquisitionMetadataProjectIO.ACQUISITION_METADATA_FILENAME));
    }

    final AcquisitionMetadataProjectIO.BoundedOutputStream output =
        new AcquisitionMetadataProjectIO.BoundedOutputStream(2);
    output.write(new byte[]{1, 2});
    assertThrows(java.io.IOException.class, () -> output.write(3));
  }

  private static RawDataFileImpl raw(final String name, final String path, final String model) {
    final RawDataFileImpl file = new RawDataFileImpl(name, path, null);
    file.setAcquisitionMetadata(new AcquisitionMetadata(AcquisitionMetadata.resolveLabel(
        Field.INSTRUMENT_MODEL, model), Map.of("MethodName", "test")));
    return file;
  }
}
