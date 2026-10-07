/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking;

import io.github.mzmine.datamodel.features.types.numbers.scores.MLModelId;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.cosine_no_precursor.NoPrecursorCosineSpectralNetworkingModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.dreams.DreaMSNetworkingModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore.MS2DeepscoreNetworkingModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.structure_tanimoto.StructureTanimotoNetworkingModule;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

public enum SpectralNetworkingOptions implements ModuleOptionsEnum<MZmineModule>, UniqueIdSupplier {

  // spectral similarities for networking or analog
  MODIFIED_COSINE, MS2_DEEPSCORE, COSINE_NO_PRECURSOR, DREAMS,

  // structural similarity in here to use the same module for any networking
  STRUCTURE_TANIMOTO;

  public static SpectralNetworkingOptions[] getSpectralSimAlgorithms() {
    return Arrays.stream(values()).filter(SpectralNetworkingOptions::isSpectralSimilarity)
        .toArray(SpectralNetworkingOptions[]::new);
  }

  private boolean isSpectralSimilarity() {
    return switch (this) {
      case STRUCTURE_TANIMOTO -> false;
      case MODIFIED_COSINE, MS2_DEEPSCORE, COSINE_NO_PRECURSOR, DREAMS -> true;
    };
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case MS2_DEEPSCORE -> MS2DeepscoreNetworkingModule.NAME;
      case MODIFIED_COSINE -> ModifiedCosineSpectralNetworkingModule.NAME;
      case COSINE_NO_PRECURSOR -> NoPrecursorCosineSpectralNetworkingModule.NAME;
      case DREAMS -> DreaMSNetworkingModule.NAME;
      case STRUCTURE_TANIMOTO -> StructureTanimotoNetworkingModule.NAME;
    };
  }


  @Override
  public Class<? extends MZmineModule> getModuleClass() {
    return switch (this) {
      case MODIFIED_COSINE -> ModifiedCosineSpectralNetworkingModule.class;
      case MS2_DEEPSCORE -> MS2DeepscoreNetworkingModule.class;
      case COSINE_NO_PRECURSOR -> NoPrecursorCosineSpectralNetworkingModule.class;
      case DREAMS -> DreaMSNetworkingModule.class;
      case STRUCTURE_TANIMOTO -> StructureTanimotoNetworkingModule.class;
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case DREAMS -> MLModelId.DREAMS_1_0.getUniqueID();
      case MS2_DEEPSCORE -> MLModelId.MS2_DEEPSCORE_2_0.getUniqueID();
      case COSINE_NO_PRECURSOR -> "cosine_no_precursor";
      case MODIFIED_COSINE -> "modified_cosine";
      case STRUCTURE_TANIMOTO -> "structure_tanimoto";
    };
  }

  @Override
  public @NotNull String toString() {
    return switch (this) {
      case DREAMS -> "DreaMS";
      case MODIFIED_COSINE -> "Modified cosine";
      case COSINE_NO_PRECURSOR -> "Cosine (no precursor)";
      case STRUCTURE_TANIMOTO -> "Structure similarity (Tanimoto)";
      case MS2_DEEPSCORE -> "MS2 Deepscore";
    };
  }
}
