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

package io.github.mzmine.parameters.parametertypes.filenames;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// additional controls on the side to apply subsetting or other operations
public class FileNamesWithSideBarControlsParameter extends FileNamesParameter {

  private final @Nullable Function<FileNamesComponent, @Nullable Node> buttonBarTopControlsFactory;

  public FileNamesWithSideBarControlsParameter(final @NotNull String name,
      final @NotNull String description, final @NotNull List<ExtensionFilter> filters,
      @Nullable final String dragPrompt,
      @Nullable final Function<FileNamesComponent, @Nullable Node> buttonBarTopControlsFactory) {
    this(name, description, filters, dragPrompt, DEFAULT_ALL_FILES_MAPPER,
        buttonBarTopControlsFactory);
  }

  public FileNamesWithSideBarControlsParameter(final @NotNull String name,
      final @NotNull String description, final @NotNull List<ExtensionFilter> filters,
      @Nullable final String dragPrompt, @NotNull final Function<File[], File[]> allFilesMapper,
      @Nullable final Function<FileNamesComponent, @Nullable Node> buttonBarTopControlsFactory) {
    this(name, description, filters, null, null, dragPrompt, allFilesMapper,
        buttonBarTopControlsFactory);
  }

  public FileNamesWithSideBarControlsParameter(final @NotNull String name,
      final @NotNull String description, final @NotNull List<ExtensionFilter> filters,
      @Nullable final Path defaultDir, @Nullable final File[] defaultFiles,
      @Nullable final String dragPrompt, @NotNull final Function<File[], File[]> allFilesMapper,
      @Nullable final Function<FileNamesComponent, @Nullable Node> buttonBarTopControlsFactory) {
    super(name, description, filters, defaultDir, defaultFiles, dragPrompt, allFilesMapper);
    this.buttonBarTopControlsFactory = buttonBarTopControlsFactory;
  }

  @Override
  public @NotNull FileNamesComponent createEditingComponent() {
    return new FileNamesComponent(getFilters(), getDefaultDir(), List.of(), dragPrompt,
        allFilesMapper, buttonBarTopControlsFactory);
  }

  @Override
  public @NotNull FileNamesWithSideBarControlsParameter cloneParameter() {
    return new FileNamesWithSideBarControlsParameter(getName(), getDescription(), getFilters(),
        getDefaultDir(), getValue(), dragPrompt, allFilesMapper, buttonBarTopControlsFactory);
  }
}
