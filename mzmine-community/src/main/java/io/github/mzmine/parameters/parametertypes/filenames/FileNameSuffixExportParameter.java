/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Parameter for export filename. This parameter allows to set a new value and add the suffix to it
 */
public class FileNameSuffixExportParameter extends FileNameParameter {

  private final String suffix;

  /**
   * @param suffix a suffix that will be added to the file name when changing the output files.
   *               suffix="test" will result in filename_test
   */
  public FileNameSuffixExportParameter(String name, String description, String suffix) {
    this(name, description, suffix, true);
  }

  /**
   * @param suffix a suffix that will be added to the file name when changing the output files.
   *               suffix="test" will result in filename_test
   */
  public FileNameSuffixExportParameter(String name, String description, String suffix,
      boolean allowEmptyString) {
    this(name, description, List.of(), suffix, allowEmptyString);
  }

  /**
   * @param suffix a suffix that will be added to the file name when changing the output files.
   *               suffix="test" will result in filename_test
   */
  public FileNameSuffixExportParameter(String name, String description,
      List<ExtensionFilter> filters, String suffix) {
    this(name, description, filters, suffix, true);
  }

  /**
   * @param suffix a suffix that will be added to the file name when changing the output files.
   *               suffix="test" will result in filename_test
   */
  public FileNameSuffixExportParameter(String name, String description,
      List<ExtensionFilter> filters, String suffix, boolean allowEmptyString) {
    super(name, description, filters, FileSelectionType.SAVE, allowEmptyString);
    this.suffix = suffix;
  }

  public String getSuffix() {
    return suffix;
  }

  /**
   * Add suffix to file name and set value. Old format is kept
   *
   * @return filename with the suffix.
   */
  public File setValueAppendSuffix(File file) {
    File suffixedFile = FileAndPathUtil.getRealFilePathWithSuffix(file, suffix);
    FileAndPathUtil.getRealFilePathWithSuffix(file, suffix);
    setValue(suffixedFile);
    return suffixedFile;
  }
}
