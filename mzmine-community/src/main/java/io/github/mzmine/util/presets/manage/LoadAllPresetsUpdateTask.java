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

package io.github.mzmine.util.presets.manage;

import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.taskcontrol.progress.TotalFinishedItemsProgress;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.presets.LocalStoredPresetGroup;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetCategory;
import io.github.mzmine.util.presets.PresetStore;
import io.github.mzmine.util.presets.PresetStoreService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class LoadAllPresetsUpdateTask extends FxUpdateTask<ManagePresetsModel> {

  private static final Logger logger = Logger.getLogger(LoadAllPresetsUpdateTask.class.getName());
  private List<Preset> allPresets = new ArrayList<>();
  private List<PresetStore<?>> allStores = new ArrayList<>();
  private final TotalFinishedItemsProgress progress = new TotalFinishedItemsProgress(100);

  protected LoadAllPresetsUpdateTask(@NotNull ManagePresetsModel model) {
    super("Load all presets", model);
  }

  @Override
  protected void process() {
    final List<LocalStoredPresetGroup> localFiles = PresetStoreService.findAllLocalStoredPresetGroups();
    if (localFiles.isEmpty()) {
      return;
    }

    progress.setTotal(localFiles.size());

    for (LocalStoredPresetGroup localGroup : localFiles) {
      load(localGroup);
      progress.getAndIncrement();
    }
  }

  private void load(LocalStoredPresetGroup localGroup) {
    // get factory for group
    final PresetStore store = findStore(localGroup);
    if (store == null) {
      logger.fine(
          "Could not create preset store for group (this may be an old group, remove files to clear this message): "
              + localGroup);
      return;
    }
    allStores.add(store);

    final @NotNull File[] presetFiles = FileAndPathUtil.findFilesInDirFlat(localGroup.groupFile(),
        ExtensionFilters.MZ_PRESETS, false, false);
    for (File presetFile : presetFiles) {
      final Preset preset = store.loadFromFile(presetFile);
      if (preset == null) {
        continue;
      }
      //  overwrite in list
      store.addAndSavePreset(preset, true);
      allPresets.add(preset);
    }
  }

  /**
   * First search in existing stores then create one from factory
   */
  private @Nullable PresetStore<?> findStore(LocalStoredPresetGroup localGroup) {
    final PresetCategory cat = localGroup.category();
    final String group = localGroup.group();
    for (PresetStore<?> store : model.getAllStores()) {
      if (store.matches(cat, group)) {
        return store;
      }
    }

    return PresetStoreService.createStoreSilent(cat, group);
  }

  @Override
  protected void updateGuiModel() {
    model.getAllStores().setAll(allStores);
  }

  @Override
  public String getTaskDescription() {
    return "Loading all presets from local storage...";
  }

  @Override
  public double getFinishedPercentage() {
    return progress.progress();
  }
}
