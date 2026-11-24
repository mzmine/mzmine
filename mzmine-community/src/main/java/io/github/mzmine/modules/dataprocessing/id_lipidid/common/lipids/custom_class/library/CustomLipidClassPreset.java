package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.util.presets.KnownPresetGroup;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetCategory;
import org.jetbrains.annotations.NotNull;

/**
 * A custom lipid class saved in the local lipid class library of the user.
 *
 * @param lipidClass the lipid class, stored in the same JSON format as the export
 */
public record CustomLipidClassPreset(
    @JsonSerialize(using = CustomLipidClassSerializer.class) //
    @JsonDeserialize(using = CustomLipidClassDeserializer.class) //
    @NotNull CustomLipidClass lipidClass) implements Preset {

  @Override
  public @NotNull String name() {
    return lipidClass.getAbbr() + " " + lipidClass.getName();
  }

  @Override
  public @NotNull PresetCategory presetCategory() {
    return PresetCategory.LIBRARIES;
  }

  @Override
  public @NotNull String presetGroup() {
    return KnownPresetGroup.CUSTOM_LIPID_CLASS_PRESET.getUniqueID();
  }

  /**
   * The name of the preset is derived from the class abbreviation and name, so renaming changes
   * the class name.
   */
  @Override
  public @NotNull CustomLipidClassPreset withName(String name) {
    return new CustomLipidClassPreset(
        new CustomLipidClass(name, lipidClass.getAbbr(), lipidClass.getCoreClass(),
            lipidClass.getMainClass(), lipidClass.getBackBoneFormula(), lipidClass.getChainTypes(),
            lipidClass.getFragmentationRules()));
  }
}
