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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.presets.AbstractJsonPresetStore;
import io.github.mzmine.util.presets.FxPresetEditor;
import io.github.mzmine.util.presets.PresetCategory;
import io.github.mzmine.util.presets.PresetGroup;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ModulePresetStore extends AbstractJsonPresetStore<ModulePreset> {

  private final ParameterSet serializationHelper;
  private final ObjectMapper mapper;
  private final ModulePresetGroup group;

  public ModulePresetStore(MZmineModule moduleClass, ParameterSet serializationHelper) {
    group = new ModulePresetGroup(moduleClass);
    // safe copy
    this.serializationHelper = serializationHelper.cloneParameterSet();

    mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    // define serialization and deserialization as plain xml string
    module.addSerializer(ParameterSet.class, new ParameterSetJsonSerializer());
    module.addDeserializer(ParameterSet.class,
        new ParameterSetJsonDeserializer(this.serializationHelper));
    mapper.registerModule(module);
  }

  @Override
  public @NotNull ObjectMapper getMapper() {
    return mapper;
  }

  @Override
  public @NotNull List<ModulePreset> createDefaults() {
    return List.of();
  }

  @Override
  public FxPresetEditor createPresetEditor() {
    // currently no editor. user should just save same name in the batch dialog
    return null;
  }

  @Override
  public @NotNull PresetCategory getPresetCategory() {
    return PresetCategory.MODULES;
  }

  @Override
  public @NotNull PresetGroup getPresetGroup() {
    return group;
  }

  public ModulePreset createPreset(String name, ParameterSet parameterSet) {
    return new ModulePreset(name, getPresetGroup().getUniqueID(), parameterSet);
  }
}
