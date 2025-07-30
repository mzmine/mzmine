/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.projectmetadata.table;

import static io.github.mzmine.util.files.FileAndPathUtil.eraseFormat;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetadataTableUtils {


  /**
   * Only maps files that actually have a raw data file in metadata table
   *
   * @param table     metadata table
   * @param dataFiles data files that may be already loaded or will be loaded. Remember to apply
   *                  {@link AllSpectralDataImportParameters#streamValidatedFiles(File[])} if the
   *                  files are not yet loaded to use the actually loaded file
   * @return a map of all dataFiles that have an entry in the table, key is the dataFiles.getName
   */
  @NotNull
  public static Map<String, RawDataFile> matchFileNames(MetadataTable table, File[] dataFiles) {
    final Map<String, RawDataFile> nameMap = HashMap.newHashMap(dataFiles.length);

    // create map of names as they may be full names with format or without
    // raw files may be placeholders here
    final List<RawDataFile> rawFiles = table.getRawDataFilesUnsorted();

    // check for files missing in metadata
    List<String> filesMissingInTable = new ArrayList<>();
    for (File file : dataFiles) {
      // match with the same method that is used during metadata import and raw file matching
      final Optional<RawDataFile> actualFile = rawFiles.stream()
          .filter(raw -> matchesFilename(file.getName(), raw)).findFirst();

      actualFile.ifPresent(raw -> nameMap.put(file.getName(), raw));
    }

    return nameMap;
  }

  /**
   * @param table     metadata table
   * @param dataFiles data files that may be already loaded or will be loaded. Remember to apply
   *                  {@link AllSpectralDataImportParameters#streamValidatedFiles(File[])} if the
   *                  files are not yet loaded to use the actually loaded file
   * @return list of error messages
   */
  @NotNull
  public static List<String> checkTableAgainstFiles(MetadataTable table, File[] dataFiles) {
    List<String> errors = new ArrayList<>();

    // create map of names as they may be full names with format or without
    // raw files may be placeholders here
    final List<RawDataFile> rawFiles = table.getRawDataFilesUnsorted();

    // check for files missing in metadata
    List<String> filesMissingInTable = new ArrayList<>();
    for (File file : dataFiles) {
      // match with the same method that is used during metadata import and raw file matching
      final Optional<RawDataFile> actualFile = rawFiles.stream()
          .filter(raw -> matchesFilename(file.getName(), raw)).findFirst();

      if (actualFile.isEmpty()) {
        filesMissingInTable.add(file.getName());
      }
    }
    if (!filesMissingInTable.isEmpty()) {
      errors.add("Low severity warning: The following files are not present in the metadata table: "
          + String.join(", ", filesMissingInTable));
    }

    // do not check if all rows in the table are actually imported.
    // metadata table may be longer than actual data import list

    return errors;
  }

  /**
   * Logic to match the a name against the {@link RawDataFile#getName()}. This is used to match
   * metadata to raw files
   */
  public static boolean matchesFilename(final @Nullable String name, @NotNull RawDataFile raw) {
    if (name == null) {
      return false;
    }
    final String target = raw.getName();
    final String noFormatName = eraseFormat(name).trim();

    return name.equalsIgnoreCase(target) || name.equalsIgnoreCase(eraseFormat(target))
        || noFormatName.equalsIgnoreCase(target) || noFormatName.equalsIgnoreCase(
        eraseFormat(target));
  }
}
