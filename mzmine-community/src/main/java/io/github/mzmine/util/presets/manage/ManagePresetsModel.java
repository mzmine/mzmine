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

import io.github.mzmine.util.presets.FxPresetEditor;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetStore;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ManagePresetsModel {

  /**
   * Usually the {@link ManagePresetsInteractor}
   */
  private ObjectProperty<Consumer<ManagePresetsEvent>> eventHandler = new SimpleObjectProperty<>();

  /**
   * main values that define other values
   */
  private final ObservableList<PresetStore<?>> allStores = FXCollections.observableArrayList();

  /**
   * The selected group / preset store
   */
  private final ObjectProperty<PresetStore<?>> selectedGroupStore = new SimpleObjectProperty<>();

  private final ObjectProperty<FxPresetEditor> presetEditor = new SimpleObjectProperty<>();

  private final ObservableList<Preset> selectedStoreDefaults = FXCollections.observableArrayList();
  private final BooleanBinding storeHasDefaults;

  /**
   * derived from selected store
   */
  private final ReadOnlyListWrapper<Preset> selectedGroupStorePresets = new ReadOnlyListWrapper<>();

  /**
   * Selected preset in list for edit
   */
  private final ObjectProperty<@Nullable EditPresetModel> selectedPreset = new SimpleObjectProperty<>();


  public ManagePresetsModel() {
    storeHasDefaults = Bindings.isNotEmpty(selectedStoreDefaults);
  }

  public ObjectProperty<@Nullable EditPresetModel> selectedPresetProperty() {
    return selectedPreset;
  }

  @Nullable
  public EditPresetModel getSelectedPreset() {
    return selectedPreset.get();
  }

  public void setEventHandler(Consumer<ManagePresetsEvent> eventHandler) {
    this.eventHandler.set(eventHandler);
  }

  public PresetStore<?> getSelectedGroupStore() {
    return selectedGroupStore.get();
  }

  public ObjectProperty<PresetStore<?>> selectedGroupStoreProperty() {
    return selectedGroupStore;
  }

  public void setSelectedGroupStore(PresetStore<?> selectedGroupPresetStore) {
    this.selectedGroupStore.set(selectedGroupPresetStore);
  }

  public ReadOnlyListProperty<Preset> getSelectedGroupStorePresets() {
    return selectedGroupStorePresets.getReadOnlyProperty();
  }

  public void setSelectedGroupStorePresets(@Nullable ObservableList<? extends Preset> presets) {
    selectedGroupStorePresets.set((ObservableList<Preset>) presets);
  }

  /**
   * sends events to eventHandler
   */
  void consumeEvent(@NotNull ManagePresetsEvent event) {
    final Consumer<ManagePresetsEvent> handler = eventHandler.get();
    if (handler == null) {
      throw new IllegalStateException("No event handler set");
    }
    handler.accept(event);
  }

  public ObservableList<PresetStore<?>> getAllStores() {
    return allStores;
  }

  public void setSelectedPreset(@Nullable Preset preset) {
    selectedPreset.set(preset == null ? null : new EditPresetModel(preset));
  }

  public boolean isStoreHasDefaults() {
    return storeHasDefaults.get();
  }

  public BooleanExpression storeHasDefaultsProperty() {
    return storeHasDefaults;
  }

  public ObservableList<Preset> getSelectedStoreDefaults() {
    return selectedStoreDefaults;
  }

  public FxPresetEditor getPresetEditor() {
    return presetEditor.get();
  }

  public ObjectProperty<FxPresetEditor> presetEditorProperty() {
    return presetEditor;
  }

  public void setPresetEditor(FxPresetEditor presetEditor) {
    this.presetEditor.set(presetEditor);
  }
}
