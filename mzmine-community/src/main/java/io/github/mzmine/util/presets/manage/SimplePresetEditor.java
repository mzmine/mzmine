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

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.util.presets.FxPresetEditor;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetStore;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public abstract class SimplePresetEditor<T extends Preset> extends HBox implements FxPresetEditor {

  private final ObjectProperty<EditPresetModel> preset = new SimpleObjectProperty<>();
  private final PresetStore store;


  public SimplePresetEditor(PresetStore store) {
    super(FxLayout.DEFAULT_SPACE);
    this.store = store;

    preset.subscribe((_, nv) -> {
      setPresetInComponent(nv == null ? null : (T) nv.getOriginalPreset());
    });

    final Button applyButton = FxButtons.createButton("Apply", FxIcons.EDIT,
        "Apply changes to preset and save.", this::saveModifiedPreset);

    getChildren().addAll(applyButton);
  }

  protected void setModifiedPreset(T modPreset) {
    final EditPresetModel model = preset.get();
    if (model == null) {
      return;
    }
    model.setModifiedPreset(modPreset);
  }

  protected abstract void setPresetInComponent(T originalPreset);

  private void saveModifiedPreset() {
    final EditPresetModel model = preset.get();
    if (model == null || !model.isModified()) {
      return;
    }
    // after saving actual
    final Preset presetToSave = model.createModifiedWithName();

    final Preset old = store.userKeepsOld(presetToSave, false);
    if (old != null) {
      // user keeps old
      return;
    }

    // set invalid to avoid more changes than this
    model.setInvalid();
    // delete old without modification
    final Preset unmodifiedPreset = model.getOriginalPreset();
    store.removePresetsWithName(unmodifiedPreset);

    // save overwrite
    store.addAndSavePreset(presetToSave, true);
  }

  @Override
  public Node getEditorNode() {
    return this;
  }

  @Override
  public ObjectProperty<EditPresetModel> presetProperty() {
    return preset;
  }
}
