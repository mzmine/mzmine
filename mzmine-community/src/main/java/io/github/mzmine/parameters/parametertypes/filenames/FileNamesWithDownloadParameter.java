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

import io.github.mzmine.modules.io.download.DownloadAsset;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileNamesWithDownloadParameter extends FileNamesParameter {

  // those may be updated externally in the future
  private final List<DownloadAsset> downloadLinks;

  public FileNamesWithDownloadParameter(final String name, final String description,
      final List<ExtensionFilter> filters, List<DownloadAsset> assets,
      @Nullable String dragPrompt) {
    super(name, description, filters, dragPrompt);
    downloadLinks = assets;
  }

  public FileNamesWithDownloadParameter(final String name, final String description,
      final List<ExtensionFilter> filters, final Path defaultDir, final File[] defaultFiles,
      final List<DownloadAsset> downloadLinks, @Nullable String dragPrompt,
      @NotNull Function<File[], File[]> allFilesMapper) {
    super(name, description, filters, defaultDir, defaultFiles, dragPrompt, allFilesMapper);
    this.downloadLinks = downloadLinks;
  }

  @Override
  public FileNamesComponent createEditingComponent() {
    return new FileNamesComponent(getFilters(), getDefaultDir(), downloadLinks, dragPrompt,
        allFilesMapper);
  }

  @Override
  public FileNamesWithDownloadParameter cloneParameter() {
    return new FileNamesWithDownloadParameter(getName(), getDescription(), getFilters(),
        getDefaultDir(), getValue(), this.downloadLinks, dragPrompt, allFilesMapper);
  }
}
