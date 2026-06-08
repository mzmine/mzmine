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

import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.util.presets.FxPresetEditor;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetStore;
import io.github.mzmine.util.presets.PresetStoreKey;
import io.github.mzmine.util.presets.PresetUtils;
import io.github.mzmine.util.presets.manage.ManagePresetsEvent.AddDefaultPresetsEvent;
import io.github.mzmine.util.presets.manage.ManagePresetsEvent.RemoveSelectedPresetsEvent;
import io.github.mzmine.util.presets.manage.ManagePresetsEvent.RenameSelectedPresetEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ManagePresetsInteractor extends FxInteractor<ManagePresetsModel> implements
    Consumer<ManagePresetsEvent> {

  private static final Logger logger = Logger.getLogger(ManagePresetsInteractor.class.getName());


  private final ManagePresetsController controller;

  public ManagePresetsInteractor(ManagePresetsController controller,
      @NotNull ManagePresetsModel model) {
    super(model);
    this.controller = controller;
    this.model.setEventHandler(this);
    initListeners();
  }


  private void initListeners() {
    final ObservableList<PresetStore<?>> allStores = model.getAllStores();
    allStores.addListener((ListChangeListener<? super PresetStore<?>>) _ -> {
      logger.info("All stores changed");
    });

    model.selectedGroupStoreProperty().subscribe(store -> {
      model.setSelectedGroupStorePresets(store == null ? null : store.getCurrentPresets());
      model.setPresetEditor(store == null ? null : store.createPresetEditor());
      if (store == null) {
        model.getSelectedStoreDefaults().clear();
        return;
      }
      List<? extends Preset> defaults = store.createDefaults();
      defaults = PresetUtils.filterDifferent(store.getCurrentPresets(), defaults);
      model.getSelectedStoreDefaults().setAll(defaults);

      logger.info("Selected store changed: %s".formatted(store));
      logger.info("Selected store defaults changed to n %d".formatted(defaults.size()));
    });

    // old was modified and changed to other selection
    // ask if save
    model.selectedPresetProperty().subscribe((old, selected) -> {
      final FxPresetEditor editor = model.getPresetEditor();
      if (editor != null) {
        editor.setPreset(selected);
      }

      if (old == null || !old.isModified()) {
        return;
      }
      model.consumeEvent(new RenameSelectedPresetEvent(old, true));
      logger.info("Selected preset changed: %s".formatted(old));
    });

  }


  PresetStore<?> findPresetStore(@Nullable PresetStoreKey group) {
    final ObservableList<PresetStore<?>> stores = model.getAllStores();
    if (group == null || stores.isEmpty()) {
      return null;
    }
    return stores.stream().filter(store -> store.matches(group.category(), group.group()))
        .findFirst().orElse(null);
  }

  @Override
  public void updateModel() {
  }

  @Override
  public void accept(ManagePresetsEvent managePresetsEvent) {
    final PresetStore store = model.getSelectedGroupStore();
    if (managePresetsEvent == null || store == null) {
      return;
    }

    switch (managePresetsEvent) {
      case AddDefaultPresetsEvent _ -> {
        ObservableList<Preset> defaults = model.getSelectedStoreDefaults();
        List<Preset> unique = PresetUtils.filterDifferent(store.getCurrentPresets(), defaults);
        for (Preset preset : unique) {
          store.addAndSavePreset(preset, false);
        }
        model.getSelectedStoreDefaults().clear();
      }
      case RemoveSelectedPresetsEvent(List<Preset> presets) -> {
        for (Preset preset : presets) {
          store.removePresetsWithName(preset);
        }
      }
      case RenameSelectedPresetEvent(@Nullable EditPresetModel preset, boolean askUser) -> {
        if (preset == null || !preset.isModified() || preset.isInvalid()) {
          return;
        }

        // ask to rename and then save with new name
        final String newName = preset.getName();
        final boolean rename = !askUser || DialogLoggerUtil.showDialogYesNo("Renaming preset",
            "Rename preset '%s' to '%s'?".formatted(preset.getOriginalName(), newName));

        if (rename) {
          preset.setInvalid();
          store.removePresetsWithName(preset.getOriginalPreset());
          Preset actual = store.addAndSavePreset(preset.createModifiedWithName(), false);
          model.setSelectedPreset(actual);
        }
      }
    }
  }
}
