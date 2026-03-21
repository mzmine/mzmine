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

package io.github.mzmine.parameters.parametertypes.filenames;

import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.Nullable;

/**
 * Parameter for export filename. This parameter allows to set a new value and add the suffix to it
 */
public class FileNameSuffixExportParameter extends FileNameParameter {

  @Nullable
  private final String suffix;

  public FileNameSuffixExportParameter(String name, String description) {
    this(name, description, null, true);
  }

  /**
   * @param suffix a suffix that will be added to the file name when changing the output files.
   *               suffix="test" will result in filename_test
   */
  public FileNameSuffixExportParameter(String name, String description, @Nullable String suffix) {
    this(name, description, suffix, true);
  }

  /**
   * @param suffix a suffix that will be added to the file name when changing the output files.
   *               suffix="test" will result in filename_test
   */
  public FileNameSuffixExportParameter(String name, String description, @Nullable String suffix,
      boolean allowEmptyString) {
    this(name, description, List.of(), suffix, allowEmptyString);
  }

  /**
   * @param suffix a suffix that will be added to the file name when changing the output files.
   *               suffix="test" will result in filename_test
   */
  public FileNameSuffixExportParameter(String name, String description,
      List<ExtensionFilter> filters, @Nullable String suffix) {
    this(name, description, filters, suffix, true);
  }

  /**
   * @param suffix a suffix that will be added to the file name when changing the output files.
   *               suffix="test" will result in filename_test
   */
  public FileNameSuffixExportParameter(String name, String description,
      List<ExtensionFilter> filters, @Nullable String suffix, boolean allowEmptyString) {
    super(name,
        description + (StringUtils.hasValue(suffix) ? "\n(default suffix=" + suffix + ")" : ""),
        filters, FileSelectionType.SAVE, allowEmptyString);
    this.suffix = suffix;
  }

  @Nullable
  public String getSuffix() {
    return suffix;
  }

  @Override
  public File getValue() {
    File value = super.getValue();
    if (value == null) {
      return null;
    }
    // automatically add the extension to the file so that export modules generate the correct format
    final List<String> formats = getFormatExtensionList();
    if (formats.size() == 1) {
      // for a single extension option use this
      value = FileAndPathUtil.getRealFilePath(value, formats.getFirst());
    } else if (formats.size() > 1) {
      // some modules allow tsv or csv export and the user needs to specify the format in the file name
      // so there is no other parameter controlling the actual format
      final String lowerName = value.getName().toLowerCase().strip();
      if (formats.stream().map(String::toLowerCase).noneMatch(lowerName::endsWith)) {
        // none of the extensions match the current file name extension.
        // therefore, add the first as the preferred extension
        value = FileAndPathUtil.getRealFilePath(value, formats.getFirst());
      }
      // otherwise a format was already defined by the user
    }
    return value;
  }

  /**
   * Add suffix to file name and set value. Old format is kept
   *
   * @return filename with the suffix.
   */
  public File setValueAppendSuffix(File file, final @Nullable String duplicateSuffix) {

    // try to use the same format as used before
    // this is useful for modules that may export to csv or tsv or other multiple formats
    final List<String> formats = getFormatExtensionList();
    final String previousValueLowerName =
        getValue() == null ? "" : getValue().getName().toLowerCase();
    final Optional<String> extension = formats.stream().map(String::toLowerCase)
        .filter(previousValueLowerName::endsWith).findFirst();

    file = FileAndPathUtil.eraseFormat(file);
    if (StringUtils.hasValue(suffix)) {
      file = FileAndPathUtil.getRealFilePathWithSuffix(file, "_" + suffix);
    }
    // may add another suffix fur duplicate files
    if (StringUtils.hasValue(duplicateSuffix)) {
      file = FileAndPathUtil.getRealFilePathWithSuffix(file, duplicateSuffix);
    }

    // set to the previous extension if it was matched
    if (extension.isPresent()) {
      file = FileAndPathUtil.getRealFilePath(file, extension.get());
    }

    setValue(file);
    return file;
  }


  @Override
  public FileNameParameter cloneParameter() {
    FileNameSuffixExportParameter copy = new FileNameSuffixExportParameter(name, description,
        filters, suffix, allowEmptyString);
    copy.setValue(this.getValue());
    copy.setLastFiles(new ArrayList<>(lastFiles));
    return copy;
  }
}
