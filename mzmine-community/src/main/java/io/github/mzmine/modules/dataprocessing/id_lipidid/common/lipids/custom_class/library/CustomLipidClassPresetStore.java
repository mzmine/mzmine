package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library;

import io.github.mzmine.util.presets.AbstractJsonPresetStore;
import io.github.mzmine.util.presets.FxPresetEditor;
import io.github.mzmine.util.presets.KnownPresetGroup;
import io.github.mzmine.util.presets.PresetCategory;
import io.github.mzmine.util.presets.PresetGroup;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The local library of custom lipid classes in the mzmine presets directory.
 */
public class CustomLipidClassPresetStore extends AbstractJsonPresetStore<CustomLipidClassPreset> {

  @Override
  public @NotNull PresetCategory getPresetCategory() {
    return PresetCategory.LIBRARIES;
  }

  @Override
  public @NotNull PresetGroup getPresetGroup() {
    return KnownPresetGroup.CUSTOM_LIPID_CLASS_PRESET;
  }

  @Override
  public @NotNull List<CustomLipidClassPreset> createDefaults() {
    return List.of();
  }

  @Override
  public @Nullable FxPresetEditor createPresetEditor() {
    return null;
  }
}
