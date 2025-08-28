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

package io.github.mzmine.modules.presets;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.presets.PresetCategory;
import io.github.mzmine.util.presets.PresetStore;
import io.github.mzmine.util.presets.PresetStoreFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Searches for initialized modules to create {@link ModulePresetStore}. Modules should be
 * initialized already from loading the config and working on modules.
 * <p>
 * TODO maybe optimize by having a list of modules like the list of datatypes
 */
public class ModulePresetStoreFactory implements PresetStoreFactory<ModulePreset> {

  @Override
  public @Nullable PresetStore<ModulePreset> createStore(@NotNull PresetCategory category,
      @NotNull String group) {
    for (MZmineModule module : MZmineCore.getAllModules()) {
      if (module.getUniqueID().equals(group)) {
        final ParameterSet params = ConfigService.getConfiguration()
            .getModuleParameters(module.getClass());
        if (params == null) {
          return null;
        }

        return new ModulePresetStore(module, params);
      }
    }

    return null;
  }
}
