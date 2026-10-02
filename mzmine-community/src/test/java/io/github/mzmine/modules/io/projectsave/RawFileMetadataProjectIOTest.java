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

package io.github.mzmine.modules.io.projectsave;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentModelType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentSerialNumberType;
import io.github.mzmine.datamodel.features.rawfiletypes.MassAnalyzersType;
import io.github.mzmine.datamodel.features.types.RawFileType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RawFileMetadataProjectIOTest {

  @TempDir
  File tempDir;

  private static @NotNull RawDataFile createFile(@NotNull final String name) {
    return new RawDataFileImpl(name, null, null, Color.BLACK);
  }

  @Test
  void saveAndLoadRoundTrip() throws Exception {
    final MZmineProjectImpl savedProject = new MZmineProjectImpl();
    final RawDataFile a = createFile("a.mzML");
    a.getFileMetadata().set(InstrumentModelType.class, "Q Exactive");
    a.getFileMetadata().set(InstrumentSerialNumberType.class, "Exactive Series slot #1331");
    a.getFileMetadata().set(MassAnalyzersType.class, List.of("quadrupole", "time-of-flight"));
    // general context-free types can be used without any raw file specific tagging
    a.getFileMetadata().set(MZType.class, 524.3718);
    // file without metadata is not written
    final RawDataFile b = createFile("b.mzML");
    savedProject.addFile(a);
    savedProject.addFile(b);

    final File zip = new File(tempDir, "project.mzmine");
    try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zip))) {
      Assertions.assertTrue(RawFileMetadataProjectIO.saveToZip(zipStream, savedProject));
    }

    // re-imported files in a new project, with a value from import that is replaced by the saved
    final MZmineProjectImpl loadedProject = new MZmineProjectImpl();
    final RawDataFile loadedA = createFile("a.mzML");
    loadedA.getFileMetadata().set(InstrumentModelType.class, "from import");
    final RawDataFile loadedB = createFile("b.mzML");
    loadedProject.addFile(loadedA);
    loadedProject.addFile(loadedB);

    try (ZipFile zipFile = new ZipFile(zip)) {
      Assertions.assertTrue(RawFileMetadataProjectIO.loadFromZip(zipFile, loadedProject));
    }

    Assertions.assertEquals("Q Exactive", loadedA.getFileMetadata().get(InstrumentModelType.class));
    Assertions.assertEquals("Exactive Series slot #1331",
        loadedA.getFileMetadata().get(InstrumentSerialNumberType.class));
    Assertions.assertEquals(List.of("quadrupole", "time-of-flight"),
        loadedA.getFileMetadata().get(MassAnalyzersType.class));
    Assertions.assertEquals(524.3718, loadedA.getFileMetadata().get(MZType.class));
    Assertions.assertTrue(loadedB.getFileMetadata().isEmpty());
  }

  @Test
  void contextTypeFailsSave() throws Exception {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    final RawDataFile a = createFile("a.mzML");
    a.getFileMetadata().set(InstrumentModelType.class, "Q Exactive");
    // requires a feature list context and must never be stored as raw file metadata
    a.getFileMetadata().set(RawFileType.class, a);
    project.addFile(a);

    final File zip = new File(tempDir, "context.mzmine");
    try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zip))) {
      final IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
          () -> RawFileMetadataProjectIO.saveToZip(zipStream, project));
      Assertions.assertTrue(e.getMessage().contains(RawFileType.class.getName()), e.getMessage());
      // zip needs at least one entry to be valid
      zipStream.putNextEntry(new ZipEntry("dummy"));
      zipStream.closeEntry();
    }
    // checked before writing, the archive has no partial metadata entry
    try (ZipFile zipFile = new ZipFile(zip)) {
      Assertions.assertNull(zipFile.getEntry(RawFileMetadataProjectIO.RAW_FILE_METADATA_FILENAME));
    }
  }

  @Test
  void nothingToSave() throws Exception {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFile(createFile("a.mzML"));

    final File zip = new File(tempDir, "empty.mzmine");
    try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zip))) {
      Assertions.assertFalse(RawFileMetadataProjectIO.saveToZip(zipStream, project));
      // zip needs at least one entry to be valid
      zipStream.putNextEntry(new ZipEntry("dummy"));
      zipStream.closeEntry();
    }
    try (ZipFile zipFile = new ZipFile(zip)) {
      Assertions.assertFalse(RawFileMetadataProjectIO.loadFromZip(zipFile, project));
    }
  }
}
