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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxFileChooser;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PresetStore<T extends Preset> {

  default File getPresetStoreFilePath() {
    // make sure this exists
    final File dir = FileAndPathUtil.PRESETS_DIR.toPath().resolve(getPresetCategory().getUniqueID())
        .resolve(getPresetGroup()).toFile();
    return dir;
  }

  default @NotNull File[] getAllPresetFiles() {
    return FileAndPathUtil.findFilesInDirFlat(getPresetStoreFilePath(),
        new ExtensionFilter("filter", getFileExtension()), false, false);
  }

  default File getPresetFile(@NotNull T preset) {
    final File path = getPresetStoreFilePath();
    final String name = preset.getFileName();
    return FileAndPathUtil.getRealFilePath(new File(path, name), getFileExtension());
  }


  default void deletePresetFile(T removed) {
    try {
      final File file = getPresetFile(removed);
      FileUtils.delete(file);
    } catch (IOException e) {
    }
  }

  /**
   * a category (category>group) that acts as the first folder in presets directory
   */
  @NotNull PresetCategory getPresetCategory();


  /**
   * The group name (category>group). For modules this is the Module class name.
   */
  @NotNull String getPresetGroup();

  @NotNull List<ExtensionFilter> getExtensionFilters();

  /**
   * @return extension without . like json
   */
  @NotNull String getFileExtension();

  /**
   * @return currently loaded presets
   */
  @NotNull ObservableList<T> getCurrentPresets();

  /**
   * The list of default presets is usually added if there are no other presets available.
   *
   * @return a list of default presets
   */
  @NotNull List<T> createDefaults();

  /**
   * Saves into the {@link #getPresetStoreFilePath()} filename=presetName.EXTENSION
   *
   * @param preset the preset to save
   */
  void saveToFile(@NotNull T preset);

  @Nullable T loadFromFile(@NotNull File file);


  /**
   * Loads all presets. If the presets are empty, then:
   * <pre>
   * 1. Save all defaults as presets
   * 2. set to list
   * </pre>
   */
  default void loadAllPresetsOrDefaults() {
    final @NotNull File[] files = getAllPresetFiles();
    List<T> presets = Arrays.stream(files).map(this::loadFromFile).filter(Objects::nonNull)
        .toList();

    if (presets.isEmpty()) {
      // empty --> create defaults and save
      presets = createDefaults();
      for (T preset : presets) {
        saveToFile(preset);
      }
    }

    // already saved
    setAllAndSavePreset(presets, false, true);
  }


  /**
   * Replaces all presets
   *
   * @param presets       all presets to set
   * @param save          if true then save to store
   * @param autoOverwrite if true then just use the new presets, if false then user will see dialog
   *                      for each duplicate
   */
  default void setAllAndSavePreset(List<T> presets, boolean save, boolean autoOverwrite) {
    final List<T> items = presets.stream()
        .map(preset -> autoOverwrite ? preset : requireNonNullElse(userKeepsOld(preset), preset))
        .filter(Objects::nonNull).toList();

    getCurrentPresets().setAll(items);
    if (save) {
      for (T item : items) {
        saveToFile(item);
      }
    }
  }


  @NotNull
  default List<T> loadPresetsInDialog() {
    final List<File> files = FxFileChooser.openSelectMultiDialog(getExtensionFilters(),
        getPresetStoreFilePath(), "Select %s files to load presets".formatted(getFileExtension()));
    if (files == null) {
      return List.of();
    }

    final List<T> presets = files.stream().map(this::loadFromFile).filter(Objects::nonNull)
        .toList();
    for (T preset : presets) {
      // make sure the preset is also in
      addAndSavePreset(preset, true, false);
    }

    return presets;
  }

  /**
   * Opens dialog asking for name, then saving the preset, adding it to the list. If the name is a
   * duplicate also ask for overwrite permission.
   *
   * @param presetFactory generates the preset for a given name
   * @return the new preset or null if user decided to cancel or did not overwrite
   */
  @Nullable
  default T showSaveDialog(@NotNull Function<String, @NotNull T> presetFactory) {
    final String name = DialogLoggerUtil.createTextInputDialog("Save Preset",
        "Enter a name for the preset", "Name:").showAndWait().orElse(null);
    if (name != null && !name.isBlank()) {
      final T preset = presetFactory.apply(name);
      return addAndSavePreset(preset, true, false);
    }
    return null;
  }

  /**
   * Check with user if they want to keep old preset
   *
   * @return if duplicate and user wants to keep old - return old. otherwise null
   */
  @Nullable T userKeepsOld(T preset);

  /**
   * @param preset        to save
   * @param save          save to file
   * @param autoOverwrite do not ask user
   * @return the input preset if successful. Or null, e.g., if user decided to cancel
   */
  @Nullable
  default T addAndSavePreset(T preset, boolean save, boolean autoOverwrite) {
    if (preset == null) {
      return null;
    }
    if (!autoOverwrite && userKeepsOld(preset) != null) {
      return null;
    }

    removePresetsWithName(preset);
    getCurrentPresets().add(preset);
    if (save) {
      saveToFile(preset);
    }
    return preset;
  }

  default void removePresetsWithName(T preset) {
    getCurrentPresets().removeIf(p -> p.equalsIgnoreCaseName(preset));
  }


  default Optional<T> getPresetForName(T preset) {
    return getCurrentPresets().stream().filter(f -> f.equalsIgnoreCaseName(preset)).findAny();
  }

  default Optional<T> getPresetForName(String name) {
    return getCurrentPresets().stream()
        .filter(f -> f.getFileName().equalsIgnoreCase(name) || f.name().equalsIgnoreCase(name))
        .findAny();
  }

  default void ensureDirectoryExists() {
    FileAndPathUtil.createDirectory(getPresetStoreFilePath());
  }
}
