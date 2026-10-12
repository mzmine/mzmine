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

package io.github.mzmine.modules.visualization.projectmetadata.io;

import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportParameters.MetadataFileFormat;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;

/** Saves and restores the current project metadata snapshot in an MZmine project archive. */
public final class ProjectMetadataProjectIO {

  public static final String PROJECT_METADATA_FILENAME = "project_metadata.tsv";
  private static final String TEMP_FILE_PREFIX = "mzmine_project_metadata";
  private static final Logger logger = Logger.getLogger(ProjectMetadataProjectIO.class.getName());

  private ProjectMetadataProjectIO() {
  }

  /**
   * @return {@code true} if metadata was written, or {@code false} for an empty metadata table
   */
  public static boolean saveToZip(@NotNull final ZipOutputStream zipStream) throws IOException {
    final MetadataTable metadata = ProjectService.getMetadata();
    if (metadata.getColumns().isEmpty()) {
      logger.info("Project metadata is empty, nothing to save");
      return false;
    }

    File tempFile = null;
    try {
      tempFile = FileAndPathUtil.createTempFile(TEMP_FILE_PREFIX, ".tsv");
      final ProjectMetadataWriter writer = new ProjectMetadataWriter(metadata,
          MetadataFileFormat.MZMINE_INTERNAL);
      if (!writer.exportTo(tempFile)) {
        throw new IOException("Could not export project metadata for project saving.");
      }

      zipStream.putNextEntry(new ZipEntry(PROJECT_METADATA_FILENAME));
      Files.copy(tempFile.toPath(), zipStream);
      zipStream.closeEntry();
      return true;
    } finally {
      deleteTempFile(tempFile);
    }
  }

  /**
   * @return {@code true} if archived metadata was found and imported, otherwise {@code false}
   */
  public static boolean loadFromZip(@NotNull final ZipFile zipFile) throws IOException {
    final ZipEntry entry = zipFile.getEntry(PROJECT_METADATA_FILENAME);
    if (entry == null) {
      return false;
    }

    File tempFile = null;
    try {
      tempFile = FileAndPathUtil.createTempFile(TEMP_FILE_PREFIX, ".tsv");
      try (InputStream input = zipFile.getInputStream(entry)) {
        Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      final ProjectMetadataReader reader = new ProjectMetadataReader(false, false);
      final MetadataTable importedMetadata = reader.readFile(tempFile);
      final List<String> errors = reader.getErrors();
      if (importedMetadata == null || !errors.isEmpty()) {
        throw new IOException("Could not import project metadata: " + String.join("; ", errors));
      }

      ProjectService.getMetadata().merge(importedMetadata);
      return true;
    } finally {
      deleteTempFile(tempFile);
    }
  }

  private static void deleteTempFile(final File tempFile) {
    if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
      tempFile.deleteOnExit();
    }
  }
}
