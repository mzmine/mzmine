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

import io.github.mzmine.modules.presets.ModulePresetStoreFactory;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterPresetStore;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Register new {@link PresetStoreFactory} with
 * {@link #registerStoreFactory(PresetStoreFactoryKey, PresetStoreFactory)}
 * <p>
 * Then use the factory to generate the store by only knowing the {@link PresetCategory} and group
 * like {@link KnownPresetGroup} or other String group.
 */
public class PresetStoreService {

  private static final Logger logger = Logger.getLogger(PresetStoreService.class.getName());

  private static final Map<PresetStoreFactoryKey, PresetStoreFactory<?>> storeFactories = new HashMap<>();

  // define known preset factories
  // can register more elsewhere
  static {
    registerStoreFactory(PresetStoreFactoryKey.MODULE(), new ModulePresetStoreFactory());
    registerStoreFactory(PresetCategory.FILTERS,
        KnownPresetGroup.ROW_TYPE_FILTER_PRESET.getUniqueID(),
        (_, _) -> new RowTypeFilterPresetStore());
  }


  public static <T extends Preset> void registerStoreFactory(@NotNull PresetCategory category,
      @NotNull String group, @NotNull PresetStoreFactory<T> factory) {
    registerStoreFactory(PresetStoreFactoryKey.create(category, group), factory);
  }

  public static synchronized <T extends Preset> void registerStoreFactory(
      @NotNull PresetStoreFactoryKey key, @NotNull PresetStoreFactory<T> factory) {
    storeFactories.put(key, factory);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public static <T extends Preset> PresetStore<T> createStoreSilent(
      @NotNull PresetCategory category, @NotNull String group) {
    try {
      return createStoreOrThrow(category, group);
    } catch (Exception exception) {
      logger.log(Level.WARNING,
          "Failed to create preset store for " + category + " and group " + group);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public static <T extends Preset> PresetStore<T> createStoreOrThrow(
      @NotNull PresetCategory category, @NotNull String group) {
    final PresetStoreFactoryKey key = PresetStoreFactoryKey.create(category, group);
    PresetStoreFactory<T> factory = (PresetStoreFactory<T>) storeFactories.get(key);
    if (factory == null) {
      throw new IllegalArgumentException("No store factory registered for key: " + key);
    }
    return factory.createStore(category, group);
  }

  /**
   * Every folder in {@link FileAndPathUtil#PRESETS_DIR} is a category. Every folder in that
   * category is a group.
   *
   * @return list of all preset files Category first then group
   */
  @NotNull
  public static List<LocalStoredPresetGroup> findAllLocalStoredPresetGroups() {
    List<LocalStoredPresetGroup> localStoredPresetGroups = new ArrayList<>();

    final File presetsDir = FileAndPathUtil.PRESETS_DIR;
    final File[] categories = FileAndPathUtil.getSubDirectories(presetsDir);
    for (File catFile : categories) {
      final PresetCategory category = PresetCategory.parse(catFile.getName());
      if (category == null) {
        continue;
      }

      // load all groups
      final File[] groupFiles = FileAndPathUtil.getSubDirectories(catFile);
      for (File groupFile : groupFiles) {
        localStoredPresetGroups.add(
            new LocalStoredPresetGroup(groupFile, category, groupFile.getName()));
      }
    }
    return localStoredPresetGroups;
  }

}
