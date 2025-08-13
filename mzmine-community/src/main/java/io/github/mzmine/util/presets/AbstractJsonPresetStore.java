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

package io.github.mzmine.util.presets;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.JsonUtils;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores each preset in their own json file
 *
 * @param <T>
 */
public abstract class AbstractJsonPresetStore<T extends Preset> extends AbstractPresetStore<T> {

  private static final Logger logger = Logger.getLogger(AbstractJsonPresetStore.class.getName());

  @Override
  public @NotNull List<ExtensionFilter> getExtensionFilters() {
    return List.of(ExtensionFilters.MZ_PRESETS, ExtensionFilters.ALL_FILES);
  }

  @Override
  public @NotNull String getFileExtension() {
    return "mzpresets";
  }

  @Override
  public void saveToFile(@NotNull T preset) {
    super.ensureDirectoryExists();
    final File file = getPresetFile(preset);

    final File parentFile = file.getParentFile();
    if (parentFile != null) {
      FileAndPathUtil.createDirectory(parentFile);
    }
    try (var writer = Files.newBufferedWriter(file.toPath(),
        WriterOptions.REPLACE.toOpenOption())) {
      writer.write(getMapper().writeValueAsString(preset));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public @Nullable T loadFromFile(@NotNull File file) {
    try {
      // make sure to add all json Presets to the annotation on {@link Preset} class
      return (T) getMapper().readValue(file, Preset.class);
    } catch (IOException e) {
      logger.warning("Preset file is broken: %s\n%s".formatted(file.getName(), e.getMessage()));
      return null;
    }
  }

  /**
   * May overwrite to provide different mapper for serialization control
   */
  protected @NotNull ObjectMapper getMapper() {
    return JsonUtils.MAPPER;
  }

}
