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

import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManagePresetsController extends FxController<ManagePresetsModel> {

  private final ManagePresetsViewBuilder viewBuilder;
  private final ManagePresetsInteractor interactor;

  protected ManagePresetsController() {
    super(new ManagePresetsModel());
    interactor = new ManagePresetsInteractor(this, model);
    viewBuilder = new ManagePresetsViewBuilder(model);
  }

  /**
   * Select group by name
   */
  public <T extends Preset> void selectGroup(@Nullable PresetStore<T> store) {
    model.setSelectedGroupStore(store);
  }

  @Override
  protected @NotNull FxViewBuilder<ManagePresetsModel> getViewBuilder() {
    return viewBuilder;
  }

  public void loadAllPresets() {
    onTaskThread(new LoadAllPresetsUpdateTask(model));
  }

  public <T extends Preset> void loadWithExisting(@Nullable PresetStore<T> presetStore) {
    if (presetStore != null) {
      model.getAllStores().add(presetStore);
    }

    // load all other presets
    loadAllPresets();

    if (presetStore != null) {
      selectGroup(presetStore);
    }
  }
}
