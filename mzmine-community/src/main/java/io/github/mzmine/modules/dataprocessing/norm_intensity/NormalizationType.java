/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import org.jetbrains.annotations.NotNull;

public enum NormalizationType implements ModuleOptionsEnum<NormalizationTypeModule> {
  NoNormalization("No normalization", "no_normalization", NoNormalizationTypeModule.class), //
  ByFeatureIntensity("By feature abundances", "feature_intensity_normalization",
      FeatureIntensityNormalizationModule.class), //
  TotalRawSignal("Spectral TIC", "spectral_tic", TotalRawSignalNormalizationTypeModule.class), //
  MetadataColumn("Metadata column (weight, dilution, ...)", "metadata_column",
      MetadataColumnNormalizationTypeModule.class),  //
  StandardCompounds("Internal standard compounds", "internal_standard_compounds",
      StandardCompoundNormalizationTypeModule.class);

  private final String name;
  private final String stableId;
  private final Class<? extends NormalizationTypeModule> moduleClass;

  NormalizationType(@NotNull final String name, @NotNull final String stableId,
      @NotNull final Class<? extends NormalizationTypeModule> moduleClass) {
    this.name = name;
    this.stableId = stableId;
    this.moduleClass = moduleClass;
  }

  /**
   * @return All internal normalizers. Currently only standard compound normalzier but could be
   * extended for lipid normalizer or similar.
   */
  public static NormalizationType[] intraSampleNormalizers() {
    return new NormalizationType[]{NoNormalization, StandardCompounds};
  }

  /**
   * @return all normalizers that correct intra batch drift
   */
  public static NormalizationType[] intraBatchDriftNormalizers() {
    return new NormalizationType[]{NoNormalization, ByFeatureIntensity, TotalRawSignal};
  }

  @Override
  public @NotNull Class<? extends NormalizationTypeModule> getModuleClass() {
    return moduleClass;
  }

  @Override
  public @NotNull String getStableId() {
    return stableId;
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * @return Active if not NoNormalization
   */
  public boolean isActive() {
    return this != NoNormalization;
  }
}
