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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
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
  @Nullable
  public File saveToFile(@NotNull File file, @NotNull T preset) {
    file = FileAndPathUtil.getRealFilePath(file, getFileExtension());

    final File parentFile = file.getParentFile();
    if (parentFile != null) {
      FileAndPathUtil.createDirectory(parentFile);
    }
    try (var writer = Files.newBufferedWriter(file.toPath(),
        WriterOptions.REPLACE.toOpenOption())) {
      writePreset(preset, writer);
      return file;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * May change logic for more complex objects
   */
  protected void writePreset(@NotNull T preset, BufferedWriter writer) throws IOException {
    writer.write(getMapper().writeValueAsString(preset));
  }

  public @Nullable T loadFromFileOrThrow(@NotNull File file)
      throws IOException, PresetTypeMismatchException {
    // make sure to add all json Presets to the annotation on {@link Preset} class
    final Preset preset = getMapper().readValue(file, Preset.class);
    if (preset == null) {
      return null;
    }

    if (!(preset.presetCategory().equals(getPresetCategory()) && Objects.equals(
        preset.presetGroup(), getPresetGroup().getUniqueID()))) {
      throw new PresetTypeMismatchException(preset, this);
    }

    try {
      return (T) preset;
    } catch (Exception e) {
      throw new PresetTypeMismatchException(preset, this);
    }
  }

  @Override
  public @Nullable T loadFromFile(@NotNull File file) {
    try {
      return loadFromFileOrThrow(file);
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Preset file is broken: %s\n%s".formatted(file.getName(), e.getMessage()), e);
    } catch (PresetTypeMismatchException e) {
      logger.log(Level.WARNING,
          "Preset file is from different preset store: %s\n%s".formatted(file.getName(),
              e.getMessage()), e);
    }
    return null;
  }

  /**
   * May overwrite to provide different mapper for serialization control
   */
  protected @NotNull ObjectMapper getMapper() {
    return JsonUtils.MAPPER;
  }

}
