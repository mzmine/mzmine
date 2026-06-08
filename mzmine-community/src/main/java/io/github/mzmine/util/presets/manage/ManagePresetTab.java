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

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetStore;
import org.jetbrains.annotations.Nullable;

public class ManagePresetTab extends SimpleTab {

  private final ManagePresetsController controller;

  private ManagePresetTab(ManagePresetsController controller) {
    super("Manage presets");
    this.controller = controller;
    setContent(this.controller.buildView());
  }

  public static <T extends Preset> void show(@Nullable PresetStore<T> presetStore) {
    ManagePresetsController controller = new ManagePresetsController();

    // reuse existing preset store
    controller.loadWithExisting(presetStore);

    MZmineCore.getDesktop().addTab(new ManagePresetTab(controller));
  }

  public ManagePresetsController getController() {
    return controller;
  }
}
