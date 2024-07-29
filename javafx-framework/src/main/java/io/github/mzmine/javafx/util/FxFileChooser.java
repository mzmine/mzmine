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

package io.github.mzmine.javafx.util;

import java.io.File;
import java.util.List;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.Nullable;

public class FxFileChooser {

  public enum FileSelectionType {
    OPEN, SAVE
  }

  public static File openSelectDialog(final FileSelectionType type,
      final List<ExtensionFilter> filters, @Nullable File initialDir) {
    // Create chooser.
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select file");
    if (filters != null) {
      fileChooser.getExtensionFilters().addAll(filters);
    }

    // Set current directory.
    try {
      if (initialDir != null) {
        final File currentDir = initialDir.getParentFile();
        if (currentDir != null && currentDir.exists()) {
          fileChooser.setInitialDirectory(currentDir);
        }
      }
    } catch (Exception _) {
    }

    // Open chooser.
    File selectedFile = null;
    if (type == FxFileChooser.FileSelectionType.OPEN) {
      selectedFile = fileChooser.showOpenDialog(null);
    } else {
      selectedFile = fileChooser.showSaveDialog(null);
    }
    return selectedFile;
  }
}
